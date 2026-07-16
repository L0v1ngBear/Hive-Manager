import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const orderSource = readFileSync(new URL('../src/views/function/order/order.vue', import.meta.url), 'utf8')
const apiSource = readFileSync(new URL('../src/views/function/order/api/order.js', import.meta.url), 'utf8')

function functionSource(source, name, nextName) {
  const start = source.search(new RegExp(`(?:async )?function ${name}\\(`))
  const end = source.slice(start + 1).search(new RegExp(`(?:async )?function ${nextName}\\(`))
  assert.notEqual(start, -1, `${name} must exist`)
  assert.notEqual(end, -1, `${nextName} must follow ${name}`)
  return source.slice(start, start + 1 + end)
}

test('order logistics query is triggered only when the express number popover opens', () => {
  const loadOrdersSource = functionSource(orderSource, 'loadOrders', 'logisticsTrackingKey')

  assert.match(orderSource, /<el-popover[\s\S]*trigger="hover"[\s\S]*@show="loadLogisticsTracking\(row\)"/)
  assert.match(orderSource, /v-if="row\.expressNo"/)
  assert.match(orderSource, /const logisticsTrackingStates = reactive\(\{\}\)/)
  assert.match(orderSource, /row\.orderId[\s\S]*row\.expressCompany[\s\S]*row\.expressNo/)
  assert.match(orderSource, /function loadLogisticsTracking\(row\)/)
  assert.doesNotMatch(loadOrdersSource, /getOrderLogisticsTracking/)
  assert.doesNotMatch(orderSource, /@(mouseenter|mouseover)="loadLogisticsTracking/)
})

test('management UI calls only the canonical order tracking endpoint', () => {
  assert.match(apiSource, /export function getOrderLogisticsTracking\(orderId\)/)
  assert.match(apiSource, /`\/orders\/\$\{encodeURIComponent\(orderId\)\}\/logistics-tracking`/)
  assert.doesNotMatch(apiSource, /kuaidi100\.com|poll\/query\.do|legacy|fallback/i)
})

test('tracking popover renders loading, error, cache and trace states without exposing credentials', () => {
  assert.match(orderSource, /物流轨迹加载中/)
  assert.match(orderSource, /logisticsTrackingState\(row\)\.errorMessage/)
  assert.match(orderSource, /logisticsTrackingState\(row\)\.data\.latestContext/)
  assert.match(orderSource, /logisticsTrackingState\(row\)\.data\.traces/)
  assert.match(orderSource, /logisticsTrackingState\(row\)\.data\.cached/)
  assert.doesNotMatch(orderSource, /KUAIDI100_(?:KEY|CUSTOMER)|secret-key|customer-code/)
})
