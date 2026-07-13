import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const orderSource = readFileSync(new URL('../src/views/function/order/order.vue', import.meta.url), 'utf8')
const installationSource = readFileSync(new URL('../src/views/function/installationTask/installationTask.vue', import.meta.url), 'utf8')
const manualSource = readFileSync(new URL('../src/views/manual/UserManual.vue', import.meta.url), 'utf8')

function functionSource(source, name, nextName) {
  const start = source.search(new RegExp(`(?:async )?function ${name}\\(`))
  const end = source.slice(start + 1).search(new RegExp(`(?:async )?function ${nextName}\\(`))
  assert.notEqual(start, -1, `${name} must exist`)
  assert.notEqual(end, -1, `${nextName} must follow ${name}`)
  return source.slice(start, start + 1 + end)
}

test('orders use informationChannel everywhere instead of delivery dates', () => {
  assert.match(orderSource, /\{key: 'informationChannel', label: '信息渠道'\}/)
  assert.match(orderSource, /v-model\.trim="filters\.informationChannel"/)
  assert.match(orderSource, /v-model\.trim="orderForm\.informationChannel"[^>]*type="text"/)
  assert.match(orderSource, /informationChannel: blank\(orderForm\.informationChannel\)/)
  assert.match(orderSource, /row\.informationChannel/)
  assert.match(orderSource, /orderDetail\.informationChannel/)
  assert.match(orderSource, /filters\.informationChannel \|\| undefined/)
  assert.match(orderSource, /if \(key === 'informationChannel'\) return \[row\.informationChannel/)
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
  assert.match(orderSource, /key: 'order-budget-completed',[\s\S]*status: 'budget_completed'/)
  assert.match(orderSource, /const drawingBudgetStatusFlow = \['budgeting', 'budget_completed'\]/)
  const previousSource = functionSource(orderSource, 'previousOrderStatus', 'isOrderMaterialApprovalTransition')
  assert.match(previousSource, /if \(row\.orderCategory === 'drawing_budget'\) \{\s*return ''\s*\}/)
})

test('advance intent saves the current status before attempting the next-stage request', () => {
  const advanceSource = functionSource(orderSource, 'advanceOrder', 'rollbackOrder')
  const submitSource = functionSource(orderSource, 'submitForm', 'validateOrderForm')

  assert.match(advanceSource, /await openEdit\(row\.orderId, row, \{targetStatus\}\)/)
  assert.doesNotMatch(advanceSource, /advanceOrderNextStage/)
  assert.match(orderSource, /const advanceIntent = ref\(null\)/)
  assert.match(orderSource, /const editingOrderStatus = ref\(''\)/)
  assert.match(orderSource, /payload\.status = advanceIntent\.value \? editingOrderStatus\.value : payload\.status/)
  assert.match(submitSource, /await saveOrder\(editingOrderId\.value, payload\)/)
  assert.match(submitSource, /await advanceOrderNextStage\(editingOrderId\.value, advancePayload\)/)
  assert.ok(
    submitSource.indexOf('await saveOrder(editingOrderId.value, payload)') < submitSource.indexOf('await advanceOrderNextStage(editingOrderId.value, advancePayload)'),
    'the order must save before it advances'
  )
  assert.match(submitSource, /订单已保存，流转未完成，请重试/)
})

test('shipping advance requires logistics fields within the edit dialog', () => {
  assert.match(orderSource, /advanceIntent\.value\?\.targetStatus === 'shipped'/)
  assert.match(orderSource, /v-if="requiresShippingDetails"/)
  assert.match(orderSource, /if \(requiresShippingDetails\.value && !String\(orderForm\.expressCompany/)
  assert.match(orderSource, /if \(requiresShippingDetails\.value && !String\(orderForm\.expressNo/)
})
