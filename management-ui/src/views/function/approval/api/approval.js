import request from '@/utils/request'

export function getApprovalSummary() {
  return request({
    url: '/approval/summary',
    method: 'get',
  })
}

export function listApprovalAuditors(params) {
  return request({
    url: '/approval/auditors',
    method: 'get',
    params,
  })
}

export function listApprovalDefaultAuditors() {
  return request({
    url: '/approval/default-auditors',
    method: 'get',
  })
}

export function saveApprovalDefaultAuditor(data) {
  return request({
    url: '/approval/default-auditors',
    method: 'post',
    data,
  })
}

export function listLeaveApprovals() {
  return request({
    url: '/approval/leave/list',
    method: 'get',
  })
}

export function getLeaveApprovalDetail(leaveCode) {
  return request({
    url: `/approval/leave/${leaveCode}`,
    method: 'get',
  })
}

export function auditLeaveApproval(data) {
  return request({
    url: '/approval/leave/audit',
    method: 'post',
    data,
  })
}

export function listFinanceApprovals() {
  return request({
    url: '/approval/finance/list',
    method: 'get',
  })
}

export function getFinanceApprovalDetail(approvalCode) {
  return request({
    url: `/approval/finance/${approvalCode}`,
    method: 'get',
  })
}

export function auditFinanceApproval(data) {
  return request({
    url: '/approval/finance/audit',
    method: 'post',
    data,
  })
}

export function submitFinanceApproval(data) {
  return request({
    url: '/approval/finance/submit',
    method: 'post',
    data,
  })
}

export function uploadFinanceApprovalAttachment(data) {
  return request({
    url: '/approval/finance/attachment/upload',
    method: 'post',
    data,
    timeout: 30000,
  })
}

export function downloadFinanceApprovalAttachment(params) {
  return request({
    url: '/approval/finance/attachment/download',
    method: 'get',
    params,
    responseType: 'blob',
    timeout: 30000,
  })
}

export function listResignationApprovals() {
  return request({
    url: '/approval/resignation/list',
    method: 'get',
  })
}

export function getResignationApprovalDetail(resignationCode) {
  return request({
    url: `/approval/resignation/${resignationCode}`,
    method: 'get',
  })
}

export function submitResignationApproval(data) {
  return request({
    url: '/approval/resignation/submit',
    method: 'post',
    data,
  })
}

export function auditResignationApproval(data) {
  return request({
    url: '/approval/resignation/audit',
    method: 'post',
    data,
  })
}

export function listOrderApprovals() {
  return request({
    url: '/approval/order/list',
    method: 'get',
  })
}

export function getOrderApprovalDetail(orderType, orderId) {
  return request({
    url: `/approval/order/${orderType}/${orderId}`,
    method: 'get',
  })
}

export function auditOrderApproval(data) {
  return request({
    url: '/approval/order/audit',
    method: 'post',
    data,
  })
}
