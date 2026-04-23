import request from '@/utils/request.js'

export function getAttendanceSummary(params) {
  return request({ url: '/attendance/summary', method: 'get', params })
}

export function getAttendancePage(params) {
  return request({ url: '/attendance/page', method: 'get', params })
}

export function getAttendanceDepartments() {
  return request({ url: '/attendance/departments', method: 'get' })
}

export function getAttendanceRule() {
  return request({ url: '/attendance/rule', method: 'get' })
}

export function saveAttendanceRule(data) {
  return request({ url: '/attendance/rule/save', method: 'post', data })
}

export function exportAttendanceExcel(params) {
  return request({ url: '/attendance/export-excel', method: 'get', params, responseType: 'blob' })
}
