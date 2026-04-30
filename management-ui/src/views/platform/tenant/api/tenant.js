import request from '@/utils/request'

export function getTenantPage(params) {
  return request({
    url: '/platform/tenant/page',
    method: 'get',
    params
  })
}

export function createTenant(data) {
  return request({
    url: '/platform/tenant/create',
    method: 'post',
    data
  })
}
