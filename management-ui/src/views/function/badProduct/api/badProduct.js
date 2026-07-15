import request from '@/utils/request.js'

export function getBadProductPage(params) {
  return request({
    url: '/quality/list',
    method: 'get',
    params: cleanParams(params)
  })
}

export function saveBadProduct(data) {
  return request({
    url: '/quality/save',
    method: 'post',
    data
  })
}

export function uploadBadProductAttachment(data) {
  return request({
    url: '/quality/attachment/upload',
    method: 'post',
    data,
    timeout: 30000
  })
}

export function downloadBadProductAttachment(params) {
  return request({
    url: '/quality/attachment/download',
    method: 'get',
    params,
    responseType: 'blob',
    timeout: 30000
  })
}

export function processBadProduct(data) {
  return request({
    url: '/quality/process',
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
