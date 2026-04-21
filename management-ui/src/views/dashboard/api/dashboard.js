import request from '@/utils/request.js'

export function getDashboardOverview() {
  return request({
    url: '/dashboard/overview',
    method: 'get'
  })
}

export function getDashboardAiAdvices() {
  return request({
    url: '/dashboard/ai-advices',
    method: 'get'
  })
}
