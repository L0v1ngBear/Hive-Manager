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

export function completeChunkedDocumentUpload(uploadId, parentId) {
  return request({
    url: `/document/file/chunked/${uploadId}/complete`,
    method: 'post',
    params: { parentId },
    timeout: 1800000,
    showGlobalLoading: false
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

export function getDocumentFolders() {
  return request({
    url: '/document/folders',
    method: 'get'
  })
}

export function renameDocument(documentId, newName) {
  return request({
    url: '/document/rename',
    method: 'put',
    params: { documentId, newName }
  })
}

export function moveDocument(documentId, newParentId) {
  return request({
    url: '/document/move',
    method: 'put',
    params: { documentId, newParentId }
  })
}

export function deleteDocument(documentId) {
  return request({
    url: `/document/${documentId}`,
    method: 'delete'
  })
}
