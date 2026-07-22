import request from '@/utils/request.js'

export function getDocumentList(parentId) {
  return request({
    url: `/document/list/${parentId}`,
    method: 'get'
  })
}

export function createFolder(data) {
  return request({
    url: '/document/folder/create',
    method: 'post',
    data
  })
}

export function uploadDocumentFile(data) {
  return request({
    url: '/document/file/upload',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 600000
  })
}

export function downloadDocumentFile(documentId) {
  return request({
    url: '/document/file/download',
    method: 'get',
    params: { documentId },
    responseType: 'blob',
    timeout: 600000
  })
}

export function getBreadcrumbs(documentId) {
  return request({
    url: '/document/breadcrumbs',
    method: 'get',
    params: { documentId }
  })
}
