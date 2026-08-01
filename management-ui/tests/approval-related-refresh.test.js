import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const readRepo = (path) => readFileSync(new URL(`../../${path}`, import.meta.url), 'utf8')
const page = readRepo('management-ui/src/views/function/approval/approvalCenter.vue')
const api = readRepo('management-ui/src/views/function/approval/api/approval.js')
const sidebar = readRepo('management-ui/src/layout/components/Sidebar.vue')
const refresh = readRepo('management-ui/src/utils/approvalRefresh.js')
const service = readRepo('management/src/main/java/my/hive/domain/approval/service/ApprovalService.java')
const candidateService = readRepo('management/src/main/java/my/hive/domain/approval/service/ApprovalAuditorCandidateService.java')
const summary = readRepo('management/src/main/java/my/hive/domain/approval/model/vo/ApprovalSummaryVO.java')

test('approval center requests applicant-or-auditor related records', () => {
  assert.match(page, /relatedApprovalParams = Object\.freeze\(\{ scope: 'related', limit: 500 \}\)/)
  for (const apiName of ['listLeaveApprovals', 'listFinanceApprovals', 'listResignationApprovals']) {
    assert.match(page, new RegExp(`${apiName}\\(relatedApprovalParams\\)`))
    assert.match(api, new RegExp(`export function ${apiName}\\(params\\)`))
  }
  assert.match(service, /case "mine", "related", "pending"/)
  assert.match(service, /appendLeaveRelatedFilter\(wrapper, userId\)/)
  assert.match(service, /appendFinanceRelatedFilter\(wrapper, userId\)/)
  assert.match(service, /appendResignationRelatedFilter\(wrapper, userId\)/)
  assert.match(service, /getApplyUserId, userId\)[\s\S]*?getAuditorId, userId\)[\s\S]*?FIND_IN_SET/)
  assert.match(candidateService, /findRelatedApprovalCodes\(/)
  assert.match(service, /findRelatedApprovalCodes\([\s\S]*?APPROVAL_TYPE_LEAVE/)
  assert.match(service, /findRelatedApprovalCodes\([\s\S]*?APPROVAL_TYPE_FINANCE/)
  assert.match(service, /findRelatedApprovalCodes\([\s\S]*?APPROVAL_TYPE_RESIGNATION/)
})

test('approval dashboard uses server-wide totals and mutations refresh all counters', () => {
  assert.match(summary, /private long mineTotal;/)
  assert.match(summary, /private long approvedTotal;/)
  assert.match(page, /mine: Number\(approvalSummary\.value\.mineTotal \|\| 0\)/)
  assert.match(page, /approved: Number\(approvalSummary\.value\.approvedTotal \|\| 0\)/)
  assert.match(page, /let summaryRequestId = 0/)
  assert.match(page, /window\.setInterval\(refreshSummaryWhenVisible, 30000\)/)
  assert.ok((page.match(/notifyApprovalChanged\(\)[\s\S]{0,40}await refreshAll\(\)/g) || []).length >= 4)
})

test('sidebar refreshes pending badges on approval changes, focus, navigation and fallback polling', () => {
  assert.match(refresh, /hive-approval-changed/)
  assert.match(sidebar, /listenApprovalChanged\(refreshApprovalPendingCount\)/)
  assert.match(sidebar, /window\.addEventListener\('focus', refreshApprovalPendingCount\)/)
  assert.match(sidebar, /window\.setInterval\(refreshApprovalPendingCount, 30000\)/)
  assert.match(sidebar, /\(\) => route\.path,[\s\S]*?refreshApprovalPendingCount/)
  assert.match(sidebar, /let approvalPendingRequestId = 0/)
})
