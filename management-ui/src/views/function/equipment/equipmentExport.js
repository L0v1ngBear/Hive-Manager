const formatDateTime = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : '--'

export function buildEquipmentExport(devices = []) {
  return {
    headers: ['设备', '类型/位置', '负责人', '最近巡检', '状态'],
    rows: devices.map((device) => [
      [device.equipmentName, device.equipmentCode].filter(Boolean).join(' '),
      [device.equipmentType || '--', device.location || '--'].join(' '),
      device.responsiblePerson || '--',
      formatDateTime(device.lastInspectionTime), device.status === 'enabled' ? '启用中' : '已停用'
    ])
  }
}
