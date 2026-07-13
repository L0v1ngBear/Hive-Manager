const ORDER_ENTRY_PERMISSIONS = new Set(['order:list', 'order:detail'])
const ORDER_STATUS_PERMISSION_PREFIX = 'order:status:'
const LEGACY_ORDER_PERMISSION_MAP = {
  'order:list': [
    'sales:order:list',
    'sales:order:detail',
    'sales:order:status',
    'sales:order:pre-confirm',
    'sales:order:fulfillment',
    'production:order:list',
    'production:order:detail',
    'production:order:log',
    'production:order:status',
    'production:order:pre-production',
    'production:order:fulfillment'
  ],
  'order:detail': [
    'sales:order:detail',
    'production:order:detail',
    'production:order:log'
  ],
  'order:create': [
    'sales:order:add',
    'sales:order:status',
    'production:order:add',
    'production:order:status'
  ],
  'order:status:*': [
    'sales:order:status',
    'sales:order:pre-confirm',
    'sales:order:fulfillment',
    'production:order:status',
    'production:order:pre-production',
    'production:order:fulfillment'
  ]
}

export function hasPermission(permissionList, permCode) {
  if (!Array.isArray(permissionList) || !permCode) {
    return false
  }

  if (matchesPermission(permissionList, permCode, '!')) {
    return false
  }

  if (matchesLegacyOrderPermission(permissionList, permCode, '!')) {
    return false
  }

  if (matchesPermission(permissionList, permCode, '')) {
    return true
  }

  if (matchesLegacyOrderPermission(permissionList, permCode, '')) {
    return true
  }

  return ORDER_ENTRY_PERMISSIONS.has(permCode) && hasOrderStatusPermission(permissionList)
}

function matchesPermission(permissionList, permCode, prefix) {
  if (permissionList.includes(`${prefix}*`) || permissionList.includes(`${prefix}*:*`)) {
    return true
  }

  if (permissionList.includes(`${prefix}${permCode}`)) {
    return true
  }

  let current = permCode
  while (current.includes(':')) {
    current = current.substring(0, current.lastIndexOf(':'))
    if (permissionList.includes(`${prefix}${current}:*`)) {
      return true
    }
  }
  return false
}

function matchesLegacyOrderPermission(permissionList, permCode, prefix) {
  const legacyCodes = LEGACY_ORDER_PERMISSION_MAP[permCode]
    || (typeof permCode === 'string' && permCode.startsWith(ORDER_STATUS_PERMISSION_PREFIX)
      ? LEGACY_ORDER_PERMISSION_MAP['order:status:*']
      : null)
  if (!Array.isArray(legacyCodes) || legacyCodes.length === 0) {
    return false
  }
  return legacyCodes.some((legacyCode) => matchesPermission(permissionList, legacyCode, prefix))
}

function hasOrderStatusPermission(permissionList) {
  return permissionList.some((permission) => {
    return typeof permission === 'string' && permission.startsWith(ORDER_STATUS_PERMISSION_PREFIX)
  })
}

export function hasAnyPermission(permissionList, permCodes) {
  if (!Array.isArray(permCodes) || !permCodes.length) {
    return true
  }
  return permCodes.some((permCode) => hasPermission(permissionList, permCode))
}
