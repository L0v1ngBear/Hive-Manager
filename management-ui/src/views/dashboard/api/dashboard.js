import request from '@/utils/request.js'

export function getDashboardOverview() {
  return request({
    url: '/dashboard/overview',
    method: 'get',
    cacheTtl: 5000
  })
}

export function getDashboardAiAdvices(params = {}) {
  return request({
    url: '/dashboard/ai-advices',
    method: 'get',
    params,
    cacheTtl: 5000
  })
}

export function getDashboardAiBrief(params = {}) {
  return request({
    url: '/dashboard/ai-brief',
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

export function getDashboardAiEvolution() {
  return request({
    url: '/dashboard/ai-evolution',
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
