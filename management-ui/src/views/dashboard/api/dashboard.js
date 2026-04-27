import request from '@/utils/request.js'

export function getDashboardOverview() {
  return request({
    url: '/dashboard/overview',
    method: 'get'
  })
}

export function getDashboardAiAdvices(params = {}) {
  return request({
    url: '/dashboard/ai-advices',
    method: 'get',
    params
  })
}

export function getDashboardAiSnapshot() {
  return request({
    url: '/dashboard/ai-snapshot',
    method: 'get'
  })
}

export function feedbackDashboardAiAdvice(data) {
  return request({
    url: '/dashboard/ai-advices/feedback',
    method: 'post',
    data
  })
}
