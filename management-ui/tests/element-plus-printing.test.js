import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const receipt = readFileSync(new URL('../src/views/function/receipt.vue', import.meta.url), 'utf8')
const label = readFileSync(new URL('../src/views/function/label.vue', import.meta.url), 'utf8')

function assertComponents(source, components, page) {
  for (const component of components) {
    const tag = component.replace(/^El/, '').replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase()
    assert.match(source, new RegExp(`<el-${tag}(?:\\s|>)`), `${page} must render ${component}`)
    assert.match(source, new RegExp(`\\b${component}\\b[\\s\\S]*from ['\"]element-plus['\"]|import \\{[\\s\\S]*\\b${component}\\b[\\s\\S]*\\} from ['\"]element-plus['\"]`), `${page} must explicitly import ${component}`)
  }
}

test('receipt migrates only peripheral controls and preserves native print output', () => {
  assertComponents(receipt, ['ElTabs', 'ElTabPane', 'ElForm', 'ElFormItem', 'ElInput', 'ElInputNumber', 'ElCheckbox', 'ElSelect', 'ElOption', 'ElButton', 'ElEmpty', 'ElResult', 'ElTooltip'], 'receipt')
  assert.doesNotMatch(receipt, /<input\b/, 'receipt peripheral editors must use Element Plus inputs')
  assert.match(receipt, /<table class="receipt-print-table">/)
  assert.match(receipt, /id="print-paper-area" class="paper-stack"/)
  assert.match(receipt, /printWindow\.document\.write\(buildPrintHtml\(printable\.innerHTML\)\)/)
  assert.match(receipt, /setTimeout\(\(\) => printWindow\.print\(\), 300\)/)
  assert.match(receipt, /@page/)
  assert.match(receipt, /page-break-after/)
})

test('receipt exposes real permissions and mutually exclusive latest-request states', () => {
  assert.match(receipt, /useUserStore/)
  for (const permission of ['print:receipt:detail', 'print:receipt:execute', 'print:receipt:update', 'print:receipt:cancel']) assert.match(receipt, new RegExp(permission.replaceAll(':', '\\:')))
  assert.match(receipt, /listLoadError/)
  assert.match(receipt, /detailLoadError/)
  assert.match(receipt, /listRequestId/)
  assert.match(receipt, /receiptDetailRequestController/)
  assert.match(receipt, /retryPendingList/)
  assert.match(receipt, /retrySelectedOrder/)
})

test('receipt detail loading is released immediately when its context is invalidated', async () => {
  const { createReceiptDetailRequestController } = await import('../src/views/function/receipt/receiptDetailRequest.js')
  const loadingStates = []
  let cleared = 0
  const controller = createReceiptDetailRequestController({
    setLoading: (value) => loadingStates.push(value),
    clearDetail: () => { cleared += 1 }
  })

  const staleRequest = controller.begin()
  controller.invalidate()

  assert.deepEqual(loadingStates, [true, false])
  assert.equal(cleared, 2)
  assert.equal(controller.isCurrent(staleRequest), false)
  assert.equal(controller.finish(staleRequest), false)
  assert.deepEqual(loadingStates, [true, false], 'stale finally must not change the invalidated context')

  const olderRequest = controller.begin()
  const newestRequest = controller.begin()
  assert.equal(controller.finish(olderRequest), false)
  assert.equal(loadingStates.at(-1), true, 'an older finally must not stop the newest request loading')
  assert.equal(controller.finish(newestRequest), true)
  assert.equal(loadingStates.at(-1), false)
})

test('label migrates only peripheral controls and preserves QR barcode and print reporting', () => {
  assertComponents(label, ['ElTabs', 'ElTabPane', 'ElBadge', 'ElForm', 'ElFormItem', 'ElInput', 'ElInputNumber', 'ElSwitch', 'ElSelect', 'ElOption', 'ElButton', 'ElEmpty', 'ElResult', 'ElTooltip'], 'label')
  assert.match(label, /import QRCode from 'qrcode'/)
  assert.match(label, /import JsBarcode from 'jsbarcode'/)
  assert.match(label, /ref="printAreaRef"[\s\S]*class="thermal-label"/)
  assert.match(label, /buildLabelPrintHtml\(labelNode\.outerHTML, profile\)/)
  assert.match(label, /await reportPrintTask\(/)
  assert.match(label, /printWindow\.addEventListener\('afterprint', finishPrint\)/)
})

test('label hides unauthorized content and exposes retryable latest-request states', () => {
  assert.match(label, /useUserStore/)
  assert.match(label, /print:label:create/)
  assert.match(label, /print:label:update/)
  assert.match(label, /equipment:list/)
  assert.match(label, /loadError/)
  assert.match(label, /loadRequestId/)
  assert.match(label, /retryCurrentTab/)
  assert.match(label, /v-if="[^"]*canListEquipment[^"]*"/)
})

test('equipment overview never requests protected equipment data without equipment:list', async () => {
  const { loadEquipmentOverviewCount } = await import('../src/views/function/label/labelOverviewAccess.js')
  let calls = 0
  const count = await loadEquipmentOverviewCount({
    canListEquipment: false,
    getEquipmentPage: async () => { calls += 1; return { total: 9 } }
  })
  assert.equal(calls, 0)
  assert.equal(count, null)
})

test('equipment overview requests equipment data when equipment:list is granted', async () => {
  const { loadEquipmentOverviewCount } = await import('../src/views/function/label/labelOverviewAccess.js')
  let calls = 0
  const count = await loadEquipmentOverviewCount({
    canListEquipment: true,
    getEquipmentPage: async () => { calls += 1; return { total: 9 } }
  })
  assert.equal(calls, 1)
  assert.equal(count, 9)
})

test('label wires equipment overview and content to equipment:list without fake counts', () => {
  assert.match(label, /hasPermission\('equipment:list'\)/)
  assert.match(label, /loadEquipmentOverviewCount/)
  assert.match(label, /if \(equipmentCount === null\)[\s\S]*updateTabCount\('equipment_inspection', 0\)/)
  assert.doesNotMatch(label, /hasPermission\('equipment:view'\)/)
})
