import request from '@/utils/request.js'

export function getCustomerPage(params) {
  return request({
    url: '/customer/page',
    method: 'get',
    params
  })
}

export function createCustomer(data) {
  return request({
    url: '/customer/add',
    method: 'post',
    data
  })
}

export function getCustomerDetail(id) {
  return request({
    url: `/customer/detail/${id}`,
    method: 'get'
  })
}