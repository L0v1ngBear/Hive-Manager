import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync(new URL('../src/views/function/order/order.vue', import.meta.url), 'utf8')

test('order form provides permission-gated multi-row notes instead of a business remark textarea', () => {
  assert.match(source, /orderForm\.notes/)
  assert.match(source, /新增备注/)
  assert.match(source, /最后修改/)
  assert.match(source, /保存后记录修改时间/)
  assert.match(source, /order:note:view/)
  assert.match(source, /order:note:create/)
  assert.match(source, /order:note:update/)
  assert.match(source, /function defaultOrderNote\(\)/)
  assert.match(source, /function addOrderNote\(\)/)
  assert.match(source, /function discardUnsavedOrderNote\(index\)/)
  assert.match(source, /v-if="!note\.id"/)
  assert.doesNotMatch(source, /v-model\.trim="orderForm\.remark"/)
  assert.doesNotMatch(source, /remark:\s*blank\(orderForm\.remark\)/)
})

test('order note payload sends only id, trimmed content, and version', () => {
  assert.match(
    source,
    /notes:\s*orderForm\.notes\.map\(\(\{ id, content, version \}\) => \(\{ id, content: content\.trim\(\), version \}\)\)/
  )
})
