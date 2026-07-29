import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const orderPage = readFileSync(
  new URL('../src/views/function/order/order.vue', import.meta.url),
  'utf8',
)

const confirmationStart = orderPage.indexOf('async function confirmRefreshOrderWarnings()')
const confirmationEnd = orderPage.indexOf('async function refreshOrderWarnings(', confirmationStart)
const confirmationHandler = confirmationStart >= 0 && confirmationEnd > confirmationStart
  ? orderPage.slice(confirmationStart, confirmationEnd)
  : ''

test('full order warning refresh is wired through a confirmation handler', () => {
  assert.match(
    orderPage,
    /class="order-warning-refresh-btn"[\s\S]*?@click="confirmRefreshOrderWarnings"/,
  )
  assert.doesNotMatch(
    orderPage,
    /class="order-warning-refresh-btn"[\s\S]*?@click="refreshOrderWarnings\(true\)"/,
  )
})

test('confirmation happens before the full warning refresh request', () => {
  const confirmIndex = confirmationHandler.indexOf('await ElMessageBox.confirm(')
  const refreshIndex = confirmationHandler.indexOf('await refreshOrderWarnings(true)')

  assert.ok(confirmIndex >= 0, 'confirmation dialog should be shown')
  assert.ok(refreshIndex > confirmIndex, 'refresh should only run after confirmation')
  assert.match(confirmationHandler, /error === 'cancel' \|\| error === 'close'[\s\S]*?return/)
})
