import request from '@/utils/request'

export function listLeaveApprovals(params) {
  return request({
    url: '/approval/leave/list',
    method: 'get',
    params,
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

export function listFinanceApprovals(params) {
  return request({
    url: '/approval/finance/list',
    method: 'get',
    params,
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