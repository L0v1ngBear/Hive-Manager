import request from '@/utils/request'

export function getCurrentTenantFieldConfig(moduleCode) {
  return request({
    url: '/tenant/field-config',
    method: 'get',
    params: { moduleCode }
  })
}
