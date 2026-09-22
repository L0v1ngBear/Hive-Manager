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

const browserTest = process.env.OFFICE_LAYOUT_BROWSER === '1' ? test : test.skip
const root = resolve(import.meta.dirname, '..')
const artifacts = process.env.OFFICE_LAYOUT_ARTIFACTS || join(tmpdir(), 'hive-office-layout')
const routerSource = readFileSync(join(root, 'src/router/index.js'), 'utf8')
const routes = [...routerSource.matchAll(/path: '([^']+)',\s*name: '([^']+)',\s*component: \(\) => import\('([^']+)'\)/g)]
  .map(([, path, name, source]) => ({ path: path.startsWith('/') ? path : `${source.includes('/function/') ? '/function' : ''}/${path}`, name }))
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

async function mockApi(route, currentSession) {
  const path = new URL(route.request().url()).pathname
  let data = []
  if (path.endsWith('/auth/me')) data = currentSession
  else if (/\/(page|orders|tickets|tasks)$/.test(path)) data = { data: [], records: [], total: 0, pages: 0 }
  else if (/\/(summary|stats|overview|status-summary|setting|rule|custom|field-config)$/.test(path)) data = {}
  else if (path.endsWith('/scan-login/session')) data = { sceneKey: 'layout-test', qrCodeDataUrl: '', expireAt: Math.floor(Date.now() / 1000) + 120 }
  await route.fulfill({ contentType: 'application/json', body: JSON.stringify({ code: 200, data }) })
}

browserTest('every page keeps readable navigation, usable actions and bounded screen geometry', { timeout: 240_000 }, async t => {
  mkdirSync(artifacts, { recursive: true })
  const vite = await startVite()
  let browser
  const results = []
  try {
    const executablePath = process.env.OFFICE_LAYOUT_CHROME_PATH || [
      'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
      'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
    ].find(existsSync)
    browser = await chromium.launch(executablePath ? { executablePath, headless: true } : { channel: 'chrome', headless: true })
    for (const route of routes) {
      const publicPage = ['Login', 'JoinOrganization', 'Privacy', 'Terms', 'OfficialAccountBindingCallback'].includes(route.name)
      const currentSession = { ...session, tenantCode: route.name === 'TenantManage' ? 'super' : session.tenantCode, mustChangePassword: route.name === 'ForcePasswordChange' }
      const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
      if (!publicPage) await context.addInitScript(value => {
        sessionStorage.setItem('token', 'isolated-layout-test-token')
        sessionStorage.setItem('userInfo', JSON.stringify(value))
        sessionStorage.setItem('permissions', JSON.stringify(value.permissions))
        sessionStorage.setItem('features', JSON.stringify(value.features))
        sessionStorage.setItem('mustChangePassword', value.mustChangePassword ? '1' : '0')
      }, currentSession)
      await context.route(url => new URL(url).pathname.startsWith('/api/'), request => mockApi(request, currentSession))
      const page = await context.newPage()
      const errors = []
      page.on('pageerror', error => errors.push(error.message))
      page.setDefaultTimeout(8000)
      await page.goto(`${vite.url}${route.path}`, { waitUntil: 'networkidle' })
      if (route.name !== 'ForcePasswordChange' && !publicPage) {
        await page.locator(`[data-route-name="${route.name}"]`).waitFor()
        const guide = page.locator('.page-operation-guide details')
        await guide.locator('summary').focus()
        await page.keyboard.press('Enter')
        if (route.name !== 'TenantManage') {
          assert.equal(await page.locator('.sidebar-tenant-card').count(), 0, 'duplicate enterprise card is removed')
          assert.equal(await page.locator('.ys-sidebar .sidebar-brand-logo img').count(), 1, 'top brand logo remains')
          assert.equal(await page.locator('.ys-sidebar--collapsed').count(), 0, 'navigation names are visible by default')
          assert.ok((await page.locator('.ys-sidebar nav').innerText()).includes('订单列表'))
          assert.ok((await page.locator('.ys-sidebar nav').innerText()).includes('文档管理'), 'secondary entries are expanded')
        }
        assert.equal(await guide.getAttribute('open'), '', `${route.name}: guide opens with keyboard`)
        assert.equal(await guide.locator('li').count(), 3)
        await page.keyboard.press('Enter')
      }
      for (const width of [1440, 1024, 768, 390, 320]) {
        await page.setViewportSize({ width, height: 1000 })
        await page.waitForTimeout(120)
        const geometry = await page.evaluate(() => {
          const visible = el => el.getBoundingClientRect().width > 0 && el.getBoundingClientRect().height > 0
          const main = document.querySelector('.ys-app-main')
          const header = document.querySelector('.function-page-header, .order-title-row, .label-print-header')
          const actionButtons = [...(header?.querySelectorAll('button') || []), ...document.querySelectorAll('.page-action-group button')]
          const badActions = actionButtons.filter(visible).filter(el => {
            const r = el.getBoundingClientRect()
            return r.left < -1 || r.right > innerWidth + 1
          }).map(el => el.textContent.trim())
          return {
            width: innerWidth,
            documentWidth: document.documentElement.scrollWidth,
            mainOverflow: main ? main.scrollWidth - main.clientWidth : 0,
            badActions,
            filterOverflow: [...document.querySelectorAll('.function-filter-form')].filter(visible).filter(el => el.scrollWidth > el.clientWidth + 1).map(el => el.className),
            sidebarNames: [...document.querySelectorAll('.ys-sidebar .sidebar-nav-button')].filter(visible).map(el => el.getAttribute('aria-label')),
            titleSize: header?.querySelector('.function-page-title') ? getComputedStyle(header.querySelector('.function-page-title')).fontSize : null,
          }
        })
        results.push({ route: route.name, ...geometry, errors: [...errors] })
        await page.screenshot({ path: join(artifacts, `${route.name}-${width}.png`), fullPage: true })
      }
      if (route.name === 'Equipment') {
        await page.getByRole('button', { name: '收起筛选', exact: true }).click()
        const create = page.locator('.function-page-header').getByRole('button', { name: '新增设备', exact: true })
        assert.ok(await create.isVisible(), 'create remains visible with collapsed filters on a narrow screen')
        assert.ok(await page.locator('.function-page-header').getByRole('button', { name: '导出 Excel', exact: true }).isVisible())
        await create.click()
        await page.getByRole('dialog', { name: '新增设备', exact: true }).waitFor()
        assert.ok(await page.getByPlaceholder('例如：定型机01').isVisible(), 'primary entry opens the existing editor')
        await page.getByRole('button', { name: '取消', exact: true }).click()
      }
      if (route.name === 'Dashboard') {
        await page.getByRole('button', { name: '打开功能导航' }).click()
        const mobileMenu = page.locator('.mobile-navigation-panel')
        await mobileMenu.waitFor()
        await mobileMenu.getByRole('button', { name: '订单列表', exact: true }).click()
        await page.waitForURL('**/function/order')
        assert.equal(await mobileMenu.count(), 0, 'navigation closes after choosing a destination')
      }
      await context.close()
      writeFileSync(join(artifacts, 'results.json'), JSON.stringify(results, null, 2))
      t.diagnostic(`checked ${route.name}`)
    }
    writeFileSync(join(artifacts, 'results.json'), JSON.stringify(results, null, 2))
    const failures = results.filter(row => row.errors.length || row.documentWidth > row.width + 1 || row.mainOverflow > 1 || row.badActions.length || row.filterOverflow.length)
    assert.deepEqual(failures, [], 'all pages must fit the viewport with visible header actions and no runtime errors')
  } finally {
    await browser?.close()
    if (vite.server.exitCode === null) { vite.server.kill(); await once(vite.server, 'exit') }
    t.diagnostic(`screenshots and measurements: ${artifacts}`)
  }
})
