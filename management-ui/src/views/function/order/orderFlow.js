const NORMAL_ORDER_STATUS_FLOW = [
  'pending_confirm',
  'pending_pay',
  'pending_material',
  'producing',
  'pending_ship',
  'shipped',
  'completed'
]
const DRAWING_BUDGET_STATUS_FLOW = ['budgeting', 'budget_completed']
const ORDER_FLOW_QR_PATTERN = /^HIVE_ORDER_FLOW:(sales|production):[A-Za-z0-9_-]{43}:[^:\s]+$/

export function isDrawingBudgetTerminal(order = {}) {
  return order.orderCategory === 'drawing_budget' && order.status === 'budget_completed'
}

export function nextOrderStatus(order = {}) {
  if (isDrawingBudgetTerminal(order)) return ''
  const flow = order.orderCategory === 'drawing_budget'
    ? DRAWING_BUDGET_STATUS_FLOW
    : NORMAL_ORDER_STATUS_FLOW
  const currentIndex = flow.indexOf(order.status)
  return currentIndex >= 0 && currentIndex < flow.length - 1 ? flow[currentIndex + 1] : ''
}

export function previousOrderStatus(order = {}) {
  if (order.orderCategory === 'drawing_budget') return ''
  const currentIndex = NORMAL_ORDER_STATUS_FLOW.indexOf(order.status)
  return currentIndex > 0 ? NORMAL_ORDER_STATUS_FLOW[currentIndex - 1] : ''
}

export function isOrderMaterialApprovalTransition(order = {}, targetStatus = nextOrderStatus(order)) {
  return order.status === 'pending_pay' && targetStatus === 'pending_material'
}

export function isOrderShippingApprovalTransition(order = {}, targetStatus = nextOrderStatus(order)) {
  return order.status === 'pending_ship' && targetStatus === 'shipped'
}

export function orderAdvanceSuccessMessage(currentStatus, targetStatus) {
  if (currentStatus === 'pending_ship' && targetStatus === 'shipped') {
    return '已提交发货审批，审批通过后进入已发货'
  }
  if (currentStatus === 'pending_pay' && targetStatus === 'pending_material') {
    return '已提交订单审批，审批通过后进入备料中'
  }
  return '订单已推进到下一阶段'
}

export function createOrderAdvancePlan(payload = {}, currentStatus = '', targetStatus = '') {
  return {
    savePayload: {...payload, status: currentStatus},
    targetStatus,
    successMessage: orderAdvanceSuccessMessage(currentStatus, targetStatus)
  }
}

export function selectOrderFlowQrValue(...candidates) {
  for (const candidate of candidates) {
    if (typeof candidate === 'string' && ORDER_FLOW_QR_PATTERN.test(candidate)) {
      return candidate
    }
  }
  return ''
}
