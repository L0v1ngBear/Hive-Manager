import request from '@/utils/request.js'

export function getOrderPage(params) {
  return request({ url: '/orders', method: 'get', params })
}

export function getOrderStatusSummary() {
  return request({ url: '/orders/status-summary', method: 'get' })
}

export function getOrderDetail(orderId) {
  return request({ url: `/orders/${orderId}`, method: 'get' })
}

export function createOrder(data) {
  return request({ url: '/orders', method: 'post', data })
}

export function uploadOrderAttachment(data) {
  return request({
    url: '/orders/attachment',
    method: 'post',
    data,
    timeout: 30000
  })
}

export function downloadOrderAttachment(params) {
  return request({
    url: '/orders/attachment',
    method: 'get',
    params,
    responseType: 'blob',
    timeout: 30000
  })
}

export function saveOrder(orderId, data) {
  return request({ url: `/orders/${orderId}/save`, method: 'post', data })
}

export function updateOrder(orderId, data) {
  return request({ url: `/orders/${orderId}/status`, method: 'post', data })
}

export function advanceOrderNextStage(orderId, data) {
  return request({ url: `/orders/${orderId}/advance`, method: 'post', data })
}

export function submitOrderRollback(orderId, data) {
  return request({ url: `/orders/${orderId}/rollback`, method: 'post', data })
}

export function createOrderFlowPrintTask(data) {
  return request({ url: '/orders/flow-print-task', method: 'post', data })
}

export function correctOrderLogTime(logId, data) {
  return request({ url: `/orders/status-log/${logId}/time`, method: 'post', data })
}

export function getOrderWarningSetting() {
  return request({ url: '/orders/warning/setting', method: 'get' })
}

export function updateOrderWarningSetting(data) {
  return request({ url: '/orders/warning/setting', method: 'post', data })
}

export function getOrderWarningSummary() {
  return request({ url: '/orders/warning/summary', method: 'get' })
}

export function refreshOrderWarningSummary() {
  return request({ url: '/orders/warning/refresh', method: 'post' })
}

export function refreshOrderWarnings() {
  return request({ url: '/orders/warning/refresh-all', method: 'post' })
}

export function refreshOrderWarning(orderId) {
  return request({
    url: `/orders/warning/${encodeURIComponent(orderId)}/refresh`,
    method: 'post'
  })
}
