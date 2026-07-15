export function resolveEquipmentAccess(hasPermission) {
  return {
    canViewList: Boolean(hasPermission?.('equipment:list')),
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

export function createLatestRequestGate() {
  let latestRequestId = 0
  return {
    begin: () => ++latestRequestId,
    isLatest: (requestId) => requestId === latestRequestId
  }
}

export function resolveInspectionEquipmentId(selectedDevice, detail) {
  return selectedDevice?.id ?? detail?.id ?? null
}
