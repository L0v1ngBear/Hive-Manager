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
