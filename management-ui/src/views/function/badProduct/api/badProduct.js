import request from '@/utils/request.js'

export function getBadProductPage(params) {
  return request({
    url: '/bad-product/list',
    method: 'get',
    params: cleanParams(params)
  })
}

export function saveBadProduct(data) {
  return request({
    url: '/bad-product/save',
    method: 'post',
    data
  })
}

export function uploadBadProductAttachment(data) {
  return request({
    url: '/bad-product/attachment/upload',
    method: 'post',
    data,
    timeout: 30000
  })
}

export function downloadBadProductAttachment(params) {
  return request({
    url: '/bad-product/attachment/download',
    method: 'get',
    params,
    responseType: 'blob',
    timeout: 30000
  })
}

export function processBadProduct(data) {
  return request({
    url: '/bad-product/process',
    method: 'post',
    data
  })
}

function cleanParams(params = {}) {
  return Object.entries(params).reduce((target, [key, value]) => {
    if (value === undefined || value === null || value === '') {
      return target
    }
    target[key] = value
    return target
  }, {})
}
