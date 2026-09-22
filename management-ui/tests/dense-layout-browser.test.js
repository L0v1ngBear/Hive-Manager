import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { once } from 'node:events'
import { existsSync, mkdirSync, readFileSync, readdirSync } from 'node:fs'
import net from 'node:net'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import test from 'node:test'
import { setTimeout as delay } from 'node:timers/promises'
import { chromium } from 'playwright-core'

const browserTest = process.env.DENSE_LAYOUT_BROWSER === '1' ? test : test.skip
const root = resolve(import.meta.dirname, '..')
const artifacts = process.env.OFFICE_LAYOUT_ARTIFACTS || join(tmpdir(), 'hive-dense-layout')
const routerSource = readFileSync(join(root, 'src/router/index.js'), 'utf8')
const sourceText = readdirSync(join(root, 'src'), { recursive: true })
  .filter(file => /\.(vue|js)$/.test(file)).map(file => readFileSync(join(root, 'src', file), 'utf8')).join('\n')
const permissions = [...new Set([...sourceText.matchAll(/['"]([a-z_]+(?::[a-z_]+)+)['"]/g)].map(match => match[1]))]
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

browserTest('populated organization, print queues and documents retain details and actions', { timeout: 120_000 }, async () => {
  mkdirSync(artifacts, { recursive: true })
  const vite = await startVite()
  const browser = await chromium.launch({ executablePath: ['C:/Program Files/Google/Chrome/Application/chrome.exe', 'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe'].find(existsSync), headless: true })
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
    await context.addInitScript(value => {
      sessionStorage.setItem('token', 'isolated-test')
      for (const [key, data] of Object.entries({ userInfo: value, permissions: value.permissions, features: value.features })) sessionStorage.setItem(key, JSON.stringify(data))
    }, session)
    await context.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
      const path = new URL(route.request().url()).pathname
      let data = []
      if (path.endsWith('/auth/me')) data = session
      if (path.endsWith('/document/folders') || /\/document\/list\/\d+$/.test(path)) data = Array.from({ length: 80 }, (_, i) => ({ id: i + 1, name: `业务文档${i + 1}`, parentId: 0, type: 0 }))
      if (path.endsWith('/receipt/print/pending')) data = Array.from({ length: 60 }, (_, i) => ({ orderNo: `ORDER-${i}`, customerName: '跨区域客户', itemCount: 20, totalMeters: 100 }))
      if (path.endsWith('/receipt/print/detail')) data = { orderNo: new URL(route.request().url()).searchParams.get('orderNo'), items: [] }
      if (path.endsWith('/print-task/pending')) data = Array.from({ length: 50 }, (_, i) => ({ taskNo: `TASK-${i}`, orderNo: `ORDER-${i}`, customerName: '跨区域客户', payload: {} }))
      if (path.endsWith('/organization/overview')) data = { departments: Array.from({ length: 60 }, (_, i) => ({ id: i + 1, deptName: `部门${i + 1} 跨区域业务运营管理中心`, employeeCount: 80, positionCount: 60, children: [] })), stats: { departmentCount: 60, employeeCount: 4800, emptyDepartmentCount: 0 } }
      if (/department\/\d+\/employees$/.test(path)) data = Array.from({ length: 80 }, (_, i) => ({ id: i + 1, name: `员工${i + 1}`, empNo: `E${i + 1}`, positionName: '业务专员', status: 1 }))
      if (/department\/\d+\/positions$/.test(path)) data = Array.from({ length: 60 }, (_, i) => ({ id: i + 1, positionName: `职位${i + 1}`, positionCode: `P${i + 1}`, employeeCount: 1 }))
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify({ code: 200, data }) })
    })
    const page = await context.newPage()
    page.on('pageerror', error => console.error(error.message))
    page.on('console', message => { if (message.type() === 'error') console.error(message.text()) })
    await page.goto(`${vite.url}/function/organization`, { waitUntil: 'networkidle' })
    await page.screenshot({ path: join(artifacts, 'initial.png') })
    assert.ok(await page.locator('.org-node').count(), await page.locator('body').innerText())
    const visibleInMain = locator => locator.evaluate(el => { const r = el.getBoundingClientRect(); const m = document.querySelector('.ys-app-main').getBoundingClientRect(); return r.top >= m.top && r.bottom <= m.bottom })
    for (const width of [1440, 1024, 390, 320]) {
      await page.setViewportSize({ width, height: 900 })
      await page.locator('.org-node').last().click()
      await page.waitForTimeout(250)
      assert.ok(await visibleInMain(page.locator('aside.panel-card .panel-header')), `selected detail stays visible after choosing last department at ${width}`)
      await page.getByRole('tab', { name: '成员', exact: true }).click()
      await page.locator('.member-card').last().scrollIntoViewIfNeeded()
      assert.ok(await visibleInMain(page.locator('aside.panel-card .panel-header')), 'department heading stays visible while scrolling employees')
      await page.getByRole('tab', { name: '职位', exact: true }).click()
      await page.locator('.position-table-wrap').evaluate(el => { el.scrollTop = el.scrollHeight })
      assert.ok(await visibleInMain(page.getByRole('button', { name: '新增职位', exact: true })), 'position action stays visible while scrolling positions')
      assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth && document.querySelector('.ys-app-main').scrollWidth <= document.querySelector('.ys-app-main').clientWidth + 1))
      await page.screenshot({ path: join(artifacts, `organization-${width}.png`) })
      if (width < 1200) {
        await page.getByRole('button', { name: '返回部门', exact: true }).click()
        assert.ok(await visibleInMain(page.locator('.department-panel .panel-header')), 'return button reveals department list')
      }
    }
    await page.setViewportSize({ width: 1440, height: 900 })
    for (const [route, list, card, header] of [
      ['receipt', '.queue-list', '.queue-card', '.preview-head'],
      ['label', '.task-list', '.task-card', '.preview-head'],
    ]) {
      await page.goto(`${vite.url}/function/${route}`, { waitUntil: 'networkidle' })
      assert.ok(await page.locator(card).count() >= 50, `${route}: populated fixture`)
      await page.locator(card).last().click()
      assert.ok(await page.locator(list).evaluate(el => el.scrollHeight > el.clientHeight && el.scrollTop > 0), `${route}: queue scrolls independently`)
      assert.ok(await visibleInMain(page.locator(header)), `${route}: preview stays visible after selecting last item`)
      await page.screenshot({ path: join(artifacts, `${route}-1440.png`) })
    }
    await page.goto(`${vite.url}/function/document`, { waitUntil: 'networkidle' })
    assert.equal(await page.locator('.document-folder-tree .el-tree-node').count(), 80)
    await page.locator('.document-folder-tree .el-tree-node__content').last().click()
    assert.ok(await page.locator('.document-tree-section').evaluate(el => el.scrollHeight > el.clientHeight && el.scrollTop > 0), 'document tree scrolls independently')
    assert.ok(await visibleInMain(page.locator('.document-command-bar')), 'document toolbar remains visible')
    await page.screenshot({ path: join(artifacts, 'document-1440.png') })
  } finally {
    await browser.close()
    if (vite.server.exitCode === null) { vite.server.kill(); await once(vite.server, 'exit') }
  }
})
