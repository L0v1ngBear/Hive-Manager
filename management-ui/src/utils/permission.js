export function hasPermission(permissionList, permCode) {
  if (!Array.isArray(permissionList) || !permCode) {
    return false
  }

  if (permissionList.includes('*') || permissionList.includes('*:*')) {
    return true
  }

  if (permissionList.includes(permCode)) {
    return true
  }

  let current = permCode
  while (current.includes(':')) {
    current = current.substring(0, current.lastIndexOf(':'))
    if (permissionList.includes(`${current}:*`)) {
      return true
    }
  }
  return false
}

export function hasAnyPermission(permissionList, permCodes) {
  if (!Array.isArray(permCodes) || !permCodes.length) {
    return true
  }
  return permCodes.some((permCode) => hasPermission(permissionList, permCode))
}
