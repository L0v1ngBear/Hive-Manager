import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import { buildStructuredExportData } from '../src/utils/structuredTableExport.js'

const orderSource = readFileSync(new URL('../src/views/function/order/order.vue', import.meta.url), 'utf8')

function functionSource(source, name, nextName) {
  const start = source.search(new RegExp(`(?:async )?function ${name}\\(`))
  const end = source.slice(start + 1).search(new RegExp(`(?:async )?function ${nextName}\\(`))
  assert.notEqual(start, -1, `${name} must exist`)
  assert.notEqual(end, -1, `${nextName} must follow ${name}`)
  return source.slice(start, start + 1 + end)
}

test('order editor saves multiple non-deletable shipment rows', () => {
  const payloadSource = functionSource(orderSource, 'buildOrderPayload', 'orderLogTitle')

  assert.match(orderSource, /orderForm\.shipments/)
  assert.match(orderSource, /function defaultOrderShipment\(/)
  assert.match(orderSource, /function normalizeOrderShipment\(/)
  assert.match(orderSource, /function addOrderShipment\(/)
  assert.match(orderSource, /function discardUnsavedOrderShipment\(/)
  assert.match(orderSource, /v-for="\(shipment, index\) in orderForm\.shipments"/)
  assert.match(orderSource, /v-if="!shipment\.id"[\s\S]*@click="discardUnsavedOrderShipment\(index\)"/)
  assert.match(orderSource, /最后修改：[\s\S]*shipment\.updateTime[\s\S]*shipment\.updaterName/)
  assert.doesNotMatch(orderSource, /orderForm\.(?:expressCompany|expressNo)/)
  assert.doesNotMatch(orderSource, /orderDetail\.(?:expressCompany|expressNo)/)
  assert.doesNotMatch(orderSource, /row\.(?:expressCompany|expressNo)/)
  assert.match(payloadSource, /shipments:\s*orderForm\.shipments\.map/)
  assert.match(payloadSource, /\{\s*id,\s*deliveryMode,\s*logisticsCompany,\s*trackingNo,\s*version\s*\}/)
  assert.match(orderSource, /shipmentDeliveryModeOptions/)
  assert.match(orderSource, /handleShipmentDeliveryModeChange/)
})

test('shipment editor enforces limits, required fields, and unique tracking numbers for tracked deliveries', () => {
  const validationSource = functionSource(orderSource, 'validateOrderForm', 'buildOrderPayload')

  assert.match(orderSource, /orderForm\.shipments\.length >= 50/)
  assert.match(validationSource, /orderForm\.shipments\.length > 50/)
  assert.match(validationSource, /shipment\.logisticsCompany/)
  assert.match(validationSource, /shipment\.trackingNo/)
  assert.match(validationSource, /isTrackableShipment\(shipment\)/)
  assert.match(validationSource, /快递物流必须填写物流公司/)
  assert.match(validationSource, /非可追踪发货方式不能填写物流单号/)
  assert.match(validationSource, /物流单号不能重复/)
  assert.match(validationSource, /requiresShippingDetails\.value[\s\S]*orderForm\.shipments/)
})

test('order list and detail render all shipments in stable order', () => {
  assert.match(orderSource, /v-for="shipment in row\.shipments"/)
  assert.match(orderSource, /row\.shipments\.length > 1[\s\S]*共 \{\{ row\.shipments\.length \}\} 单/)
  assert.match(orderSource, /orderDetail\.shipments/)
  assert.match(orderSource, /orderDetail\.shipments[\s\S]*shipment\.logisticsCompany[\s\S]*shipment\.trackingNo/)
})

test('shipment tracking asks for a four-digit phone suffix before it sends a tracking request', () => {
  assert.match(orderSource, /@show="prepareLogisticsTracking\(row, shipment\)"/)
  assert.match(orderSource, /请输入手机号尾号 4 位/)
  assert.match(orderSource, /@click\.stop="loadLogisticsTracking\(row, shipment\)"/)
  assert.doesNotMatch(orderSource, /@show="loadLogisticsTracking\(row, shipment\)"/)
  assert.match(orderSource, /getOrderLogisticsTracking\(row\.orderId, shipment\.id, shipment\.version, tracking\.phoneSuffix\)/)
})

test('current-page export callback includes non-trackable delivery labels', () => {
  assert.match(orderSource, /<TableColumnSettings[\s\S]*:export-rows="visibleOrderRows"/)
  const callbackExpression = orderSource.match(/:export-cell="([^"]+)"/)?.[1]
  assert.ok(callbackExpression, 'order export callback binding must exist')

  assert.ok(buildStructuredExportData, 'shared table export formatter remains available')
  assert.match(orderSource, /if \(key === 'shipments'\) return \(row\.shipments \|\| \[\]\)[\s\S]*\.map\(shipment => shipmentListLabel\(shipment\)\)[\s\S]*\.join\('、'\)/u)
  assert.match(orderSource, /货拉拉 \/ 同城配送（无轨迹）/)
  assert.match(orderSource, /客户自提（无轨迹）/)
})
