import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { once } from 'node:events'
import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs'
import net from 'node:net'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import test from 'node:test'
import { setTimeout as delay } from 'node:timers/promises'
import { chromium } from 'playwright-core'

const browserTest = process.env.COMPONENT_DETAIL_BROWSER === '1' ? test : test.skip
const root = resolve(import.meta.dirname, '..')
const artifacts = process.env.COMPONENT_DETAIL_ARTIFACTS || join(tmpdir(), 'hive-component-details')
const routerSource = readFileSync(join(root, 'src/router/index.js'), 'utf8')
const sourceText = readdirSync(join(root, 'src'), { recursive: true })
  .filter(file => /\.(vue|js)$/.test(file)).map(file => readFileSync(join(root, 'src', file), 'utf8')).join('\n')
const permissions = [...new Set([...sourceText.matchAll(/['"]([a-z_-]+(?::[a-z_-]+)+)['"]/g)].map(match => match[1]))]
const features = [...new Set([...routerSource.matchAll(/'module\.([^']+)'/g)].map(match => `module.${match[1]}`))]
const session = { userId: 1, userName: '布局测试员', positionName: '业务专员', tenantCode: 'layout-test', tenantName: '布局验证企业', developer: true, permissions, features }

async function startVite() {
  const probe = net.createServer()
  probe.listen(0, '127.0.0.1')
  await once(probe, 'listening')
  const { port } = probe.address()
  await new Promise(resolveClose => probe.close(resolveClose))
  const server = spawn(process.execPath, [join(root, 'node_modules/vite/bin/vite.js'), '--host', '127.0.0.1', '--port', String(port), '--strictPort'], { cwd: root, stdio: 'ignore', windowsHide: true })
  const url = `http://127.0.0.1:${port}`
  for (let attempt = 0; attempt < 200; attempt++) {
    try { if ((await fetch(url)).ok) return { server, url } } catch { /* starting */ }
    await delay(100)
  }
  server.kill()
  throw new Error('Vite did not become ready')
}


const ticket = { id: 17, ticketNo: 'AS-17', customerName: '组件验收客户', projectName: '组件验收项目', ticketType: 'repair', status: 'pending_assignment', logisticsCompany: '顺丰快递', waybillNo: 'SF0218528272181' }
const order = { orderId: 'SO-17', orderNo: 'SO-17', customerName: ticket.customerName, projectName: ticket.projectName, status: 'pending_ship', orderCategory: 'bulk', items: [], shipments: [{ id: 17, deliveryMode: 'tracked', logisticsCompany: '顺丰快递', trackingNo: ticket.waybillNo }] }

browserTest('component details keep logistics inputs usable and editor actions reachable', { timeout: 300_000 }, async t => {
  mkdirSync(artifacts, { recursive: true })
  const vite = await startVite()
  const results = []
  const failures = []
  let browser
  try {
    const executablePath = process.env.COMPONENT_DETAIL_CHROME_PATH || ['C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe', 'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe'].find(existsSync)
    browser = await chromium.launch({ executablePath, headless: true })
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
    await context.addInitScript(value => {
      sessionStorage.setItem('token', 'isolated-component-test-token')
      sessionStorage.setItem('userInfo', JSON.stringify(value))
      sessionStorage.setItem('permissions', JSON.stringify(value.permissions))
      sessionStorage.setItem('features', JSON.stringify(value.features))
    }, session)
    const trackingCalls = []
    let trackingMode = 'empty'
    let releaseTracking
    await context.route('**/*', async route => {
      const url = new URL(route.request().url())
      if (url.origin !== vite.url) return route.abort()
      if (!url.pathname.startsWith('/api/')) return route.continue()
      let data = []
      if (url.pathname.endsWith('/auth/me')) data = session
      else if (url.pathname.endsWith('/logistics-tracking')) {
        trackingCalls.push(url)
        await new Promise(resolveResponse => { releaseTracking = resolveResponse })
        if (trackingMode === 'error') return route.fulfill({ contentType: 'application/json', body: JSON.stringify({ code: 500, msg: '暂时无法获取物流轨迹，请稍后重试' }) })
        data = trackingMode === 'long'
          ? { stateLabel: '运输中', traces: Array.from({ length: 30 }, (_, i) => ({ time: `2026-09-22 10:${String(i).padStart(2, '0')}`, context: '包裹已到达转运中心，正在安排下一站运输，请耐心等待物流更新。' })) }
          : { traces: [], latestContext: '暂未返回物流路径' }
      }
      else if (url.pathname.endsWith('/after-sales/tickets')) data = { data: [ticket], total: 1 }
      else if (url.pathname.endsWith('/orders')) data = { data: [order], total: 1, pages: 1 }
      else if (url.pathname.endsWith('/quality/list')) data = { data: [], total: 0 }
      else if (url.pathname.endsWith('/attendance/rule')) data = { id: 1, workStartTime: '08:00', workEndTime: '18:00', workDays: [1, 2, 3, 4, 5] }
      else if (/\/(page|tickets|tasks|parts)$/.test(url.pathname)) data = { data: [], records: [], total: 0, pages: 0 }
      else if (/\/(summary|stats|overview|status-summary|setting|rule|custom|field-config)$/.test(url.pathname)) data = {}
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify({ code: 200, data }) })
    })
    const page = await context.newPage()
    const pageErrors = []
    page.on('pageerror', error => pageErrors.push(error.message))
    page.setDefaultTimeout(8000)
    page.setDefaultNavigationTimeout(60_000)
    for (const [route, prefix, trigger] of [['after-sales', 'after-sales', '.after-sales-waybill-trigger']]) {
      for (const width of [1440, 390, 320]) {
        await page.setViewportSize({ width, height: 900 })
        await page.goto(`${vite.url}/function/${route}`, { waitUntil: 'networkidle' })
        const target = page.locator(trigger).first()
        await target.scrollIntoViewIfNeeded()
        await target.evaluate(el => {
          const wrap = el.closest('.el-table')?.querySelector('.el-scrollbar__wrap')
          if (wrap) wrap.scrollLeft += el.getBoundingClientRect().left - wrap.getBoundingClientRect().left - 8
        })
        const triggerBox = await target.boundingBox()
        await page.screenshot({ path: join(artifacts, `${route}-trigger-${width}.png`) })
        await page.mouse.move(triggerBox.x + 8, triggerBox.y + 12)
        const card = page.locator(`.${prefix}-logistics-card`)
        await card.waitFor({ state: 'visible' })
        await page.waitForTimeout(250)
        const input = card.getByRole('textbox', { name: '手机号后四位' })
        const callsBefore = trackingCalls.length
        await page.screenshot({ path: join(artifacts, `${route}-opened-${width}.png`) })
        await input.fill('1234')
        assert.equal(trackingCalls.length, callsBefore, 'hover and typing must not send phone suffix')
        const geometry = await card.evaluate(el => {
          const rect = node => { const r = node.getBoundingClientRect(); return { left: r.left, right: r.right, top: r.top, bottom: r.bottom } }
          const prompt = el.querySelector('[class$="logistics-phone-prompt"]')
          return { card: rect(el), prompt: rect(prompt), label: rect(prompt.querySelector('p')), input: rect(prompt.querySelector('.el-input')), button: rect(prompt.querySelector('button')), viewport: innerWidth }
        })
        results.push({ route, width, ...geometry })
        if (geometry.label.left - geometry.card.left < 12) failures.push(`${route}/${width}: phone prompt has less than 12px left inset`)
        if (Math.abs((geometry.input.top + geometry.input.bottom) / 2 - (geometry.button.top + geometry.button.bottom) / 2) > 3) failures.push(`${route}/${width}: input and query must share a row`)
        if (geometry.button.right > geometry.card.right - 11) failures.push(`${route}/${width}: query needs right inset`)
        if (geometry.card.left < 0 || geometry.card.right > width + 1) failures.push(`${route}/${width}: logistics card exceeds viewport`)
        await page.screenshot({ path: join(artifacts, `${route}-logistics-${width}.png`) })
        await card.screenshot({ path: join(artifacts, `${route}-logistics-window-${width}.png`) })
        await page.mouse.move(0, 0)
        await card.waitFor({ state: 'hidden' })
        await page.setViewportSize({ width, height: 900 })
        await page.mouse.move(triggerBox.x + 8, triggerBox.y + 12)
        await card.waitFor({ state: 'visible' })
        assert.equal(await input.inputValue(), '', 'hiding logistics must clear phone suffix')
        for (const mode of ['long', 'empty', 'error']) {
          trackingMode = mode
          await card.hover({ position: { x: 24, y: 24 } })
          await input.fill('1234')
          await card.getByRole('button', { name: '查询', exact: true }).focus()
          await page.keyboard.press('Enter')
          await card.locator('.after-sales-logistics-spinner').waitFor({ state: 'visible' })
          while (!releaseTracking) await delay(10)
          releaseTracking()
          releaseTracking = null
          if (mode === 'long') {
            await card.locator('.after-sales-logistics-trace').last().waitFor()
            const history = await card.locator('.after-sales-logistics-traces').evaluate(el => ({ scrollHeight: el.scrollHeight, clientHeight: el.clientHeight, count: el.children.length, overflow: getComputedStyle(el).overflowY, maxHeight: getComputedStyle(el).maxHeight }))
            await page.screenshot({ path: join(artifacts, `${route}-history-check-${width}.png`) })
            assert.ok(history.scrollHeight > history.clientHeight, `long logistics history scrolls inside its panel: ${JSON.stringify(history)}`)
          } else {
            await card.getByText(mode === 'empty' ? '暂未返回物流路径' : '暂时无法获取物流轨迹，请稍后重试', { exact: true }).waitFor()
          }
          assert.equal(trackingCalls.at(-1).searchParams.get('phoneSuffix'), '1234')
          await page.screenshot({ path: join(artifacts, `${route}-logistics-${mode}-${width}.png`) })
          await page.mouse.move(0, 0)
          await card.waitFor({ state: 'hidden' })
          await page.mouse.move(triggerBox.x + 8, triggerBox.y + 12)
          await card.waitFor({ state: 'visible' })
        }
        await page.mouse.move(0, 0)
        await card.waitFor({ state: 'hidden' })
      }
    }
    for (const [route, buttonName, tab] of [
      ['employee', '添加员工'], ['equipment', '新增设备'], ['customer', '新建客户'],
      ['price', '新增价格'], ['inventory', '新增入库'], ['document', '新建文件夹'],
      ['after-sales', '新建售后工单'], ['order', '新建订单'], ['order', '预警设置'],
      ['organization', '新增部门'], ['role', '新建角色'], ['bad-product', '新增质量记录'],
      ['attendance', '规则配置'], ['approval', '审批负责人'],
      ['approval', '新建财务审批', '财务'], ['approval', '新建离职申请', '离职'],
    ]) {
      for (const width of [1440, 390, 320]) {
        await page.setViewportSize({ width, height: 740 })
        await page.goto(`${vite.url}/function/${route}`, { waitUntil: 'networkidle' })
        if (tab) await page.getByRole('tab', { name: new RegExp(tab) }).click()
        await page.getByRole('button', { name: new RegExp(buttonName) }).first().click()
        const dialog = page.getByRole('dialog').last()
        await dialog.waitFor({ state: 'visible' })
        await page.waitForTimeout(350)
        await page.screenshot({ path: join(artifacts, `${route}-${buttonName}-editor-top-${width}.png`) })
        const geometry = await dialog.evaluate(el => {
          const r = el.getBoundingClientRect()
          const header = el.querySelector('.el-drawer__header, .el-dialog__header')
          const body = el.querySelector('.el-drawer__body, .el-dialog__body')
          const footer = el.querySelector('.el-drawer__footer, .el-dialog__footer, .el-drawer__body > .flex.shrink-0, .el-drawer__body > .h-full > :last-child, .el-drawer__body > .el-form > :last-child')
          if (body) body.scrollTop = body.scrollHeight
          return { left: r.left, right: r.right, top: r.top, bottom: r.bottom, headerBorder: header && getComputedStyle(header).borderBottomWidth, footerBorder: footer && getComputedStyle(footer).borderTopWidth, bodyOverflow: body && body.scrollWidth - body.clientWidth, footerBottom: footer?.getBoundingClientRect().bottom }
        })
        results.push({ route, buttonName, width, editor: geometry })
        if (geometry.left < -1 || geometry.right > width + 1) failures.push(`${route}/${width}: editor exceeds viewport`)
        if (geometry.footerBottom > 741) failures.push(`${route}/${width}: editor save actions outside viewport`)
        if (geometry.bodyOverflow > 1) failures.push(`${route}/${width}: editor body overflows horizontally`)
        await page.screenshot({ path: join(artifacts, `${route}-${buttonName}-editor-${width}.png`) })
        await page.keyboard.press('Escape')
        await dialog.waitFor({ state: 'hidden' })
      }
    }
    assert.deepEqual(pageErrors, [], 'no page runtime errors')
    assert.deepEqual(failures, [], 'component geometry regressions')
  } finally {
    writeFileSync(join(artifacts, 'results.json'), JSON.stringify({ results, failures }, null, 2))
    await browser?.close()
    if (vite.server.exitCode === null) { vite.server.kill(); await once(vite.server, 'exit') }
    t.diagnostic(`screenshots and geometry: ${artifacts}`)
  }
})

browserTest('public password-reset dialog fits narrow screens without sending account requests', { timeout: 60_000 }, async () => {
  mkdirSync(artifacts, { recursive: true })
  const vite = await startVite()
  let browser
  const results = []
  try {
    const executablePath = ['C:/Program Files/Google/Chrome/Application/chrome.exe', 'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe'].find(existsSync)
    browser = await chromium.launch({ executablePath, headless: true })
    const page = await browser.newPage()
    const errors = [], writes = []
    page.on('pageerror', error => errors.push(error.message))
    await page.route(url => new URL(url).pathname.startsWith('/api/'), request => {
      if (request.request().method() !== 'GET') writes.push(request.request().url())
      return request.fulfill({ contentType: 'application/json', body: JSON.stringify({ code: 200, data: { sceneKey: 'component-test', expireAt: Math.floor(Date.now() / 1000) + 120 } }) })
    })
    for (const width of [1440, 390, 320]) {
      await page.setViewportSize({ width, height: 740 })
      await page.goto(`${vite.url}/login`, { waitUntil: 'networkidle' })
      await page.getByRole('link', { name: /忘记密码/ }).click()
      const dialog = page.locator('.el-dialog')
      await dialog.waitFor({ state: 'visible' })
      await page.waitForTimeout(350)
      const box = await dialog.boundingBox()
      assert.ok(box.x >= 0 && box.x + box.width <= width + 1 && box.y + box.height <= 741)
      await page.screenshot({ path: join(artifacts, `login-reset-${width}.png`) })
      await page.keyboard.press('Escape')
      await dialog.waitFor({ state: 'hidden' })
      results.push({ viewportWidth: width, ...box })
    }
    assert.deepEqual(errors, [])
    assert.deepEqual(writes.filter(url => new URL(url).pathname !== '/api/auth/admin/scan-login/session'), [], 'only the existing scan-session initialization is allowed; no SMS or password writes')
  } finally {
    writeFileSync(join(artifacts, 'login-results.json'), JSON.stringify(results, null, 2))
    await browser?.close()
    vite.server.kill()
  }
})
