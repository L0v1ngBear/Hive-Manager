import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { once } from 'node:events'
import { existsSync } from 'node:fs'
import net from 'node:net'
import { resolve } from 'node:path'
import test from 'node:test'
import { setTimeout as delay } from 'node:timers/promises'
import { chromium } from 'playwright-core'

const projectRoot = resolve(import.meta.dirname, '..')
const viteEntry = resolve(projectRoot, 'node_modules/vite/bin/vite.js')
const browserTestRequested = process.env.LOGIN_LAYOUT_BROWSER === '1'

const chromeCandidates = {
  win32: [
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe'
  ],
  darwin: [
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    `${process.env.HOME || ''}/Applications/Google Chrome.app/Contents/MacOS/Google Chrome`
  ],
  linux: ['/usr/bin/google-chrome', '/usr/bin/google-chrome-stable', '/usr/bin/chromium', '/usr/bin/chromium-browser']
}

const getFreePort = () => new Promise((resolvePort, reject) => {
  const probe = net.createServer()
  probe.once('error', reject)
  probe.listen(0, '127.0.0.1', () => {
    const { port } = probe.address()
    probe.close((error) => error ? reject(error) : resolvePort(port))
  })
})

const launchChrome = () => {
  const override = process.env.LOGIN_LAYOUT_CHROME_PATH
  if (override && !existsSync(override)) {
    throw new Error(`LOGIN_LAYOUT_CHROME_PATH does not exist: ${override}`)
  }
  const executablePath = override || chromeCandidates[process.platform]?.find(existsSync)
  const options = executablePath ? { executablePath } : { channel: 'chrome' }
  return chromium.launch({ ...options, headless: true }).catch((error) => {
    throw new Error(`Login layout browser test requires Google Chrome. Set LOGIN_LAYOUT_CHROME_PATH or install Chrome discoverable by Playwright. ${error.message}`)
  })
}

const waitForVite = async (baseUrl) => {
  const deadline = Date.now() + 20_000
  while (Date.now() < deadline) {
    try {
      const response = await fetch(baseUrl)
      if (response.ok) return
    } catch {
      // Vite is still starting.
    }
    await delay(100)
  }
  throw new Error('Vite test server did not become ready')
}

const startVite = async (port) => {
  const baseUrl = `http://127.0.0.1:${port}`
  const server = spawn(process.execPath, [viteEntry, '--host', '127.0.0.1', '--port', String(port), '--strictPort'], {
    cwd: projectRoot,
    stdio: ['ignore', 'pipe', 'pipe'],
    windowsHide: true
  })
  let output = ''
  server.stdout.on('data', (chunk) => { output += chunk })
  server.stderr.on('data', (chunk) => { output += chunk })

  try {
    await waitForVite(baseUrl)
  } catch (error) {
    server.kill()
    throw new Error(`${error.message}\n${output}`)
  }
  return server
}

const stop = async (process) => {
  if (!process || process.exitCode !== null) return
  process.kill()
  await once(process, 'exit')
}

const respondToApi = async (route) => {
  const path = new URL(route.request().url()).pathname
  const data = path.endsWith('/scan-login/session')
    ? {
        sceneKey: 'browser-layout-scene',
        qrCodeDataUrl: 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="100" height="100"/%3E',
        expireAt: Math.floor(Date.now() / 1000) + 120
      }
    : { status: 'PENDING', message: '请用已登录的小程序扫码确认' }
  await route.fulfill({
    contentType: 'application/json',
    body: JSON.stringify({ code: 200, data })
  })
}

const openLoginPage = async (browser, baseUrl, width) => {
  const context = await browser.newContext({ viewport: { width, height: 900 } })
  const page = await context.newPage()
  await page.route((url) => new URL(url).pathname.startsWith('/api/'), respondToApi)
  await page.goto(`${baseUrl}/login`, { waitUntil: 'domcontentloaded' })
  await page.locator('.login-shell').waitFor({ state: 'visible', timeout: 5_000 })
  return { context, page }
}

const assertLoginMode = async (page, expected) => page.evaluate((mode) => {
  const accountTab = document.getElementById('login-account-tab')
  const scanTab = document.getElementById('login-scan-tab')
  const accountPanel = document.getElementById('login-account-panel')
  const scanPanel = document.getElementById('login-scan-panel')
  return {
    accountSelected: accountTab?.getAttribute('aria-selected'),
    scanSelected: scanTab?.getAttribute('aria-selected'),
    activeElement: document.activeElement?.id,
    accountPanelPresent: Boolean(accountPanel),
    scanPanelPresent: Boolean(scanPanel),
    accountPanelHidden: accountPanel?.hidden,
    scanPanelHidden: scanPanel?.hidden,
    accountControls: accountTab?.getAttribute('aria-controls'),
    scanControls: scanTab?.getAttribute('aria-controls'),
    commonPromptVisible: document.querySelector('.login-account-heading')?.textContent.includes('登录您的 Hive 账户以继续'),
    scanLiveRegion: scanPanel?.querySelector('[role="status"][aria-live="polite"][aria-atomic="true"]')?.id || ''
  }
}, expected)

