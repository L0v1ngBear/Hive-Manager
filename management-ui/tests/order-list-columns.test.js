import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const orderPage = readFileSync(
  new URL('../src/views/function/order/order.vue', import.meta.url),
  'utf8',
)

const defaultColumnsSource = orderPage.match(
  /const defaultOrderTableColumns = \[([\s\S]*?)\]\s*const \{/,
)?.[1] || ''

test('order list omits order information and time columns', () => {
  assert.doesNotMatch(defaultColumnsSource, /key:\s*'core'/)
  assert.doesNotMatch(defaultColumnsSource, /label:\s*'订单信息'/)
  assert.doesNotMatch(defaultColumnsSource, /key:\s*'time'/)
  assert.doesNotMatch(defaultColumnsSource, /label:\s*'时间'/)
  assert.doesNotMatch(orderPage, /column\.key === 'core'/)
  assert.doesNotMatch(orderPage, /column\.key === 'time'/)
})

test('order export follows the remaining visible columns', () => {
  assert.doesNotMatch(orderPage, /key === 'core'/)
  assert.doesNotMatch(orderPage, /key === 'time'/)
  assert.match(orderPage, /headers:\s*orderTableColumns\.value\.map/)
  assert.match(orderPage, /formatOrderExportCell\(row,\s*column\.key\)/)
})
