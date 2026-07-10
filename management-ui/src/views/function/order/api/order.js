import request from '@/utils/request.js'

export function getOrderPage(params) {
  return request({ url: '/order/page', method: 'get', params })
}

export function getOrderStatusSummary() {
  return request({ url: '/order/status-summary', method: 'get' })
}

export function getOrderDetail(orderId) {
  return request({ url: `/order/detail/${orderId}`, method: 'get' })
}

export function createOrder(data) {
  return request({ url: '/order/create', method: 'post', data })
}

export function uploadOrderAttachment(data) {
  return request({
    url: '/order/attachment/upload',
    method: 'post',
    data,
    timeout: 30000
  })
}

export function downloadOrderAttachment(params) {
  return request({
    url: '/order/attachment/download',
    method: 'get',
    params,
    responseType: 'blob',
    timeout: 30000
  })
}

export function saveOrder(orderId, data) {
  return request({ url: `/order/save/${orderId}`, method: 'post', data })
}

export function updateOrder(orderId, data) {
  return request({ url: `/order/update/${orderId}`, method: 'post', data })
}

export function advanceOrderNextStage(orderId, data) {
  return request({ url: `/order/next/${orderId}`, method: 'post', data })
}

export function submitOrderRollback(orderId, data) {
  return request({ url: `/order/rollback/${orderId}`, method: 'post', data })
}

export function createOrderFlowPrintTask(data) {
  return request({ url: '/order/flow-print-task', method: 'post', data })
}

export function correctOrderLogTime(logId, data) {
  return request({ url: `/order/log/${logId}/time`, method: 'post', data })
}

export function getOrderWarningSetting() {
  return request({ url: '/order/warning/setting', method: 'get' })
}

export function updateOrderWarningSetting(data) {
  return request({ url: '/order/warning/setting', method: 'post', data })
}

export function getOrderWarningSummary() {
  return request({ url: '/order/warning/summary', method: 'get' })
}

export function refreshOrderWarningSummary() {
  return request({ url: '/order/warning/refresh', method: 'post' })
}

export function refreshOrderWarnings() {
  return request({ url: '/order/warning/refresh-all', method: 'post' })
}

export function refreshOrderWarning(orderId) {
  return request({
    url: `/order/warning/${encodeURIComponent(orderId)}/refresh`,
    method: 'post'
  })
}

export function checkOrderModuleHealth() {
  return request({ url: '/order/health', method: 'get' })
}
