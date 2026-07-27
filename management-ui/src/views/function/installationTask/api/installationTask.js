import request from '@/utils/request.js'
import { uploadAttachmentWithChunks } from '@/utils/chunkedAttachmentUpload.js'

export function getInstallationTaskPage(params) {
  return request({
    url: '/installation-tasks/page',
    method: 'get',
    params
  })
}

export function updateInstallationTaskStatus(data) {
  return request({
    url: '/installation-tasks/status',
    method: 'post',
    data
  })
}

export function uploadInstallationTaskAttachment(data) {
  const file = data?.get?.('file')
  return uploadAttachmentWithChunks(file, () => request({
    url: '/installation-tasks/attachment/upload',
    method: 'post',
    data,
    timeout: 600000
  }), 'installation-task')
}

export function downloadInstallationTaskAttachment(params) {
  return request({
    url: '/installation-tasks/attachment/download',
    method: 'get',
    params,
    responseType: 'blob',
    timeout: 600000
  })
}
