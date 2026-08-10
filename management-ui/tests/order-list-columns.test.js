import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const orderPage = readFileSync(
  new URL('../src/views/function/order/order.vue', import.meta.url),
  'utf8',
)

const defaultColumnsSource = orderPage.match(
  /const defaultOrderTableColumns = \[([\s\S]*?)\]\s*const orderColumnWidths/,
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

test('order list and export show project before customer', () => {
  assert.match(defaultColumnsSource, /key:\s*'customer',\s*label:\s*'项目 \/ 客户'/)

  const customerCellSource = orderPage.match(
    /<template v-else-if="column\.key === 'customer'">([\s\S]*?)<\/template>/,
  )?.[1] || ''
  assert.ok(customerCellSource.indexOf('row.projectName') < customerCellSource.indexOf('row.customerName'))
  assert.match(
    orderPage,
    /if \(key === 'customer'\) return \[row\.projectName, row\.customerName\]/,
  )
})
