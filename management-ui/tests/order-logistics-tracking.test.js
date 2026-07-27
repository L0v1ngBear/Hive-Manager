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
  assert.match(orderSource, /getOrderLogisticsTracking\(row\.orderId, shipment\.id, shipment\.version\)/)
  assert.doesNotMatch(loadOrdersSource, /getOrderLogisticsTracking/)
  assert.doesNotMatch(orderSource, /@(mouseenter|mouseover)="loadLogisticsTracking/)
})

test('management UI calls only the canonical order tracking endpoint', () => {
  assert.match(apiSource, /export function getOrderLogisticsTracking\(orderId, shipmentId, shipmentVersion\)/)
  assert.match(apiSource, /`\/orders\/\$\{encodeURIComponent\(orderId\)\}\/shipments\/\$\{encodeURIComponent\(shipmentId\)\}\/logistics-tracking`/)
  assert.match(apiSource, /params:\s*\{\s*shipmentVersion\s*\}/)
  assert.match(apiSource, /cacheTtl:\s*30\s*\*\s*60\s*\*\s*1000/)
  assert.doesNotMatch(apiSource, /`\/orders\/\$\{encodeURIComponent\(orderId\)\}\/logistics-tracking`/)
  assert.doesNotMatch(apiSource, /kuaidi100\.com|poll\/query\.do|legacy|fallback/i)
})

test('successful and failed hover queries are both throttled locally', async () => {
  const loadSource = functionSource(orderSource, 'loadLogisticsTracking', 'resolveOrderListFailure').trim()
  const createLoader = (tracking, query) => Function(
    'canViewOrderDetail',
    'logisticsTrackingState',
    'logisticsTrackingCacheValid',
    'getOrderLogisticsTracking',
    'LOGISTICS_TRACKING_FAILURE_RETRY_MS',
    `return (${loadSource})`
  )(
    () => true,
    () => tracking,
    (data) => Date.parse(data?.cacheExpiresAt || '') > Date.now(),
    query,
    30_000
  )
  const row = { orderId: 'SO-001', status: 'shipped' }
  const shipment = { id: 7, trackingNo: 'SF123456' }

  let successCalls = 0
  const successState = { loading: false, data: null, errorMessage: '', retryAfter: 0 }
  const successLoader = createLoader(successState, async () => {
    successCalls += 1
    return { cacheExpiresAt: new Date(Date.now() + 30 * 60 * 1000).toISOString() }
  })
  await successLoader(row, shipment)
  await successLoader(row, shipment)
  assert.equal(successCalls, 1)

  let failureCalls = 0
  const failureState = { loading: false, data: null, errorMessage: '', retryAfter: 0 }
  const failureLoader = createLoader(failureState, async () => {
    failureCalls += 1
    throw new Error('provider unavailable')
  })
  await failureLoader(row, shipment)
  await failureLoader(row, shipment)
  assert.equal(failureCalls, 1)
  assert.ok(failureState.retryAfter > Date.now())
})

test('tracking popover renders loading, error and reference-aligned trace states without exposing credentials', () => {
  assert.match(orderSource, /物流轨迹加载中/)
  assert.match(orderSource, /logisticsTrackingState\(row, shipment\)\.errorMessage/)
  assert.match(orderSource, /logisticsTrackingState\(row, shipment\)\.data\.latestContext/)
  assert.match(orderSource, /logisticsTrackingState\(row, shipment\)\.data\.traces/)
  assert.match(orderSource, /class="order-logistics-timeline-title"/)
  assert.match(orderSource, /物流跟踪/)
  assert.match(orderSource, /function logisticsTrackingRoute\(data = \{\}\)/)
  assert.match(orderSource, /function copyTrackingNumber\(shipment = \{\}\)/)
  assert.match(orderSource, /aria-label="复制运单号"/)
  assert.doesNotMatch(orderSource, /缓存结果，30分钟内不重复查询|刚刚查询，结果已缓存30分钟/)
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
