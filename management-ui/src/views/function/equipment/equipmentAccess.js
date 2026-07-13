export function resolveEquipmentAccess(hasPermission) {
  return {
    canViewDetail: Boolean(hasPermission?.('equipment:detail')),
    canViewInspection: Boolean(hasPermission?.('equipment:inspection:list'))
  }
}

export function planEquipmentDrawerOpen(mode, access) {
  const requestDetail = mode === 'detail' && Boolean(access?.canViewDetail)
  const requestInspection = mode === 'inspection'
    ? Boolean(access?.canViewInspection)
    : requestDetail && Boolean(access?.canViewInspection)
  return { open: requestDetail || requestInspection, requestDetail, requestInspection }
}
