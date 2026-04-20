import request from '@/utils/request.js'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function createScanLoginSession() {
  return request({
    url: '/auth/scan-login/session',
    method: 'post'
  })
}

export function getScanLoginStatus(params) {
  return request({
    url: '/auth/scan-login/status',
    method: 'get',
    params
  })
}
