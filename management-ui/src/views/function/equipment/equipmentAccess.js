export function resolveEquipmentAccess(hasPermission) {
  return {
    canViewDetail: Boolean(hasPermission?.('equipment:detail')),
    canViewInspection: Boolean(hasPermission?.('equipment:inspection:list'))
  }
}
