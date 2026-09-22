import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { once } from 'node:events'
import { existsSync, mkdirSync, writeFileSync } from 'node:fs'
import net from 'node:net'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import test from 'node:test'
import { setTimeout as delay } from 'node:timers/promises'
import { chromium } from 'playwright-core'

const browserTest = process.env.COMPONENT_DETAILS_BROWSER === '1' ? test : test.skip
const root = resolve(import.meta.dirname, '..')
const artifacts = join(tmpdir(), 'hive-component-primitives')

async function startVite() {
  const probe = net.createServer().listen(0, '127.0.0.1')
  await once(probe, 'listening')
  const { port } = probe.address()
  await new Promise(done => probe.close(done))
  const server = spawn(process.execPath, [join(root, 'node_modules/vite/bin/vite.js'), '--host', '127.0.0.1', '--port', String(port), '--strictPort'], { cwd: root, windowsHide: true, stdio: 'ignore' })
  const url = `http://127.0.0.1:${port}`
  for (let i = 0; i < 200; i++) {
    try { if ((await fetch(url)).ok) return { server, url } } catch { /* starting */ }
    await delay(100)
  }
  server.kill()
  throw new Error('Vite did not become ready')
}

async function assertBounded(locator, width, height) {
  const box = await locator.boundingBox()
  assert.ok(box && box.x >= -1 && box.y >= -1 && box.x + box.width <= width + 1 && box.y + box.height <= height + 1, JSON.stringify(box))
}

browserTest('component surfaces keep long content scrollable and controls usable at desktop and narrow sizes', { timeout: 180_000 }, async () => {
  mkdirSync(artifacts, { recursive: true })
  const vite = await startVite()
  let browser
  const results = []
  try {
    const executablePath = ['C:/Program Files/Google/Chrome/Application/chrome.exe', 'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe'].find(existsSync)
    browser = await chromium.launch({ executablePath, headless: true })
    for (const width of [1440, 1024, 768, 390, 320]) {
      const height = width < 500 ? 640 : 800
      const page = await browser.newPage({ viewport: { width, height } })
      page.setDefaultTimeout(8000)
      const errors = []
      page.on('pageerror', error => errors.push(error.message))
      await page.goto(`${vite.url}/tests/fixtures/component-detail-gallery.html`, { waitUntil: 'networkidle' })
      for (const name of ['查看详情', '辅助操作']) {
        const style = await page.getByRole('button', { name, exact: true }).evaluate(el => ({ image: getComputedStyle(el).backgroundImage, color: getComputedStyle(el).color }))
        assert.equal(style.image, 'none', `${name}: secondary text actions must not inherit the solid primary-button gradient`)
        assert.notEqual(style.color, 'rgb(255, 255, 255)', `${name}: text remains readable on the page surface`)
      }
      await page.getByRole('button', { name: '打开长表单', exact: true }).click()
      const dialog = page.locator('.el-dialog')
      await dialog.waitFor()
      await page.waitForTimeout(350)
      await assertBounded(dialog, width, height)
      const geometry = await dialog.evaluate(el => {
        const body = el.querySelector('.el-dialog__body')
        const header = el.querySelector('.el-dialog__header')
        const footer = el.querySelector('.el-dialog__footer')
        const label = el.querySelector('.el-form-item__label')
        return { scrollable: body.scrollHeight > body.clientHeight, overflow: body.scrollWidth - body.clientWidth, padding: parseFloat(getComputedStyle(body).paddingLeft), headerBorder: parseFloat(getComputedStyle(header).borderBottomWidth), footerBorder: parseFloat(getComputedStyle(footer).borderTopWidth), labelHeight: label.getBoundingClientRect().height }
      })
      assert.ok(geometry.scrollable, 'long form owns its scroll area')
      assert.ok(geometry.overflow <= 1, `form has horizontal overflow: ${JSON.stringify(geometry)}`)
      assert.ok(geometry.padding >= 16 && geometry.headerBorder >= 1 && geometry.footerBorder >= 1)
      await page.screenshot({ path: join(artifacts, `dialog-top-${width}.png`) })
      await dialog.getByRole('combobox', { name: '业务类型', exact: true }).focus()
      await page.keyboard.press('Enter')
      await page.getByRole('option', { name: '增补订单', exact: true }).click()
      await dialog.getByPlaceholder('起始日期').click()
      const calendar = page.locator('.el-date-range-picker:visible')
      await calendar.waitFor()
      await page.waitForTimeout(200)
      await assertBounded(calendar, width, height)
      await page.screenshot({ path: join(artifacts, `calendar-${width}.png`) })
      await calendar.locator('.el-date-range-picker__content.is-left td.available:not(.prev-month):not(.next-month)').first().click()
      await calendar.locator('.el-date-range-picker__content.is-right td.available:not(.prev-month):not(.next-month)').first().click()
      await calendar.waitFor({ state: 'hidden' })
      assert.ok(await dialog.getByPlaceholder('起始日期').inputValue())
      assert.ok(await dialog.getByPlaceholder('结束日期').inputValue())
      const save = dialog.getByRole('button', { name: '保存资料', exact: true })
      await assertBounded(save, width, height)
      await save.click()
      await dialog.getByPlaceholder('填写第 12 项资料').fill('末尾内容仍可编辑')
      await assertBounded(save, width, height)
      await page.screenshot({ path: join(artifacts, `dialog-${width}.png`) })
      await dialog.getByRole('button', { name: '取消', exact: true }).click()
      await page.getByRole('button', { name: '打开标准抽屉', exact: true }).click()
      const drawer = page.locator('.el-drawer')
      await drawer.waitFor()
      await page.waitForTimeout(350)
      await assertBounded(drawer, width, height)
      await drawer.locator('input').last().fill('最后一项')
      await assertBounded(drawer.getByRole('button', { name: '确认资料', exact: true }), width, height)
      await page.screenshot({ path: join(artifacts, `drawer-${width}.png`) })
      await page.keyboard.press('Escape')
      await drawer.waitFor({ state: 'hidden' })
      await page.getByRole('button', { name: '打开确认框', exact: true }).click()
      const confirm = page.locator('.el-message-box')
      await confirm.waitFor()
      await page.waitForTimeout(250)
      await assertBounded(confirm, width, height)
      await page.screenshot({ path: join(artifacts, `confirm-${width}.png`) })
      await confirm.getByRole('button', { name: '取消', exact: true }).click()
      await page.locator('.column-settings-trigger').filter({ hasText: '列设置' }).click()
      const columns = page.locator('.column-settings-panel')
      await columns.waitFor({ state: 'visible' })
      await page.waitForTimeout(250)
      await assertBounded(columns, width, height)
      await columns.getByRole('button', { name: '上移', exact: true }).last().click()
      assert.match(await columns.locator('.column-settings-label').nth(22).innerText(), /24/)
      await page.screenshot({ path: join(artifacts, `columns-${width}.png`) })
      await page.keyboard.press('Escape')
      await columns.waitFor({ state: 'hidden' })
      assert.equal(await page.getByRole('button', { name: '列设置', exact: true }).evaluate(el => document.activeElement === el), true, 'closing column settings returns keyboard focus to its trigger')
      assert.equal(await page.getByRole('button', { name: '无权限操作', exact: true }).isDisabled(), true)
      assert.deepEqual(errors, [])
      results.push({ width, height, ...geometry, errors })
      await page.close()
    }
  } finally {
    writeFileSync(join(artifacts, 'results.json'), JSON.stringify(results, null, 2))
    await browser?.close()
    vite.server.kill()
  }
})
