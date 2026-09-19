import assert from 'node:assert/strict'
import { mkdtemp } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import { createServer } from 'vite'
import { chromium } from 'playwright-core'

const root = resolve(import.meta.dirname, '..')
const screenshots = await mkdtemp(join(tmpdir(), 'hive-aftersales-treatments-'))
const server = await createServer({ root, cacheDir: join(screenshots, 'vite-cache'), server: { host: '127.0.0.1', port: 0, strictPort: false, open: false } })
await server.listen()
const browser = await chromium.launch({ channel: 'chrome', headless: true })
const context = await browser.newContext({ viewport: { width: 1024, height: 900 } })
const page = await context.newPage()
page.setDefaultTimeout(10000)
const errors = [], requests = []
page.on('pageerror', error => errors.push(error.message))
page.on('console', message => { if (message.type() === 'error' || message.type() === 'warning') errors.push(message.text()) })
const records = [
  { id: 1, ticketId: 1, ticketNo: 'AS-TEST', sequenceNo: 1, requestKey: 'first', treatmentType: 'resend_parts', status: 'unresolved', version: 3, originalOpeningDate: '2020-02-29', createTime: '2026-09-17 10:00', creatorName: '测试处理人', details: { description: '第一次补发皮带', repairImages: [], logisticsCompany: '中通', waybillNo: 'ZT-OLD' }, parts: [{ partId: 10, partName: '皮带', quantity: 2, partLocation: '一车间', lineStatus: 'outbound' }], followUp: { resolved: false, satisfaction: '一般', content: '仍有异响', followUpImages: [] } },
  { id: 2, ticketId: 1, ticketNo: 'AS-TEST', sequenceNo: 2, requestKey: 'second', treatmentType: 'motor_replacement', status: 'processing', version: 0, originalOpeningDate: '2020-02-29', createTime: '2026-09-19 10:00', creatorName: '测试处理人', details: { requestKey: 'second', treatmentType: 'motor_replacement', description: '第二次更换电机', oldMotorInfo: 'M1', newMotorModel: 'M2', motorQuantity: 1, repairImages: [], parts: [] }, parts: [] }
]
const original = JSON.stringify(records[0])
await page.route('**/api/**', async route => {
  const request = route.request(), url = new URL(request.url()), path = url.pathname
  if (!path.startsWith('/api/')) return route.continue()
  let data
  if (request.method() === 'POST' && /\/treatments\/2\/(complete|follow-up)$/.test(path)) {
    const payload = request.postDataJSON(); requests.push({ path, payload })
    assert.equal(payload.version, records[1].version)
    records[1].version++
    if (path.endsWith('/complete')) { records[1].status = 'waiting_follow_up'; records[1].resolution = payload.resolution; records[1].completedTime = '2026-09-19 11:00' }
    else { records[1].status = payload.followUp.resolved ? 'resolved' : 'unresolved'; records[1].followUp = payload.followUp; records[1].followUpTime = '2026-09-19 12:00' }
    data = records[1]
  } else if (request.method() === 'POST' && path.endsWith('/treatments')) {
    const payload = request.postDataJSON(); requests.push({ path, payload })
    data = { id: 3, ticketId: 1, ticketNo: 'AS-TEST', sequenceNo: 3, requestKey: payload.requestKey, treatmentType: payload.treatmentType, status: 'waiting_outbound', version: 0, originalOpeningDate: '2020-02-29', details: payload, parts: payload.parts.map(p => ({ ...p, partName: '皮带', lineStatus: 'pending' })) }
    records.push(data)
  } else if (path.endsWith('/treatments')) data = records
  else if (path.endsWith('/treatment-todos')) data = { data: records.filter(r => r.status === 'processing' || r.status === 'waiting_follow_up'), total: 1 }
  else if (path.endsWith('/parts')) data = { data: [{ id: 10, partName: '皮带', modelSpec: 'A', status: 1 }] }
  else data = []
  await route.fulfill({ contentType: 'application/json', body: JSON.stringify({ code: 200, data }) })
})
try {
  const address = server.httpServer.address()
  await page.goto(`http://127.0.0.1:${address.port}/tests/fixtures/after-sales-treatments.html`)
  await page.getByRole('button', { name: '完成本次处理', exact: true }).waitFor()
  assert.equal(await page.locator('#treatment-1').getByRole('button', { name: '补充本次资料' }).count(), 0)
  await page.getByRole('button', { name: '新增处理记录', exact: true }).click()
  await page.getByRole('textbox', { name: '本次处理说明' }).fill('第三次补发')
  await page.getByRole('button', { name: '保存本次处理', exact: true }).click()
  await page.getByText('请添加补发配件', { exact: true }).waitFor()
  assert.equal(requests.length, 0)
  for (const width of [320, 768, 1024, 1440]) {
    await page.setViewportSize({ width, height: 900 })
    const overflow = await page.locator('.el-dialog:visible').evaluate(el => el.scrollWidth > el.clientWidth + 2)
    assert.equal(overflow, false, `dialog overflow at ${width}`)
    await page.screenshot({ path: join(screenshots, `editor-${width}.png`), fullPage: true })
  }
  await page.getByRole('combobox', { name: '本次处理方式' }).focus()
  await page.getByRole('combobox', { name: '本次处理方式' }).press('ArrowDown')
  await page.getByRole('option', { name: '补发配件并更换电机', exact: true }).click()
  await page.getByRole('textbox', { name: '新电机型号' }).fill('M3')
  await page.getByRole('button', { name: '添加配件', exact: true }).click()
  await page.getByRole('combobox', { name: '配件1', exact: true }).fill('皮带')
  await page.getByRole('option', { name: '皮带 / A', exact: true }).click()
  await page.getByRole('button', { name: '保存本次处理', exact: true }).click()
  await page.getByRole('button', { name: '保存本次处理', exact: true }).waitFor({ state: 'hidden' })
  await page.locator('#treatment-3').getByText('待配件出库', { exact: true }).waitFor()
  await page.locator('#treatment-2').getByRole('button', { name: '完成本次处理', exact: true }).click()
  await page.locator('.el-message-box textarea').fill('第二次已更换完成')
  await page.locator('.el-message-box').getByRole('button', { name: /确定|OK/ }).click()
  await page.locator('#treatment-2').getByRole('button', { name: '本次回访', exact: true }).click()
  await page.getByRole('combobox', { name: '客户满意度' }).focus()
  await page.getByRole('combobox', { name: '客户满意度' }).press('ArrowDown')
  await page.getByRole('option', { name: '满意', exact: true }).click()
  await page.getByRole('textbox', { name: '回访内容' }).fill('客户确认正常')
  await page.getByRole('button', { name: '保存回访', exact: true }).click()
  await page.locator('#treatment-2').getByText('回访已解决', { exact: true }).waitFor()
  await page.getByRole('button', { name: '保存回访', exact: true }).waitFor({ state: 'hidden' })
  assert.equal(JSON.stringify(records[0]), original, 'previous treatment and warranty unchanged')
  assert.equal(records[1].originalOpeningDate, '2020-02-29')
  assert.equal(requests.length, 3)
  assert.equal(requests[0].payload.treatmentType, 'parts_and_motor')
  assert.equal('openingDate' in requests[0].payload, false)
  assert.deepEqual(errors, [])
  await page.locator('.el-message').last().waitFor({ state: 'hidden' })
  await page.screenshot({ path: join(screenshots, 'completed.png'), fullPage: true })
  console.log(JSON.stringify({ result: 'passed', screenshots, actions: requests.map(r => r.path) }))
} catch (error) {
  await page.screenshot({ path: join(screenshots, 'failure.png'), fullPage: true })
  console.error(JSON.stringify({ errors, observedBody: (await page.locator('body').innerText()).slice(0, 3000), screenshots }))
  throw error
} finally {
  await context.close(); await browser.close(); await server.close()
}
