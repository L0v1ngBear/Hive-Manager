import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync(new URL('../src/views/function/order/order.vue', import.meta.url), 'utf8')
const permissionSource = readFileSync(new URL('../src/views/function/order/orderPermissions.js', import.meta.url), 'utf8')

test('migrates order standard controls while retaining protected business surfaces', () => {
  for (const tag of ['el-input','el-select','el-input-number','el-pagination','el-drawer','el-dialog','el-form','el-tag','el-progress','el-empty']) assert.match(source, new RegExp(`<${tag}\\b`))
  assert.match(source, /v-loading/)
  for (const invariant of ['orderStatuses','TableColumnSettings','DragAttachmentUpload','BusinessTimeCorrectionPanel']) assert.match(source, new RegExp(invariant))
  for (const permissionInvariant of ['ORDER_STATUS_PERMISSION_PREFIX', 'order:status:', 'canViewOrderDetail']) {
    assert.match(permissionSource, new RegExp(permissionInvariant))
  }
  assert.match(source, /class="order-list-table/)
  assert.match(source, /:data-label="column\.label"/)
})

test('replaces unsafe warning HTML and keeps explicit imports and export data', () => {
  assert.doesNotMatch(source, /dangerouslyUseHTMLString/)
  assert.match(source, /ElDialog/)
  assert.match(source, /from ['"]element-plus['"]/)
  assert.match(source, /exportRowsToExcel/)
  assert.match(source, /@click\.stop/)
})

test('keeps warning settings visible but blocks unauthorized reads and writes', () => {
  assert.match(source, /const canManageWarningSetting = computed/)
  assert.match(source, /:disabled="!canManageWarningSetting"/)
  assert.match(source, /if \(!canManageWarningSetting\.value\) return/)
  assert.match(source, /canManageWarningSetting\.value\s*\?\s*getOrderWarningSetting\(\)\s*:\s*Promise\.resolve\(null\)/)
})

test('owns edit detail requests and exposes retryable mutually exclusive states', () => {
  assert.match(source, /let editRequestId = 0/)
  assert.match(source, /const editLoading = ref\(false\)/)
  assert.match(source, /const editErrorMessage = ref\(''\)/)
  assert.match(source, /const requestId = \+\+editRequestId/)
  assert.match(source, /if \(requestId !== editRequestId\) return/)
  assert.match(source, /editRequestId \+= 1/)
  assert.match(source, /v-loading="editLoading"/)
  assert.match(source, /<template v-else-if="!editLoading">/)
  assert.match(source, /editErrorMessage/)
  assert.match(source, /submitting \|\| editLoading \|\| !canSubmitCurrentForm/)
})

test('guards edit submission against concurrent and non-ready calls inside the handler', () => {
  assert.match(source, /async function submitForm\(\) \{\s*if \(submitting\.value \|\| editLoading\.value \|\| editErrorMessage\.value\) return/)
})

test('keeps detail and export commands visible but permission disabled and guarded', () => {
  assert.match(source, /function canViewOrderDetail\(row = \{\}\) \{\s*return hasOrderDetailPermission\(userStore\.permissions, row\)/)
  assert.match(permissionSource, /'order:detail'/)
  assert.match(source, /const canExportTable = computed\(\(\) => userStore\.hasPermission\('order:list'\)\)/)
  assert.match(source, /:export-disabled="!canExportTable"/)
  assert.match(source, /export-disabled-reason="当前账号暂无表格导出权限"/)
  assert.match(source, /if \(!canExportTable\.value\) return/)
  assert.match(source, /if \(!canViewOrderDetail\(row\)\) \{/)
  assert.match(source, /:disabled="!canViewOrderDetail\(row\)"/)
  assert.match(source, /当前账号暂无订单详情查看权限/)
})

test('uses ElButton for ordinary order commands while retaining protected native buttons', () => {
  for (const command of ['查询', '重置', '添加商品', '删除', '取消', '保存修改']) {
    assert.match(source, new RegExp(`<el-button[^>]*[\\s\\S]{0,180}${command}`))
  }
  assert.match(source, /<button[\s\S]*class="stat-card/)
  assert.match(source, /<button v-for="option in customerOptions"/)
})
