import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { once } from 'node:events'
import { resolve } from 'node:path'
import test from 'node:test'
import { setTimeout as delay } from 'node:timers/promises'
import { chromium } from 'playwright-core'

const projectRoot = resolve(import.meta.dirname, '..')
const viteEntry = resolve(projectRoot, 'node_modules/vite/bin/vite.js')
const chromeExecutable = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const port = 4179
const baseUrl = `http://127.0.0.1:${port}`
const legacyLayout = process.env.CUSTOMER_LAYOUT_LEGACY === '1'

const customer = {
  id: 1,
  customerName: '浏览器布局测试客户',
  customerType: '1',
  contacts: [{ contactName: '测试联系人', contactPhone: '13800000000' }],
  projects: [{ projectName: '测试项目', projectOwner: '测试负责人', constructionArea: '1000㎡' }]
}

const waitForVite = async () => {
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

const startVite = async () => {
  const server = spawn(process.execPath, [viteEntry, '--host', '127.0.0.1', '--port', String(port), '--strictPort'], {
    cwd: projectRoot,
    stdio: ['ignore', 'pipe', 'pipe'],
    windowsHide: true
  })
  let output = ''
  server.stdout.on('data', (chunk) => { output += chunk })
  server.stderr.on('data', (chunk) => { output += chunk })
  server.once('exit', (code) => {
    if (code !== 0) output += `\nVite exited with code ${code}`
  })

  try {
    await waitForVite()
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
  const data = path.endsWith('/customer/page')
    ? { data: [customer], total: 1, pages: 1 }
    : []
  await route.fulfill({
    contentType: 'application/json',
    body: JSON.stringify({ code: 200, data })
  })
}

const collectGeometry = async (page) => page.evaluate(() => {
  const visible = (element) => {
    if (!element || element.hidden) return false
    const style = window.getComputedStyle(element)
    const rect = element.getBoundingClientRect()
    return style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0
  }
  const rectangle = (element) => {
    const { left, right, top, bottom, width, height } = element.getBoundingClientRect()
    return { left, right, top, bottom, width, height }
  }
  const shell = document.querySelector('.function-page-shell')
  const sidebar = document.querySelector('aside.ys-sidebar')
  const filter = document.querySelector('.customer-filter-form')
  const actions = document.querySelector('.customer-filter-form .function-filter-actions')
  const tableWrap = document.querySelector('.responsive-table-wrap')
  const fixedRight = tableWrap?.querySelector('.el-table-fixed-column--right')
  const actionControls = [...actions.querySelectorAll('button')]
  const exportControl = actions.querySelector('.column-export-trigger')
  const filterRect = rectangle(filter)
  const tableRect = rectangle(tableWrap)
  const fixedRect = fixedRight ? rectangle(fixedRight) : null

  return {
    viewport: window.innerWidth,
    sidebarWidth: sidebar ? Math.round(sidebar.getBoundingClientRect().width) : 0,
    shellWidth: Math.round(shell.clientWidth),
    filter: {
      clientWidth: filter.clientWidth,
      scrollWidth: filter.scrollWidth,
      rect: filterRect
    },
    actions: {
      clientWidth: actions.clientWidth,
      scrollWidth: actions.scrollWidth,
      visible: visible(actions),
      controlsVisible: actionControls.every(visible),
      exportVisible: visible(exportControl),
      controlsInsideFilter: actionControls.every((control) => {
        const rect = control.getBoundingClientRect()
        return rect.left >= filterRect.left && rect.right <= filterRect.right && rect.top >= filterRect.top && rect.bottom <= filterRect.bottom
      })
    },
    table: {
      clientWidth: tableWrap.clientWidth,
      scrollWidth: tableWrap.scrollWidth,
      overflowX: window.getComputedStyle(tableWrap).overflowX,
      fixedRightPresent: Boolean(fixedRight),
      fixedRightInsideWrap: Boolean(fixedRect) && fixedRect.left >= tableRect.left && fixedRect.right <= tableRect.right + 1,
      fixedRightSticky: fixedRight ? window.getComputedStyle(fixedRight).position === 'sticky' : false
    }
  }
})

const assertLayout = (geometry, name) => {
  assert.ok(geometry.filter.scrollWidth <= geometry.filter.clientWidth, `${name}: filter overflowed (${geometry.filter.scrollWidth} > ${geometry.filter.clientWidth})`)
  assert.ok(geometry.actions.visible, `${name}: filter actions were hidden`)
  assert.ok(geometry.actions.controlsVisible, `${name}: a filter action was hidden`)
  assert.ok(geometry.actions.exportVisible, `${name}: export action was hidden`)
  assert.ok(geometry.actions.controlsInsideFilter, `${name}: a filter action was clipped outside the filter panel`)
  assert.equal(geometry.table.overflowX, 'auto', `${name}: table wrapper is not horizontally scrollable`)
  assert.ok(geometry.table.fixedRightPresent, `${name}: fixed right operation column was not rendered`)
  assert.ok(geometry.table.fixedRightInsideWrap, `${name}: fixed right operation column was outside the table wrapper`)
  assert.ok(geometry.table.fixedRightSticky, `${name}: fixed right operation column was not sticky`)
}

const openCustomerPage = async (browser, viewport) => {
  const context = await browser.newContext({ viewport: { width: viewport, height: 900 } })
  await context.addInitScript(() => {
    sessionStorage.setItem('token', 'browser-layout-token')
    sessionStorage.setItem('userInfo', JSON.stringify({ userId: 1, userName: 'layout-test', tenantCode: 'test', tenantName: '布局测试组织' }))
    sessionStorage.setItem('permissions', JSON.stringify(['customer:list', 'customer:create', 'customer:update', 'customer:detail', 'customer:export']))
    sessionStorage.setItem('features', JSON.stringify(['module.customer']))
    sessionStorage.setItem('mustChangePassword', '0')
  })
  const page = await context.newPage()
  const pageErrors = []
  page.on('console', (message) => {
    if (message.type() === 'error') pageErrors.push(message.text())
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))
  await page.route((url) => new URL(url).pathname.startsWith('/api/'), respondToApi)
  await page.goto(`${baseUrl}/function/customer`, { waitUntil: 'domcontentloaded' })
  try {
    await page.locator('.customer-filter-form').waitFor({ state: 'visible', timeout: 5_000 })
  } catch (error) {
    throw new Error(`Customer page did not render at ${page.url()}: ${await page.locator('body').innerText()}\n${pageErrors.join('\n')}\n${error.message}`)
  }

  if (legacyLayout) {
    await page.addStyleTag({ content: '.customer-filter-form { grid-template-columns: minmax(16rem, 1.5fr) repeat(3, minmax(10rem, 1fr)) minmax(15rem, auto) !important; }' })
  }
  return { context, page }
}

test('customer layout renders without clipped filters across shell widths', { timeout: 60_000 }, async (t) => {
  const server = await startVite()
  const browser = await chromium.launch({ executablePath: chromeExecutable, headless: true })
  const measurements = {}

  try {
    const wide = await openCustomerPage(browser, 1440)
    measurements.wide = await collectGeometry(wide.page)
    assertLayout(measurements.wide, '1440px')
    await wide.context.close()

    const desktop = await openCustomerPage(browser, 1024)
    measurements.expanded = await collectGeometry(desktop.page)
    assertLayout(measurements.expanded, '1024px expanded sidebar')
    await desktop.page.locator('aside.ys-sidebar > div:last-child button').click()
    await desktop.page.locator('aside.ys-sidebar.ys-sidebar--collapsed').waitFor()
    await desktop.page.waitForTimeout(350)
    measurements.collapsed = await collectGeometry(desktop.page)
    assertLayout(measurements.collapsed, '1024px collapsed sidebar')
    assert.ok(measurements.expanded.sidebarWidth > measurements.collapsed.sidebarWidth, '1024px sidebar did not collapse in the rendered layout')
    assert.ok(measurements.expanded.shellWidth < measurements.collapsed.shellWidth, '1024px shell width did not grow after sidebar collapse')
    await desktop.context.close()

    const compact = await openCustomerPage(browser, 390)
    measurements.compact = await collectGeometry(compact.page)
    assertLayout(measurements.compact, '390px')
    await compact.context.close()
    t.diagnostic(JSON.stringify(measurements))
  } finally {
    await browser.close()
    await stop(server)
  }
})
