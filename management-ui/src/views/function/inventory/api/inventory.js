import request from '@/utils/request.js'

export function getInventorySummary() {
  return request({ url: '/inventory/summary', method: 'get' })
}

export function getInventoryPage(params) {
  return request({ url: '/inventory/page', method: 'get', params })
}

export function getInventoryWarnings() {
  return request({ url: '/inventory/warning/list', method: 'get' })
}

export function getRecentInventoryRecords() {
  return request({ url: '/inventory/record/recent', method: 'get' })
}

export function getInventoryTrend() {
  return request({ url: '/inventory/trend', method: 'get' })
}

export function searchInventoryModels(params) {
  return request({ url: '/inventory/model/search', method: 'get', params })
}

export function searchInventoryBarcode(params) {
  return request({ url: '/inventory/barCode/search', method: 'get', params })
}

export function inCloth(data) {
  return request({ url: '/inventory/cloth/in', method: 'post', data })
}

export function outCloth(data) {
  return request({ url: '/inventory/cloth/out', method: 'post', data })
}

export function downloadInventoryImportTemplate() {
  return request({
    url: '/inventory/import-template',
    method: 'get',
    responseType: 'blob'
  })
}

export function importInventory(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/inventory/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
