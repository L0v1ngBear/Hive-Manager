import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync(new URL('../src/views/function/order/order.vue', import.meta.url), 'utf8')

test('migrates order standard controls while retaining protected business surfaces', () => {
  for (const tag of ['el-input','el-select','el-date-picker','el-input-number','el-pagination','el-drawer','el-dialog','el-form','el-tag','el-progress','el-empty']) assert.match(source, new RegExp(`<${tag}\\b`))
  assert.match(source, /v-loading/)
  for (const invariant of ['orderStatuses','ORDER_STATUS_PERMISSION_PREFIX','order:status:','TableColumnSettings','DragAttachmentUpload','BusinessTimeCorrectionPanel']) assert.match(source, new RegExp(invariant))
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
