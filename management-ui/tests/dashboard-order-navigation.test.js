import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import { buildOrderSummaryRoute } from '../src/views/dashboard/dashboardNavigation.js'

const dashboard = readFileSync(new URL('../src/views/dashboard/index.vue', import.meta.url), 'utf8')
const order = readFileSync(new URL('../src/views/function/order/order.vue', import.meta.url), 'utf8')

test('dashboard month and warning summaries target filtered order lists', () => {
  assert.deepEqual(buildOrderSummaryRoute('month', new Date(2026, 6, 29, 12)), {
    path: '/function/order',
    query: {
      createStart: '2026-07-01',
      createEnd: '2026-07-29'
    }
  })
  assert.deepEqual(buildOrderSummaryRoute('warning', new Date(2026, 6, 29, 12)), {
    path: '/function/order',
    query: { staleOnly: '1' }
  })
})

test('dashboard summaries are accessible navigation buttons and month card is white', () => {
  assert.match(dashboard, /type="button"[\s\S]*?bg-white[\s\S]*?aria-label="查看本月新增订单"[\s\S]*?@click="openOrderSummary\('month'\)"/)
  assert.match(dashboard, /aria-label="查看预警订单"[\s\S]*?@click="openOrderSummary\('warning'\)"/)
  assert.match(dashboard, /function openOrderSummary\(type\)[\s\S]*router\.push\(buildOrderSummaryRoute\(type\)\)/)
  assert.match(dashboard, /\.dashboard-summary-card--link:focus-visible\s*\{/)
})

test('order page consumes dashboard date and warning query filters', () => {
  assert.match(order, /route\.query\.createStart/)
  assert.match(order, /route\.query\.createEnd/)
  assert.match(order, /route\.query\.staleOnly/)
  assert.match(order, /filters\.createStart = routeCreateStart/)
  assert.match(order, /filters\.createEnd = routeCreateEnd/)
  assert.match(order, /filters\.staleOnly = routeStaleOnly/)
})
