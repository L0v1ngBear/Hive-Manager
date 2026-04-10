import request from '@/utils/request.js'

export function getPendingPrintOrders() {
  return request({ url: '/receipt/print/pending', method: 'get' })
}

export function getPrintDetail(params) {
  return request({ url: '/receipt/print/detail', method: 'get', params })
}

export function markPrinted(params) {
  return request({ url: '/receipt/print/mark-printed', method: 'post', params })
}

export function cancelPrint(params) {
  return request({ url: '/receipt/print/cancel', method: 'post', params })
}