import request from '@/utils/request.js'

export function getPricePage(params) {
  return request({ url: '/price/page', method: 'get', params })
}

export function getPriceStats() {
  return request({ url: '/price/stats', method: 'get' })
}

export function publishPrice(data) {
  return request({ url: '/price/publish', method: 'post', data })
}

export function getPriceDetail(id) {
  return request({ url: `/price/detail/${id}`, method: 'get' })
}

export function deletePrice(id) {
  return request({ url: `/price/${id}`, method: 'delete' })
}

export function getPriceCustomers(params) {
  return request({ url: '/price/customers', method: 'get', params })
}

export function getPriceModels(params) {
  return request({ url: '/price/models', method: 'get', params })
}

export function exportPriceExcel(params) {
  return request({
    url: '/price/export-excel',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function downloadPriceImportTemplate() {
  return request({
    url: '/price/import-template',
    method: 'get',
    responseType: 'blob'
  })
}

export function importPrices(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/price/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
