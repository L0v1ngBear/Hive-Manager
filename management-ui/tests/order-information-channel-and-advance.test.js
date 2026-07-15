import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const orderSource = readFileSync(new URL('../src/views/function/order/order.vue', import.meta.url), 'utf8')
const installationSource = readFileSync(new URL('../src/views/function/installationTask/installationTask.vue', import.meta.url), 'utf8')
const manualSource = readFileSync(new URL('../src/views/manual/UserManual.vue', import.meta.url), 'utf8')
const labelSource = readFileSync(new URL('../src/views/function/label.vue', import.meta.url), 'utf8')

function functionSource(source, name, nextName) {
  const start = source.search(new RegExp(`(?:async )?function ${name}\\(`))
  const end = source.slice(start + 1).search(new RegExp(`(?:async )?function ${nextName}\\(`))
  assert.notEqual(start, -1, `${name} must exist`)
  assert.notEqual(end, -1, `${nextName} must follow ${name}`)
  return source.slice(start, start + 1 + end)
}

function sourceBetween(source, startToken, endToken) {
  const start = source.indexOf(startToken)
  const end = source.indexOf(endToken, start + startToken.length)
  assert.notEqual(start, -1, `${startToken} must exist`)
  assert.notEqual(end, -1, `${endToken} must follow ${startToken}`)
  return source.slice(start, end)
}

test('orders use informationChannel everywhere instead of delivery dates', () => {
  assert.match(orderSource, /\{key: 'informationChannel', label: '信息渠道'\}/)
  assert.match(orderSource, /v-model\.trim="filters\.informationChannel"/)
  assert.match(orderSource, /v-model\.trim="orderForm\.informationChannel"[^>]*type="text"/)
  assert.match(orderSource, /informationChannel: blank\(orderForm\.informationChannel\)/)
  assert.match(orderSource, /row\.informationChannel/)
  assert.match(orderSource, /orderDetail\.informationChannel/)
  assert.match(orderSource, /filters\.informationChannel \|\| undefined/)
  assert.match(orderSource, /if \(key === 'informationChannel'\) return row\.informationChannel \|\| ''/)
  assert.doesNotMatch(orderSource, /deliveryDate|deliveryStart|deliveryEnd|toDateInput/)
})

test('installation tasks and the user manual use informationChannel terminology', () => {
  assert.match(installationSource, /row\.informationChannel/)
  assert.match(installationSource, /editorForm\.informationChannel/)
  assert.doesNotMatch(installationSource, /deliveryDate|交付日期/)
  assert.match(manualSource, /信息渠道/)
  assert.doesNotMatch(manualSource, /交付日期|交付时间/)
})

test('drawing budget completion is a separately selectable terminal status', () => {
  assert.match(orderSource, /\{value: 'budget_completed', label: '预算完成'\}/)
  assert.match(orderSource, /key: 'drawing-budget-completed',[\s\S]*status: 'budget_completed'/)
  assert.match(orderSource, /isDrawingBudgetTerminal\(row\)/)
  assert.match(orderSource, /isDrawingBudgetTerminal\(orderForm\)/)
  assert.match(
    orderSource,
    /const canEditDetail = intent\?\.targetStatus \? canAdvanceOrder\(detail\) : canEditOrder\(detail\)/
  )
  assert.match(orderSource, /if \(!canEditDetail\)/)
})

test('advance intent saves the current status before attempting the next-stage request', () => {
  const advanceSource = functionSource(orderSource, 'advanceOrder', 'rollbackOrder')
  const submitSource = functionSource(orderSource, 'submitForm', 'validateOrderForm')

  assert.match(advanceSource, /await openEdit\(row\.orderId, row, \{targetStatus\}\)/)
  assert.doesNotMatch(advanceSource, /advanceOrderNextStage/)
  assert.match(orderSource, /const advanceIntent = ref\(null\)/)
  assert.match(orderSource, /const editingOrderStatus = ref\(''\)/)
  assert.match(orderSource, /createOrderAdvancePlan\(basePayload, editingOrderStatus\.value, advanceIntent\.value\.targetStatus\)/)
  assert.match(submitSource, /await saveOrder\(editingOrderId\.value, payload\)/)
  assert.match(submitSource, /await advanceOrderNextStage\(editingOrderId\.value, advancePayload\)/)
  assert.ok(
    submitSource.indexOf('await saveOrder(editingOrderId.value, payload)') < submitSource.indexOf('await advanceOrderNextStage(editingOrderId.value, advancePayload)'),
    'the order must save before it advances'
  )
  assert.match(submitSource, /订单已保存，流转未完成，请重试/)
})

