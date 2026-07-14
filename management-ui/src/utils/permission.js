export function hasPermission(permissionList, permCode) {
  if (!Array.isArray(permissionList) || typeof permCode !== 'string' || !permCode.trim()) {
    return false
  }

  const normalizedCode = permCode.trim()
  return !permissionList.includes(`!${normalizedCode}`)
    && permissionList.includes(normalizedCode)
}

export function hasAnyPermission(permissionList, permCodes) {
  if (!Array.isArray(permCodes) || !permCodes.length) {
    return true
  }
  return permCodes.some((permCode) => hasPermission(permissionList, permCode))
}
