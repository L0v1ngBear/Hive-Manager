import { hasPermission } from '../../../utils/permission.js'

const ORDER_STATUS_PERMISSION_PREFIX = 'order:status:'
const ORDER_STATUS_ACTIONS = new Set(['view', 'advance', 'rollback', 'cancel'])

function hasAllPermissions(permissionList, requiredPermissions) {
  return requiredPermissions.length > 0
    && requiredPermissions.every((permission) => hasPermission(permissionList, permission))
}

export function normalizeOrderStatusPermission(status) {
  return String(status || '').trim().replace(/_/g, '-')
}

export function orderStatusPermission(status, action) {
  const normalizedStatus = normalizeOrderStatusPermission(status)
  const normalizedAction = String(action || '').trim()
  if (!normalizedStatus || !ORDER_STATUS_ACTIONS.has(normalizedAction)) {
    return ''
  }
  return `${ORDER_STATUS_PERMISSION_PREFIX}${normalizedStatus}:${normalizedAction}`
}

export function canViewOrder(permissionList, order = {}) {
  return hasAllPermissions(permissionList, [orderStatusPermission(order.status, 'view')])
}

export function canViewOrderDetail(permissionList, order = {}) {
  return hasAllPermissions(permissionList, [
    'order:detail',
    orderStatusPermission(order.status, 'view')
  ])
}

export function canEditOrder(permissionList) {
  return hasAllPermissions(permissionList, ['order:update'])
}

export function canAdvanceOrder(permissionList, order = {}) {
  return hasAllPermissions(permissionList, [
    'order:update',
    orderStatusPermission(order.status, 'advance')
  ])
}

export function canRollbackOrder(permissionList, order = {}) {
  return hasAllPermissions(permissionList, [
    'order:update',
    orderStatusPermission(order.status, 'rollback')
  ])
}

export function canCancelOrder(permissionList, order = {}) {
  return hasAllPermissions(permissionList, [
    'order:update',
    orderStatusPermission(order.status, 'cancel')
  ])
}

export function canPrintOrder(permissionList) {
  return hasAllPermissions(permissionList, ['order:print'])
}
