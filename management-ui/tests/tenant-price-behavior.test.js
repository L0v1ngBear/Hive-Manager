import assert from 'node:assert/strict'
import test from 'node:test'
import { createLatestRequestGate, normalizeOptionalNumber, presentPriceOverrides, presentPriceLogs, resolvePriceCommands } from '../src/views/function/price/priceBehavior.js'

test('keeps numeric zero while normalizing absent query values', () => {
  assert.equal(normalizeOptionalNumber(0), 0)
  assert.equal(normalizeOptionalNumber(''), undefined)
  assert.equal(normalizeOptionalNumber(null), undefined)
})

test('keeps a zero-priced customer override in the publish payload', () => {
  assert.deepEqual(presentPriceOverrides([{ customerId: 7, customerName: '零价客户', price: 0 }]), [{ customerId: 7, customerName: '零价客户', price: 0 }])
})

test('only the newest asynchronous request may commit state', () => {
  const gate = createLatestRequestGate()
  const first = gate.begin()
  const second = gate.begin()
  assert.equal(gate.isCurrent(first), false)
  assert.equal(gate.isCurrent(second), true)
  gate.invalidate()
  assert.equal(gate.isCurrent(second), false)
})

test('renders the real PriceDetailVO logs contract without renaming response fields', () => {
  const detail = { logs: [{ createTime: '2026-07-13 09:30:00', remark: '调整基准价', oldPrice: 12.3, newPrice: 13.5 }] }
  assert.deepEqual(presentPriceLogs(detail), [{ createTime: '2026-07-13 09:30:00', remark: '调整基准价', oldPrice: 12.3, newPrice: 13.5 }])
})

test('new and adjust commands use distinct real permission combinations', () => {
  assert.deepEqual(resolvePriceCommands(true, false), { canCreate: true, canAdjust: false })
  assert.deepEqual(resolvePriceCommands(true, true), { canCreate: true, canAdjust: true })
  assert.deepEqual(resolvePriceCommands(false, true), { canCreate: false, canAdjust: false })
})
