import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const orderPage = readFileSync(
  new URL('../src/views/function/order/order.vue', import.meta.url),
  'utf8',
)

const cssBlock = (selector) => {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return orderPage.match(new RegExp(`${escaped}\\s*\\{([\\s\\S]*?)\\}`))?.[1] || ''
}

test('order list assigns every column a stable share through colgroup', () => {
  assert.match(orderPage, /<colgroup>[\s\S]*?v-for="column in orderTableColumns"[\s\S]*?:style="\{ width: orderColumnWidth\(column\.key\) \}"[\s\S]*?orderActionColumnWidth/)
  assert.match(orderPage, /orderNo:\s*'14%'/)
  assert.match(orderPage, /customer:\s*'25%'/)
  assert.match(orderPage, /informationChannel:\s*'10%'/)
  assert.match(orderPage, /shipments:\s*'15%'/)
  assert.match(orderPage, /status:\s*'10%'/)
  assert.match(orderPage, /progress:\s*'13%'/)
  assert.match(orderPage, /const orderActionColumnWidth = '13%'/)

  const table = cssBlock('.function-page-shell .order-list-table.responsive-data-table')
  assert.match(table, /width:\s*100%/)
  assert.match(table, /min-width:\s*90rem\s*!important/)
  assert.match(table, /table-layout:\s*fixed/)
  assert.doesNotMatch(table, /table-layout:\s*auto/)
  assert.match(orderPage, /@media \(max-width: 640px\)[\s\S]*?\.order-list-table\.responsive-data-table\s*\{[\s\S]*?min-width:\s*0\s*!important[\s\S]*?table-layout:\s*auto/)
  assert.match(orderPage, /@media \(max-width: 640px\)[\s\S]*?\.order-list-table colgroup\s*\{[\s\S]*?display:\s*none/)
})

test('long order cells wrap inside their assigned column instead of widening the table', () => {
  const sharedCells = cssBlock('.function-page-shell .order-list-table.responsive-data-table th,\n.function-page-shell .order-list-table.responsive-data-table td')
  assert.match(sharedCells, /white-space:\s*normal/)
  assert.match(sharedCells, /overflow-wrap:\s*anywhere/)

  const primaryText = cssBlock('.order-column-orderNo .font-bold,\n.order-column-customer .font-bold')
  assert.match(primaryText, /white-space:\s*normal/)
  assert.match(primaryText, /overflow-wrap:\s*anywhere/)
  assert.doesNotMatch(primaryText, /text-overflow:\s*ellipsis/)

  const trackingNumber = cssBlock('.order-express-number-trigger span:last-child')
  assert.match(trackingNumber, /white-space:\s*normal/)
  assert.match(trackingNumber, /overflow-wrap:\s*anywhere/)
})
