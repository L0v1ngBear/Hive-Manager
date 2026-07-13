import request from '@/utils/request'

export function listTenants() {
  return request({
    url: '/platform/tenants',
    method: 'get'
  })
}

export function listTenantFeatures() {
  return request({
    url: '/platform/tenants/features',
    method: 'get'
  })
}

export function updateTenantProfile(id, data) {
  return request({
    url: `/platform/tenants/${id}/profile`,
    method: 'put',
    data
  })
}

export function uploadTenantLogo(id, data) {
  return request({
    url: `/platform/tenants/${id}/logo`,
    method: 'post',
    data
  })
}

export function updateTenantLicense(id, data) {
  return request({
    url: `/platform/tenants/${id}/license`,
    method: 'put',
    data
  })
}

export function updateTenantStatus(id, status) {
  return request({
    url: `/platform/tenants/${id}/status`,
    method: 'put',
    data: { status }
  })
}

export function reassignTenantOwnerAccount(id, data) {
  return request({
    url: `/platform/tenants/${id}/owner-account`,
    method: 'put',
    data
  })
}
