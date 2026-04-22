import request from '@/utils/request.js'

export function getPendingPrintOrders() {
  return request({ url: '/receipt/print/pending', method: 'get' })
}

export function getPrintDetail(params) {
  return request({ url: '/receipt/print/detail', method: 'get', params })
}

export function getRawPrintCommand(params) {
  return request({ url: '/receipt/print/raw-command', method: 'get', params })
}

export function markPrinted(params) {
  return request({ url: '/receipt/print/mark-printed', method: 'post', params })
}

export function cancelPrint(params) {
  return request({ url: '/receipt/print/cancel', method: 'post', params })
}

export function listReceiptTemplateVariables() {
  return request({ url: '/receipt/template/variables', method: 'get' })
}

export function listReceiptTemplates() {
  return request({ url: '/receipt/template/list', method: 'get' })
}

export function saveReceiptTemplate(data) {
  return request({ url: '/receipt/template/save', method: 'post', data })
}

export function setDefaultReceiptTemplate(id) {
  return request({ url: `/receipt/template/${id}/default`, method: 'post' })
}
