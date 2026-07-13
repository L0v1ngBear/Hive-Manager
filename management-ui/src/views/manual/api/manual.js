import request from '@/utils/request'

export function getCustomManual() {
  return request({
    url: '/manual/custom',
    method: 'get',
    showGlobalLoading: false
  })
}

export function saveCustomManualContent(content) {
  return request({
    url: '/manual/custom',
    method: 'post',
    data: { content }
  })
}
