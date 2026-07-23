import request from '@/utils/request.js'
import { uploadAttachmentWithVideoChunks } from '@/utils/chunkedVideoUpload.js'

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
  const file = data?.get?.('file')
  return uploadAttachmentWithVideoChunks(file, () => request({
    url: '/quality/attachment/upload',
    method: 'post',
    data,
    timeout: 600000
  }), 'bad-product')
}

export function downloadBadProductAttachment(params) {
  return request({
    url: '/quality/attachment/download',
    method: 'get',
    params,
    responseType: 'blob',
    timeout: 600000
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
