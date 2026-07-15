import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const apiSource = readFileSync(new URL('../src/views/function/order/api/order.js', import.meta.url), 'utf8')
const pageSource = readFileSync(new URL('../src/views/function/order/order.vue', import.meta.url), 'utf8')
const controllerSource = readFileSync(
  new URL('../../management/src/main/java/my/hive/api/order/OrderController.java', import.meta.url),
  'utf8'
)

test('order API exposes one canonical route family', () => {
  for (const route of [
    '/orders',
    '/orders/status-summary',
    '/orders/attachment',
    '/orders/flow-print-task'
  ]) {
    assert.match(apiSource, new RegExp(route.replaceAll('/', '\\/')))
  }
  assert.doesNotMatch(apiSource, /\/order\/(sales|production)\//)
  assert.match(apiSource, /url: `\/orders\/\$\{orderId\}`, method: 'put', data/)
  assert.doesNotMatch(apiSource, /\/orders\/\$\{orderId\}\/(save|status)/)
  assert.doesNotMatch(apiSource, /\/order\/create/)
  assert.doesNotMatch(apiSource, /\/order\/next\//)
  assert.doesNotMatch(apiSource, /SalesOrder|ProductionOrder/)
  assert.match(controllerSource, /@PutMapping\("\/\{orderId\}"\)/)
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
