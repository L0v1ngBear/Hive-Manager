import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync(new URL('../src/views/function/order/order.vue', import.meta.url), 'utf8')
const backendVo = readFileSync(
  new URL('../../management/src/main/java/my/hive/domain/order/model/vo/SalesOrderPageVO.java', import.meta.url),
  'utf8'
)
const statusColumnBranch = source.slice(
  source.indexOf(`<template v-else-if="column.key === 'status'">`),
  source.indexOf(`<template v-else-if="column.key === 'progress'">`)
)

test('order form and filters expose all three invoice states', () => {
  assert.match(source, /<el-option label="其他类型" value="2"\s*\/>/)
  assert.match(source, /<el-option label="其他类型" :value="2"\s*\/>/)
  assert.match(source, /invoice_other/)
  assert.match(source, /invoice_warning/)
})

test('invoice labels and colors distinguish overdue from settled states', () => {
  assert.match(source, /if \(normalized === 2\) return '其他类型'/)
  assert.match(statusColumnBranch, /row\.invoiceWarning/)
  assert.match(statusColumnBranch, /未开票 \{\{ row\.invoiceAgeDays \}\} 天/)
  assert.match(source, /order-invoice-settled/)
  assert.match(source, /order-invoice-warning/)
  assert.match(source, /--order-card-color:\s*#64748b/)
})

test('backend list contract keeps invoice warning independent from stale warning', () => {
  for (const field of ['invoiceWarning', 'invoiceAgeDays', 'invoiceWarningDays']) {
    assert.match(backendVo, new RegExp(`private [^;=]+ ${field}(?:\\s*=\\s*[^;]+)?;`))
  }
  assert.match(backendVo, /private Boolean staleWarning = false;/)
})
