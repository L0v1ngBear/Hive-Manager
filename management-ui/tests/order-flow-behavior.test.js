import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const moduleUrl = new URL('../src/views/function/order/orderFlow.js', import.meta.url)

async function loadOrderFlow() {
  assert.ok(existsSync(fileURLToPath(moduleUrl)), 'order flow pure-function module must exist')
  return import(moduleUrl)
}

test('advance plan preserves the old status before next and selects the approval message', async () => {
  const { createOrderAdvancePlan } = await loadOrderFlow()
  const payload = { status: 'shipped', expressCompany: 'Hive Logistics', expressNo: 'HIVE-001' }

  const shippingPlan = createOrderAdvancePlan(payload, 'pending_ship', 'shipped')
  assert.equal(shippingPlan.savePayload.status, 'pending_ship')
  assert.equal(shippingPlan.targetStatus, 'shipped')
  assert.equal(shippingPlan.successMessage, '已提交发货审批，审批通过后进入已发货')
  assert.equal(payload.status, 'shipped', 'the source payload must not be mutated')

  const materialPlan = createOrderAdvancePlan(payload, 'pending_pay', 'pending_material')
  assert.equal(materialPlan.successMessage, '已提交订单审批，审批通过后进入备料中')

  const directPlan = createOrderAdvancePlan(payload, 'pending_confirm', 'pending_pay')
  assert.equal(directPlan.successMessage, '订单已推进到下一阶段')
})

test('completed drawing budgets are terminal for edit, rollback, and advance', async () => {
  const { isDrawingBudgetTerminal, nextOrderStatus, previousOrderStatus } = await loadOrderFlow()
  const terminalOrder = { orderCategory: 'drawing_budget', status: 'budget_completed' }

  assert.equal(isDrawingBudgetTerminal(terminalOrder), true)
  assert.equal(nextOrderStatus(terminalOrder), '')
  assert.equal(previousOrderStatus(terminalOrder), '')
  assert.equal(nextOrderStatus({ orderCategory: 'drawing_budget', status: 'budgeting' }), 'budget_completed')
  assert.equal(previousOrderStatus({ orderCategory: 'drawing_budget', status: 'budgeting' }), '')
  assert.equal(isDrawingBudgetTerminal({ orderCategory: 'bulk', status: 'budget_completed' }), false)
})

test('web order flow QR accepts only the exact canonical value and returns it unchanged', async () => {
  const { selectOrderFlowQrValue } = await loadOrderFlow()
  const token = 'Abcdefghijklmnopqrstuvwxyz0123456789_-ABCDE'
  assert.equal(token.length, 43)
  const salesCode = `HIVE_ORDER_FLOW:sales:${token}:SO-1001`
  const productionCode = `HIVE_ORDER_FLOW:production:${token}:PO_1001`

  assert.equal(selectOrderFlowQrValue(salesCode), salesCode)
  assert.equal(selectOrderFlowQrValue(productionCode), productionCode)
  assert.equal(selectOrderFlowQrValue('', salesCode), salesCode)

  for (const invalid of [
    JSON.stringify({ flowScanCode: salesCode }),
    'FLOW202607130001',
    `HIVE_ORDER_FLOW:order:${token}:SO-1001`,
    `HIVE_ORDER_FLOW:sales:${token.slice(1)}:SO-1001`,
    `HIVE_ORDER_FLOW:sales:${token}=:SO-1001`,
    ` ${salesCode}`,
    `${salesCode} `,
    `${salesCode}:extra`
  ]) {
    assert.equal(selectOrderFlowQrValue(invalid), '', `must reject: ${invalid}`)
  }
})
