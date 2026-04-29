import request from '@/utils/request.js'

export function getSalesOrderPage(params) {
  return request({
    url: '/order/sales/page',
    method: 'get',
    params
  })
}

export function getSalesOrderDetail(orderId) {
  return request({
    url: `/order/sales/detail/${orderId}`,
    method: 'get'
  })
}

export function createSalesOrder(data) {
  return request({
    url: '/order/sales/create',
    method: 'post',
    data
  })
}

export function uploadSalesOrderAttachment(data) {
  return request({
    url: '/order/sales/attachment/upload',
    method: 'post',
    data,
    timeout: 30000
  })
}

export function downloadSalesOrderAttachment(params) {
  return request({
    url: '/order/sales/attachment/download',
    method: 'get',
    params,
    responseType: 'blob',
    timeout: 30000
  })
}

export function saveSalesOrder(orderId, data) {
  return request({
    url: `/order/sales/save/${orderId}`,
    method: 'post',
    data
  })
}

export function updateSalesOrder(orderId, data) {
  return request({
    url: `/order/sales/update/${orderId}`,
    method: 'post',
    data
  })
}

export function getProductionOrderPage(params) {
  return request({
    url: '/order/production/page',
    method: 'get',
    params
  })
}

export function getProductionOrderDetail(orderId) {
  return request({
    url: `/order/production/detail/${orderId}`,
    method: 'get'
  })
}

export function createProductionOrder(data) {
  return request({
    url: '/order/production/create',
    method: 'post',
    data
  })
}

export function saveProductionOrder(orderId, data) {
  return request({
    url: `/order/production/save/${orderId}`,
    method: 'post',
    data
  })
}

export function updateProductionOrder(orderId, data) {
  return request({
    url: `/order/production/update/${orderId}`,
    method: 'post',
    data
  })
}

export function checkOrderModuleHealth() {
  return request({
    url: '/order/health',
    method: 'get'
  })
}
