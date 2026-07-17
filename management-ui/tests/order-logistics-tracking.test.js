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

test('each shipment logistics query is triggered only when its popover opens', () => {
  const loadOrdersSource = functionSource(orderSource, 'loadOrders', 'logisticsTrackingKey')
  const trackingKeySource = functionSource(orderSource, 'logisticsTrackingKey', 'logisticsTrackingState')

  assert.match(orderSource, /v-for="shipment in row\.shipments"[\s\S]*<el-popover[\s\S]*trigger="hover"/)
  assert.match(orderSource, /:key="logisticsTrackingKey\(row, shipment\)"/)
  assert.match(orderSource, /@show="loadLogisticsTracking\(row, shipment\)"/)
  assert.match(orderSource, /const logisticsTrackingStates = reactive\(\{\}\)/)
  assert.match(trackingKeySource, /row\.orderId/)
  assert.match(trackingKeySource, /shipment\.id/)
  assert.match(trackingKeySource, /shipment\.logisticsCompany/)
  assert.match(trackingKeySource, /shipment\.trackingNo/)
  assert.match(trackingKeySource, /shipment\.version/)
  const trackingKey = Function(`return (${trackingKeySource.trim()})`)()
  const row = { orderId: 'SO-001' }
  const shipment = {
    id: 7,
    logisticsCompany: 'shunfeng',
    trackingNo: 'SF123456',
    version: 2
  }
  const originalKey = trackingKey(row, shipment)
  assert.notEqual(trackingKey({ orderId: 'SO-002' }, shipment), originalKey)
  assert.notEqual(trackingKey(row, { ...shipment, logisticsCompany: 'zhongtong' }), originalKey)
  assert.notEqual(trackingKey(row, { ...shipment, trackingNo: 'ZT123456' }), originalKey)
  assert.notEqual(trackingKey(row, { ...shipment, version: 3 }), originalKey)
  assert.notEqual(
    trackingKey(row, { logisticsCompany: 'shunfeng', trackingNo: 'NEW-002', version: 0 }),
    trackingKey(row, { logisticsCompany: 'shunfeng', trackingNo: 'NEW-001', version: 0 })
  )
  assert.match(orderSource, /function logisticsTrackingState\(row = \{\}, shipment = \{\}\)/)
  assert.match(orderSource, /function loadLogisticsTracking\(row, shipment\)/)
  assert.match(orderSource, /getOrderLogisticsTracking\(row\.orderId, shipment\.id\)/)
  assert.doesNotMatch(loadOrdersSource, /getOrderLogisticsTracking/)
  assert.doesNotMatch(orderSource, /@(mouseenter|mouseover)="loadLogisticsTracking/)
})

test('management UI calls only the canonical order tracking endpoint', () => {
  assert.match(apiSource, /export function getOrderLogisticsTracking\(orderId, shipmentId\)/)
  assert.match(apiSource, /`\/orders\/\$\{encodeURIComponent\(orderId\)\}\/shipments\/\$\{encodeURIComponent\(shipmentId\)\}\/logistics-tracking`/)
  assert.doesNotMatch(apiSource, /`\/orders\/\$\{encodeURIComponent\(orderId\)\}\/logistics-tracking`/)
  assert.doesNotMatch(apiSource, /kuaidi100\.com|poll\/query\.do|legacy|fallback/i)
})

test('tracking popover renders loading, error, cache and trace states without exposing credentials', () => {
  assert.match(orderSource, /物流轨迹加载中/)
  assert.match(orderSource, /logisticsTrackingState\(row, shipment\)\.errorMessage/)
  assert.match(orderSource, /logisticsTrackingState\(row, shipment\)\.data\.latestContext/)
  assert.match(orderSource, /logisticsTrackingState\(row, shipment\)\.data\.traces/)
  assert.match(orderSource, /logisticsTrackingState\(row, shipment\)\.data\.cached/)
  assert.doesNotMatch(orderSource, /KUAIDI100_(?:KEY|CUSTOMER)|secret-key|customer-code/)
})

test('list-only users render disabled tracking numbers and never call the tracking API', async () => {
  const loadSource = functionSource(orderSource, 'loadLogisticsTracking', 'resolveOrderListFailure').trim()
  assert.match(orderSource, /v-for="shipment in row\.shipments"[\s\S]*<el-popover\s+v-if="canViewOrderDetail\(row\)"/)
  assert.match(orderSource, /v-else[\s\S]*order-express-number-trigger is-disabled[\s\S]*aria-disabled="true"/)

  let apiCalls = 0
  const loadLogisticsTracking = Function(
    'canViewOrderDetail',
    'logisticsTrackingState',
    'logisticsTrackingCacheValid',
    'getOrderLogisticsTracking',
    `return (${loadSource})`
  )(
    () => false,
    () => ({ loading: false, data: null, errorMessage: '' }),
    () => false,
    async () => { apiCalls += 1 }
  )

  await loadLogisticsTracking(
    { orderId: 'SO-001', status: 'pending_ship' },
    { id: 7, trackingNo: 'SF123456' }
  )
  assert.equal(apiCalls, 0)
})
