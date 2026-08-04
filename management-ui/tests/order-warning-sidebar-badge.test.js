import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')
const sidebar = read('../src/layout/components/Sidebar.vue')
const orderPage = read('../src/views/function/order/order.vue')
const refresh = read('../src/utils/orderWarningRefresh.js')

test('order navigation badge displays the warning total for authorized users', () => {
  assert.match(sidebar, /item\.path === '\/function\/order' && orderWarningCount > 0/)
  assert.match(sidebar, /:value="orderWarningCount"/)
  assert.match(sidebar, /class="order-warning-menu-badge"/)
  assert.match(sidebar, /getOrderWarningSummary\(\)/)
  assert.match(sidebar, /hasPermission\('order:warning:list'\)/)
})

test('order warning badge refreshes after warning changes, navigation, focus and polling', () => {
  assert.match(refresh, /hive-order-warning-changed/)
  assert.match(sidebar, /listenOrderWarningChanged\(refreshOrderWarningCount\)/)
  assert.match(sidebar, /window\.addEventListener\('focus', refreshOrderWarningCount\)/)
  assert.match(sidebar, /window\.setInterval\(refreshOrderWarningCount, 30000\)/)
  assert.match(sidebar, /\(\) => route\.path,[\s\S]*?refreshOrderWarningCount/)
  assert.match(sidebar, /let orderWarningRequestId = 0/)
  assert.match(orderPage, /notifyOrderWarningChanged\(orderWarningSummary\.totalCount\)/)
})
