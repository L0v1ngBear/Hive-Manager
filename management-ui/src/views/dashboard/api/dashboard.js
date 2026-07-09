import request from '@/utils/request.js'

export function getDashboardOverview() {
  return request({
    url: '/dashboard/overview',
    method: 'get',
    cacheTtl: 5000
  })
}
