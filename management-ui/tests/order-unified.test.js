import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const apiSource = readFileSync(new URL('../src/views/function/order/api/order.js', import.meta.url), 'utf8')
const pageSource = readFileSync(new URL('../src/views/function/order/order.vue', import.meta.url), 'utf8')

test('order API exposes one canonical route family', () => {
  for (const route of [
    '/order/page',
    '/order/status-summary',
    '/order/create',
    '/order/attachment/upload',
    '/order/flow-print-task'
  ]) {
    assert.match(apiSource, new RegExp(route.replaceAll('/', '\\/')))
  }
  assert.doesNotMatch(apiSource, /\/order\/(sales|production)\//)
  assert.doesNotMatch(apiSource, /SalesOrder|ProductionOrder/)
})

test('order page contains no legacy sales/production tab branches', () => {
  for (const legacyToken of [
    'currentTab',
    'productionState',
    'productionForm',
    'productionDetail',
    'switchTab',
    'getProductionOrder',
    'createProductionOrder'
  ]) {
    assert.doesNotMatch(pageSource, new RegExp(legacyToken), `legacy token remains: ${legacyToken}`)
  }
})
