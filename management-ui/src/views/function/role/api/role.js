import request from '@/utils/request.js'

export function getRolePage(params) {
  return request({
    url: '/sys/role/page',
    method: 'get',
    params
  })
}

export function getAllPermissions() {
  return request({
    url: '/sys/role/role/all',
    method: 'get'
  })
}

export function createRole(data) {
  return request({
    url: '/sys/role/create',
    method: 'post',
    data
  })
}

export function updateRolePermissions(data) {
  return request({
    url: '/sys/role/role/update',
    method: 'post',
    data
  })
}

export function getRolePermissionIds(roleId) {
  return request({
    url: `/sys/role/${roleId}/permission-ids`,
    method: 'get'
  })
}