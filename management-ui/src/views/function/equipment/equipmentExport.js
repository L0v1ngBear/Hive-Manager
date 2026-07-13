const formatDateTime = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : '--'

export function buildEquipmentExport(devices = []) {
  return {
    headers: ['设备名称', '设备编码', '设备类型', '设备位置', '负责人', '巡检周期（天）', '最近巡检', '状态'],
    rows: devices.map((device) => [
      device.equipmentName || '', device.equipmentCode || '', device.equipmentType || '--',
      device.location || '--', device.responsiblePerson || '--', device.inspectionCycleDays ?? 7,
      formatDateTime(device.lastInspectionTime), device.status === 'enabled' ? '启用中' : '已停用'
    ])
  }
}
