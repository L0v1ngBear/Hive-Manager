import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

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
  assert.match(payloadSource, /\{\s*id,\s*logisticsCompany,\s*trackingNo,\s*version\s*\}/)
})

test('shipment editor enforces limits, required fields, and unique tracking numbers', () => {
  const validationSource = functionSource(orderSource, 'validateOrderForm', 'buildOrderPayload')

  assert.match(orderSource, /orderForm\.shipments\.length >= 50/)
  assert.match(validationSource, /orderForm\.shipments\.length > 50/)
  assert.match(validationSource, /shipment\.logisticsCompany/)
  assert.match(validationSource, /shipment\.trackingNo/)
  assert.match(validationSource, /物流单号不能重复/)
  assert.match(validationSource, /requiresShippingDetails\.value[\s\S]*orderForm\.shipments/)
})

test('order list and detail render all shipments in stable order', () => {
  assert.match(orderSource, /v-for="shipment in row\.shipments"/)
  assert.match(orderSource, /row\.shipments\.length > 1[\s\S]*共 \{\{ row\.shipments\.length \}\} 单/)
  assert.match(orderSource, /orderDetail\.shipments/)
  assert.match(orderSource, /orderDetail\.shipments[\s\S]*shipment\.logisticsCompany[\s\S]*shipment\.trackingNo/)
})