const assertModeState = (state, mode) => {
  assert.equal(state.accountPanelPresent, true, 'account panel should remain in the DOM')
  assert.equal(state.scanPanelPresent, true, 'scan panel should remain in the DOM')
  assert.equal(state.accountControls, 'login-account-panel')
  assert.equal(state.scanControls, 'login-scan-panel')
  assert.equal(state.accountSelected, mode === 'account' ? 'true' : 'false')
  assert.equal(state.scanSelected, mode === 'scan' ? 'true' : 'false')
  assert.equal(state.accountPanelHidden, mode !== 'account')
  assert.equal(state.scanPanelHidden, mode !== 'scan')
  assert.equal(state.commonPromptVisible, true)
  assert.equal(state.scanLiveRegion, 'login-scan-status')
}

const browserTest = browserTestRequested ? test : test.skip

browserTest('public authentication layouts work in Chrome across viewports and modes', { timeout: 60_000 }, async (t) => {
  let server
  let browser
  const measurements = {}

  try {
    const port = await getFreePort()
    const baseUrl = `http://127.0.0.1:${port}`
    server = await startVite(port)
    browser = await launchChrome()

    for (const width of [1440, 1024, 390]) {
      const { context, page } = await openLoginPage(browser, baseUrl, width)
      const geometry = await page.evaluate(() => ({
        viewport: window.innerWidth,
        documentScrollWidth: document.documentElement.scrollWidth,
        bodyScrollWidth: document.body.scrollWidth
      }))
      assert.ok(geometry.documentScrollWidth <= width, `${width}px document overflowed horizontally`)
      assert.ok(geometry.bodyScrollWidth <= width, `${width}px body overflowed horizontally`)

      await page.locator('#login-scan-tab').click()
      assertModeState(await assertLoginMode(page, 'scan'), 'scan')
      await page.locator('#login-account-tab').click()
      assertModeState(await assertLoginMode(page, 'account'), 'account')
      await page.locator('#login-account-tab').focus()
      assertModeState(await assertLoginMode(page, 'account'), 'account')
      assert.equal(await page.locator('#login-account-tab').getAttribute('tabindex'), '0')
      assert.equal(await page.locator('#login-scan-tab').getAttribute('tabindex'), '-1')

      await page.keyboard.press('ArrowRight')
      assertModeState(await assertLoginMode(page, 'scan'), 'scan')
      assert.equal(await page.evaluate(() => document.activeElement?.id), 'login-scan-tab')

      await page.keyboard.press('ArrowLeft')
      assertModeState(await assertLoginMode(page, 'account'), 'account')
      assert.equal(await page.evaluate(() => document.activeElement?.id), 'login-account-tab')

      await page.keyboard.press('End')
      assertModeState(await assertLoginMode(page, 'scan'), 'scan')
      await page.keyboard.press('Home')
      assertModeState(await assertLoginMode(page, 'account'), 'account')

      await page.getByRole('button', { name: '使用组织码加入', exact: true }).click()
      await page.waitForURL('**/join-organization')
      const joinGeometry = await page.evaluate(() => ({
        viewport: window.innerWidth,
        documentScrollWidth: document.documentElement.scrollWidth,
        bodyScrollWidth: document.body.scrollWidth,
        brandDisplay: getComputedStyle(document.querySelector('.join-brand-panel')).display,
        formColumns: getComputedStyle(document.querySelector('.join-form-grid')).gridTemplateColumns
      }))
      assert.ok(joinGeometry.documentScrollWidth <= width, `${width}px join page document overflowed horizontally`)
      assert.ok(joinGeometry.bodyScrollWidth <= width, `${width}px join page body overflowed horizontally`)
      assert.equal(joinGeometry.brandDisplay, width <= 900 ? 'none' : 'flex')
      await page.getByRole('button', { name: '返回登录', exact: true }).click()
      await page.waitForURL('**/login')
      measurements[width] = { login: geometry, join: joinGeometry }
      await context.close()
    }
    t.diagnostic(JSON.stringify(measurements))
  } finally {
    await browser?.close()
    await stop(server)
  }
})