test('order list replaces remark with the form logistics tracking number', () => {
  const informationChannelCell = sourceBetween(
    orderSource,
    `<template v-else-if="column.key === 'informationChannel'">`,
    `<template v-else-if="column.key === 'invoice'">`
  )
  const exportSource = functionSource(orderSource, 'formatOrderExportCell', 'openDetail')

  assert.match(orderSource, /\{key: 'expressNo', label: '物流单号'\}/)
  assert.doesNotMatch(orderSource, /\{key: 'remark', label: '备注'\}/)
  assert.match(orderSource, /column\.key === 'expressNo'[\s\S]*row\.expressNo \|\| '未填写物流单号'/)
  assert.match(informationChannelCell, /row\.informationChannel \|\| '未填写信息渠道'/)
  assert.doesNotMatch(informationChannelCell, /row\.(?:expressCompany|expressNo)/)
  assert.match(exportSource, /if \(key === 'expressNo'\) return row\.expressNo \|\| ''/)
  assert.doesNotMatch(exportSource, /key === 'remark'/)
  assert.match(orderSource, /class="order-information-channel-cell text-sm text-on-surface-variant"/)
  assert.match(orderSource, /\.order-column-informationChannel\s*\{[\s\S]*?min-width:\s*11rem/)
  assert.match(orderSource, /\.order-column-expressNo\s*\{[\s\S]*?min-width:\s*11rem/)
  assert.match(orderSource, /\.order-information-channel-cell\s*\{[\s\S]*?overflow-wrap:\s*anywhere/)
  assert.match(orderSource, /\.order-express-number-cell\s*\{[\s\S]*?overflow-wrap:\s*anywhere/)
  assert.match(orderSource, /\.order-list-table\.responsive-data-table\s*\{[\s\S]*?table-layout:\s*auto/)
  assert.match(orderSource, /useLocalTableColumns\('order\.list\.commercial\.v4'/)
})

test('shipping advance requires logistics fields within the edit dialog', () => {
  assert.match(orderSource, /advanceIntent\.value\?\.targetStatus === 'shipped'/)
  assert.match(orderSource, /v-if="requiresShippingDetails"/)
  assert.match(orderSource, /if \(requiresShippingDetails\.value && !String\(orderForm\.expressCompany/)
  assert.match(orderSource, /if \(requiresShippingDetails\.value && !String\(orderForm\.expressNo/)
})

test('advance success messages distinguish shipping approval, material approval, and direct advance', () => {
  const submitSource = functionSource(orderSource, 'submitForm', 'validateOrderForm')

  assert.match(orderSource, /isOrderShippingApprovalTransition/)
  assert.match(submitSource, /advancePlan\.successMessage/)
  assert.doesNotMatch(submitSource, /orderForm\.status\s*=\s*advanceIntent/)
  assert.ok(
    submitSource.indexOf('await advanceOrderNextStage(editingOrderId.value, advancePayload)') < submitSource.indexOf('await loadOrders()'),
    'the list must refresh only after the next-stage request returns'
  )
})

test('web order flow QR uses only canonical values and shows invalid-data feedback', () => {
  assert.doesNotMatch(orderSource, /buildOrderFlowQrTextForWeb|flowScanCode \|\| order\.flowBarcode \|\| order\.flowCode/)
  assert.doesNotMatch(orderSource, /payload\.flowQrPayload \|\| payload\.flowScanCode \|\| payload\.flowBarcode \|\| payload\.flowCode/)
  assert.match(orderSource, /selectOrderFlowQrValue\(order\.flowScanCode, order\.flowBarcode\)/)
  assert.match(orderSource, /selectOrderFlowQrValue\(payload\.flowQrPayload, payload\.flowScanCode, payload\.flowBarcode\)/)
  assert.match(labelSource, /selectOrderFlowQrValue\(target\.flowQrPayload, target\.flowScanCode, target\.flowBarcode\)/)
  assert.doesNotMatch(labelSource, /target\.flowQrPayload \|\| target\.flowScanCode \|\| target\.flowBarcode \|\| target\.flowCode/)
  assert.match(labelSource, /流转二维码格式无效，无法生成二维码/)
  assert.match(labelSource, /流转二维码格式无效，无法预览或打印/)
})

test('installation task table labels information channel and logistics consistently', () => {
  assert.match(installationSource, /<el-table-column label="信息渠道与物流"/)
  assert.match(installationSource, /row\.informationChannel/)
})

test('filter overview collapses through an accessible click button and retains the active summary', () => {
  assert.match(orderSource, /const filterOverviewExpanded = ref\(true\)/)
  assert.match(orderSource, /const activeFilterSummary = computed\(/)
  assert.match(orderSource, /:aria-expanded="filterOverviewExpanded"/)
  assert.match(orderSource, /aria-controls="order-filter-overview order-filter-details"/)
  assert.match(orderSource, /@click="filterOverviewExpanded = !filterOverviewExpanded"/)
  assert.match(orderSource, /\{\{ activeFilterSummary \}\}/)
  assert.match(orderSource, /v-show="filterOverviewExpanded" id="order-filter-overview"/)
  assert.match(orderSource, /v-show="filterOverviewExpanded" id="order-filter-details"/)
  assert.doesNotMatch(orderSource, /@(mouseenter|mouseover|touchstart)="[^"]*select/)
})

test('drawing budget cards are independent from ordinary status and category cards', () => {
  const statusTabsSource = sourceBetween(orderSource, 'const currentStatusTabs = computed', 'const orderWarningHint = computed')
  const summarySource = sourceBetween(orderSource, 'const summaryCards = computed', 'const categorySummaryCards = computed')
  const categorySource = sourceBetween(orderSource, 'const categorySummaryCards = computed', 'const drawingBudgetSummaryCards = computed')
  const drawingSource = sourceBetween(orderSource, 'const drawingBudgetSummaryCards = computed', 'const selectedCustomerOption = computed')

  assert.match(statusTabsSource, /filter\(status => !\['budgeting', 'budget_completed'\]\.includes\(status\.value\)\)/)
  assert.doesNotMatch(summarySource, /order-budgeting|status: 'budgeting'/)
  assert.doesNotMatch(summarySource, /order-budget-completed|status: 'budget_completed'/)
  assert.match(categorySource, /filter\(option => option\.value !== 'drawing_budget'\)/)
  assert.doesNotMatch(categorySource, /图纸预算订单/)
  assert.match(drawingSource, /key: 'drawing-budget-total'[\s\S]*count: orderSummary\.category_drawing_budget \|\| 0/)
  assert.match(drawingSource, /key: 'drawing-budget-budgeting'[\s\S]*status: 'budgeting'[\s\S]*count: orderSummary\.budgeting \|\| 0/)
  assert.match(drawingSource, /key: 'drawing-budget-completed'[\s\S]*status: 'budget_completed'[\s\S]*count: orderSummary\.budget_completed \|\| 0/)
  assert.match(orderSource, /@click="selectDrawingBudgetCard\(card\)"/)
  assert.match(orderSource, /filters\.orderCategory = 'drawing_budget'[\s\S]*filters\.status = card\.status \|\| ''/)
})

test('drawing budget cards stay in one responsive row without mobile overflow', () => {
  assert.match(orderSource, /\.drawing-budget-summary-grid\s*\{[\s\S]*grid-template-columns: repeat\(3, minmax\(0, 1fr\)\)/)
  assert.match(orderSource, /@media \(max-width: 768px\)[\s\S]*\.drawing-budget-summary-grid\s*\{[\s\S]*overflow-x: auto/)
  assert.match(orderSource, /\.order-filter-overview-copy\s*\{[\s\S]*min-width: 0/)
})
