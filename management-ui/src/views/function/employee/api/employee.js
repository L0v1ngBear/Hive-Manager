import request from '@/utils/request.js'

export function getEmployeePage(params) {
  return request({
    url: '/emp/employee/page',
    method: 'get',
    params
  })
}

export function getEmployeeStats() {
  return request({
    url: '/emp/employee/stats',
    method: 'get'
  })
}

export function getEmployeeDetail(id) {
  return request({
    url: `/emp/employee/${id}`,
    method: 'get'
  })
}

export function createEmployee(data) {
  return request({
    url: '/emp/employee/create',
    method: 'post',
    data
  })
}

export function updateEmployee(data) {
  return request({
    url: '/emp/employee/update',
    method: 'post',
    data
  })
}

export function changeEmployeeStatus(data) {
  return request({
    url: '/emp/employee/change-status',
    method: 'post',
    data
  })
}

export function batchUpdateEmployees(data) {
  return request({
    url: '/emp/employee/batch-update',
    method: 'post',
    data
  })
}

export function deleteEmployee(id) {
  return request({
    url: `/emp/employee/${id}`,
    method: 'delete'
  })
}

export function searchEmployeeLeaders(params) {
  return request({
    url: '/emp/employee/leader/search',
    method: 'get',
    params
  })
}

export function getEmployeeFormOptions() {
  return request({
    url: '/emp/employee/init-form-options',
    method: 'get'
  })
}

export function exportEmployees(data) {
  return request({
    url: '/emp/employee/export',
    method: 'post',
    data
  })
}

export function exportEmployeesExcel(params) {
  return request({
    url: '/emp/employee/export-excel',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function downloadEmployeeImportTemplate() {
  return request({
    url: '/emp/employee/import-template',
    method: 'get',
    responseType: 'blob'
  })
}

export function importEmployees(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/emp/employee/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function createOrganizationJoinCode() {
  return request({
    url: '/organization/join-code',
    method: 'post'
  })
}
