import request from '@/utils/request.js'

export function login(data) {
  return request({
    url: '/auth/admin/login',
    method: 'post',
    data
  })
}

export function sendPasswordResetCode(data) {
  return request({
    url: '/auth/admin/password-reset/code',
    method: 'post',
    data
  })
}

export function resetPassword(data) {
  return request({
    url: '/auth/admin/password-reset',
    method: 'post',
    data
  })
}

export function sendOrganizationJoinCode(data) {
  return request({
    url: '/auth/admin/join-organization/code',
    method: 'post',
    data
  })
}

export function joinOrganization(data) {
  return request({
    url: '/auth/admin/join-organization',
    method: 'post',
    data
  })
}

export function changeInitialPassword(data) {
  return request({
    url: '/auth/admin/initial-password',
    method: 'post',
    data,
    silent: true
  })
}

export function changePassword(data) {
  return request({
    url: '/auth/admin/password',
    method: 'post',
    data,
    silent: true
  })
}

export function createScanLoginSession() {
  return request({
    url: '/auth/admin/scan-login/session',
    method: 'post'
  })
}

export function getScanLoginStatus(params) {
  return request({
    url: '/auth/admin/scan-login/status',
    method: 'get',
    params
  })
}

export function getWebWechatLoginConfig() {
  return request({
    url: '/auth/admin/wechat-login/config',
    method: 'get',
    showGlobalLoading: false
  })
}

export function createWebWechatLoginSession() {
  return request({
    url: '/auth/admin/wechat-login/session',
    method: 'post',
    showGlobalLoading: false
  })
}

export function completeWebWechatLogin(data) {
  return request({
    url: '/auth/admin/wechat-login/complete',
    method: 'post',
    data,
    showGlobalLoading: false
  })
}

export function bindWebWechatLogin(data) {
  return request({
    url: '/auth/admin/wechat-login/bind',
    method: 'post',
    data,
    showGlobalLoading: false
  })
}

export function selectWebWechatTenant(data) {
  return request({
    url: '/auth/admin/wechat-login/select',
    method: 'post',
    data,
    showGlobalLoading: false
  })
}
