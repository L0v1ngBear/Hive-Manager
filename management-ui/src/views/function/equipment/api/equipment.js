import request from '@/utils/request.js'

export function getEquipmentPage(params) {
  return request({ url: '/equipment/page', method: 'get', params })
}

export function getEquipmentDetail(id) {
  return request({ url: `/equipment/detail/${id}`, method: 'get' })
}

export function saveEquipment(data) {
  return request({ url: '/equipment/save', method: 'post', data })
}

export function disableEquipment(id) {
  return request({ url: `/equipment/disable/${id}`, method: 'post' })
}

export function getEquipmentInspectionRecords(params) {
  return request({ url: '/equipment/inspection/records', method: 'get', params })
}
