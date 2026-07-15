import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

import {
  canAdvanceOrder,
  canCancelOrder,
  canEditOrder,
  canPrintOrder,
  canRollbackOrder,
  canViewOrder,
  canViewOrderDetail,
  orderStatusPermission
} from '../src/views/function/order/orderPermissions.js'

const pendingShipOrder = { orderId: 'ORDER-001', status: 'pending_ship' }

test('order status permissions use exact V3 action leaves', () => {
  assert.equal(orderStatusPermission('pending_ship', 'view'), 'order:status:pending-ship:view')
  assert.equal(orderStatusPermission('pending_ship', 'advance'), 'order:status:pending-ship:advance')
  assert.equal(orderStatusPermission('pending_ship', 'rollback'), 'order:status:pending-ship:rollback')
  assert.equal(orderStatusPermission('pending_ship', 'cancel'), 'order:status:pending-ship:cancel')
  assert.equal(orderStatusPermission('pending_ship', ''), '')
})

test('order list and detail require exact view permissions', () => {
  assert.equal(canViewOrder(['order:status:pending-ship:view'], pendingShipOrder), true)
  assert.equal(canViewOrder(['order:status:pending-ship'], pendingShipOrder), false)
  assert.equal(
    canViewOrderDetail(['order:detail', 'order:status:pending-ship:view'], pendingShipOrder),
    true
  )
  assert.equal(canViewOrderDetail(['order:detail'], pendingShipOrder), false)
  assert.equal(canViewOrderDetail(['order:status:pending-ship:view'], pendingShipOrder), false)
})

test('order mutations require update plus their exact action leaf', () => {
  assert.equal(canEditOrder(['order:update'], pendingShipOrder), true)
  assert.equal(canEditOrder(['order:status:pending-ship'], pendingShipOrder), false)

  assert.equal(
    canAdvanceOrder(['order:update', 'order:status:pending-ship:advance'], pendingShipOrder),
    true
  )
  assert.equal(canAdvanceOrder(['order:update'], pendingShipOrder), false)
  assert.equal(canAdvanceOrder(['order:status:pending-ship:advance'], pendingShipOrder), false)

  assert.equal(
    canRollbackOrder(['order:update', 'order:status:pending-ship:rollback'], pendingShipOrder),
    true
  )
  assert.equal(canRollbackOrder(['order:update'], pendingShipOrder), false)

  assert.equal(
    canCancelOrder(['order:update', 'order:status:pending-ship:cancel'], pendingShipOrder),
    true
  )
  assert.equal(canCancelOrder(['order:update'], pendingShipOrder), false)
})

test('order flow printing requires order:print only', () => {
  assert.equal(canPrintOrder(['order:print'], pendingShipOrder), true)
  assert.equal(canPrintOrder(['order:status:pending-ship'], pendingShipOrder), false)
  assert.equal(canPrintOrder(['order:update'], pendingShipOrder), false)
})

test('order page binds list visibility and actions to dedicated guards', () => {
  const source = fs.readFileSync(
    new URL('../src/views/function/order/order.vue', import.meta.url),
    'utf8'
  )

  assert.match(source, /v-for="row in visibleOrderRows"/)
  assert.match(source, /canViewOrderDetail\(row\)/)
  assert.match(source, /canPrintOrderFlowCode\(row\)/)
  assert.match(source, /canAdvanceOrder\(row\)/)
  assert.match(source, /canRollbackOrder\(row\)/)
  assert.match(source, /canCancelCurrentOrder/)
  assert.match(source, /:disabled="!canSelectOrderStatus\(status\.value\)"/)
  assert.match(source, /status === editingOrderStatus\.value/)
  assert.match(source, /status === 'cancelled' && canCancelCurrentOrder\.value/)
  assert.doesNotMatch(source, /canMutateOrderStatus/)
  assert.doesNotMatch(source, /\$\{ORDER_STATUS_PERMISSION_PREFIX\}\$\{normalizedStatus\}`/)
})
