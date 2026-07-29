import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const readRepo = (relativePath) => readFileSync(path.join(repoRoot, relativePath), 'utf8')
const approvalPage = readRepo('management-ui/src/views/function/approval/approvalCenter.vue')
const approvalVO = readRepo('management/src/main/java/my/hive/domain/approval/model/vo/OrderApprovalVO.java')
const approvalService = readRepo('management/src/main/java/my/hive/domain/approval/service/ApprovalService.java')

const sourceBetween = (source, start, end) => {
  const startIndex = source.indexOf(start)
  const endIndex = source.indexOf(end, startIndex + start.length)
  return startIndex >= 0 && endIndex > startIndex ? source.slice(startIndex, endIndex) : ''
}

test('sales order approval responses carry production location', () => {
  assert.match(approvalVO, /private String productionLocation;/)
  assert.match(approvalService, /vo\.setProductionLocation\(order\.getProductionLocation\(\)\)/)
  assert.match(approvalPage, /productionLocation: item\.productionLocation \|\| ''/)
  assert.match(approvalPage, /productionLocation: detail\.productionLocation \|\| ''/)
})

test('only approving a pending sales order asks to confirm its location', () => {
  const handler = sourceBetween(
    approvalPage,
    'const confirmSalesOrderProductionLocation = async',
    'const quickAudit = async',
  )

  assert.match(handler, /Number\(action\) !== 1/)
  assert.match(handler, /item\?\.type !== 'order'/)
  assert.match(handler, /orderType !== 'sales'/)
  assert.match(handler, /orderStatus !== 'pending_confirm'/)
  assert.match(handler, /`本订单生产地点为“\$\{productionLocation\}”，请确认选择无误。`/)
  assert.match(handler, /confirmButtonText: '确认订单'/)
  assert.match(handler, /cancelButtonText: '返回检查'/)
  assert.match(handler, /尚未设置生产地点[\s\S]*?return false/)
})

test('quick and detail approval stop before their APIs when location confirmation is cancelled', () => {
  const quickAudit = sourceBetween(approvalPage, 'const quickAudit = async', 'const submitAudit = async')
  const detailAudit = sourceBetween(approvalPage, 'const submitAudit = async', 'const orderAuditActionText')

  for (const handler of [quickAudit, detailAudit]) {
    const confirmIndex = handler.indexOf('await confirmSalesOrderProductionLocation(')
    const apiIndex = handler.indexOf('await auditOrderApproval(')
    assert.ok(confirmIndex >= 0, 'production location confirmation should be called')
    assert.ok(apiIndex > confirmIndex, 'order approval API should run only after confirmation')
    assert.match(handler, /!\(await confirmSalesOrderProductionLocation\([\s\S]*?return/)
  }
})
