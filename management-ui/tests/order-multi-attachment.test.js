import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
const order = read('src/views/function/order/order.vue')
const uploader = read('src/components/DragAttachmentUpload.vue')
const request = read('../management/src/main/java/my/hive/domain/order/model/dto/SalesOrderSaveRequest.java')
const migration = read('../db-migrations/migrations/V20260728_002_order_multi_attachments.sql')

test('shared uploader supports opt-in multi-file selection without changing single-file callers', () => {
  assert.match(uploader, /:multiple="multiple"/)
  assert.match(uploader, /default:\s*false/)
  assert.match(uploader, /defineEmits\(\['select', 'select-files', 'download', 'remove'\]\)/)
  assert.match(uploader, /if \(props\.multiple\)[\s\S]*emit\('select-files', supported\)[\s\S]*else[\s\S]*emit\('select', supported\[0\]\)/)
})

test('order editor appends views and removes up to twenty attachments', () => {
  assert.match(order, /attachments:\s*\[\]/)
  assert.match(order, /:multiple="true"/)
  assert.match(order, /@select-files="handleOrderAttachmentFiles"/)
  assert.match(order, /orderForm\.attachments\.push\(attachment\)/)
  assert.match(order, /orderForm\.attachments\.splice\(index,\s*1\)/)
  assert.match(order, /detailOrderAttachments/)
  assert.match(order, /v-for="\(attachment, index\) in orderForm\.attachments"/)
  assert.match(order, /每个订单最多添加20个附件/)
})

test('order payload sends the collection and mirrors its first item for old deployments', () => {
  assert.match(order, /const attachments = orderForm\.attachments\.map/)
  assert.match(order, /const firstAttachment = attachments\[0\] \|\| null/)
  assert.match(order, /attachments,\s*\n\s*attachmentName: firstAttachment\?\.fileName \|\| null/)
  assert.match(order, /attachmentUrl: firstAttachment\?\.fileUrl \|\| null/)
  assert.match(request, /@Size\(max = 20, message = "每个订单最多添加20个附件"\)/)
})

test('migration backfills existing scalar attachments into the JSON collection', () => {
  assert.match(migration, /ADD COLUMN attachments_json JSON/)
  assert.match(migration, /JSON_ARRAY\(/)
  assert.match(migration, /JSON_OBJECT\(/)
  assert.match(migration, /WHERE attachments_json IS NULL/)
  assert.match(migration, /attachment_url IS NOT NULL/)
})
