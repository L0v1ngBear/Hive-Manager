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

export function updateTenantLicense(data) {
  return request({
    url: '/platform/tenant/license',
    method: 'post',
    data
  })
}

export function getTenantFeatureCatalog() {
  return request({
    url: '/platform/tenant/features/catalog',
    method: 'get'
  })
}
