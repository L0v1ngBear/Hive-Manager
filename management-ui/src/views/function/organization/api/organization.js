import request from '@/utils/request.js'

export function getOrganizationOverview() {
  return request({ url: '/organization/overview', method: 'get' })
}

export function getDepartmentEmployees(departmentId) {
  return request({ url: `/organization/department/${departmentId}/employees`, method: 'get' })
}

export function saveDepartment(data) {
  return request({ url: '/organization/department/save', method: 'post', data })
}

export function deleteDepartment(departmentId) {
  return request({ url: `/organization/department/${departmentId}`, method: 'delete' })
}

export function getDepartmentPositions(departmentId) {
  return request({ url: `/organization/department/${departmentId}/positions`, method: 'get' })
}

export function savePosition(data) {
  return request({ url: '/organization/position/save', method: 'post', data })
}

export function deletePosition(positionId) {
  return request({ url: `/organization/position/${positionId}`, method: 'delete' })
}
