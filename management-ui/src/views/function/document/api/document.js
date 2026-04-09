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

export function getBreadcrumbs(documentId) {
  return request({
    url: '/document/breadcrumbs',
    method: 'get',
    params: { documentId }
  })
}