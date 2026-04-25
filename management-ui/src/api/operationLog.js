import request from '@/utils/request'

export function fetchOperationLogPage(params) {
  return request({
    url: '/platform/operation-log/page',
    method: 'get',
    params
  })
}
