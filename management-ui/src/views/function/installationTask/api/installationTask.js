import request from '@/utils/request.js'

export function getInstallationTaskPage(params) {
  return request({
    url: '/installation-task/page',
    method: 'get',
    params
  })
}

export function updateInstallationTaskStatus(data) {
  return request({
    url: '/installation-task/status',
    method: 'post',
    data
  })
}

export function uploadInstallationTaskAttachment(data) {
  return request({
    url: '/installation-task/attachment/upload',
    method: 'post',
    data,
    timeout: 30000
  })
}

export function downloadInstallationTaskAttachment(params) {
  return request({
    url: '/installation-task/attachment/download',
    method: 'get',
    params,
    responseType: 'blob',
    timeout: 30000
  })
}
