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
