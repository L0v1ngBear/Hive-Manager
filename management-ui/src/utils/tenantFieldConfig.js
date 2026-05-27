export const tenantFieldCatalog = {
  inventory: [
    { fieldKey: 'modelCode', fieldLabel: '型号', visible: true, required: true, sortNo: 10 },
    { fieldKey: 'spec', fieldLabel: '规格', visible: true, required: false, sortNo: 20 },
    { fieldKey: 'barCode', fieldLabel: '条码', visible: true, required: false, sortNo: 30 },
    { fieldKey: 'totalMeters', fieldLabel: '总米数', visible: true, required: true, sortNo: 40 },
    { fieldKey: 'remainingMeters', fieldLabel: '剩余米数', visible: true, required: false, sortNo: 50 },
    { fieldKey: 'location', fieldLabel: '库位', visible: true, required: false, sortNo: 60 },
    { fieldKey: 'status', fieldLabel: '库存状态', visible: true, required: false, sortNo: 70 },
    { fieldKey: 'updateTime', fieldLabel: '更新时间', visible: true, required: false, sortNo: 80 }
  ],
  receipt: [
    { fieldKey: 'modelCode', fieldLabel: '货物名称', visible: true, required: true, sortNo: 10 },
    { fieldKey: 'spec', fieldLabel: '规格', visible: true, required: false, sortNo: 20 },
    { fieldKey: 'meters', fieldLabel: '数量/米', visible: true, required: true, sortNo: 30 },
    { fieldKey: 'price', fieldLabel: '单价', visible: true, required: false, sortNo: 40 },
    { fieldKey: 'amount', fieldLabel: '金额', visible: true, required: false, sortNo: 50 },
    { fieldKey: 'remark', fieldLabel: '备注', visible: true, required: false, sortNo: 60 }
  ],
  employee: [
    { fieldKey: 'name', fieldLabel: '姓名', visible: true, required: true, sortNo: 10 },
    { fieldKey: 'empNo', fieldLabel: '工号', visible: true, required: false, sortNo: 20 },
    { fieldKey: 'departmentName', fieldLabel: '部门', visible: true, required: true, sortNo: 30 },
    { fieldKey: 'positionName', fieldLabel: '职位', visible: true, required: true, sortNo: 40 },
    { fieldKey: 'phone', fieldLabel: '手机号', visible: true, required: true, sortNo: 50 },
    { fieldKey: 'email', fieldLabel: '邮箱', visible: true, required: false, sortNo: 60 },
    { fieldKey: 'leaderName', fieldLabel: '直属负责人', visible: true, required: false, sortNo: 70 },
    { fieldKey: 'entryDate', fieldLabel: '入职日期', visible: true, required: false, sortNo: 80 },
    { fieldKey: 'attendanceRequired', fieldLabel: '考勤', visible: true, required: false, sortNo: 85 },
    { fieldKey: 'attendanceLocationNames', fieldLabel: '打卡地点', visible: true, required: false, sortNo: 86 },
    { fieldKey: 'status', fieldLabel: '状态', visible: true, required: false, sortNo: 90 }
  ],
  customer: [
    { fieldKey: 'customerName', fieldLabel: '客户名称', visible: true, required: true, sortNo: 10 },
    { fieldKey: 'customerType', fieldLabel: '客户类型', visible: true, required: true, sortNo: 20 },
    { fieldKey: 'contactName', fieldLabel: '首要联系人', visible: true, required: false, sortNo: 30 },
    { fieldKey: 'contactPhone', fieldLabel: '联系电话', visible: true, required: false, sortNo: 40 },
    { fieldKey: 'projectName', fieldLabel: '合作项目', visible: true, required: false, sortNo: 50 },
    { fieldKey: 'projectOwner', fieldLabel: '项目负责人', visible: true, required: false, sortNo: 60 },
    { fieldKey: 'projectCount', fieldLabel: '项目数量', visible: true, required: false, sortNo: 70 },
    { fieldKey: 'constructionArea', fieldLabel: '施工区域', visible: true, required: false, sortNo: 80 }
  ]
}

const CUSTOM_FIELD_KEY_PATTERN = /^custom_[a-zA-Z][a-zA-Z0-9_]{0,63}$/

export function isCustomTenantFieldKey(key) {
  return CUSTOM_FIELD_KEY_PATTERN.test(String(key || '').trim())
}

export function defaultTenantFieldConfig(moduleCode) {
  const catalog = tenantFieldCatalog[moduleCode] || []
  return catalog.reduce((config, item) => {
    config[item.fieldKey] = {
      key: item.fieldKey,
      label: item.fieldLabel,
      visible: item.visible !== false,
      required: item.required === true,
      sortNo: Number.isFinite(Number(item.sortNo)) ? Number(item.sortNo) : 999,
      fieldType: item.fieldType || 'text',
      custom: false
    }
    return config
  }, {})
}

export function mergeTenantFieldConfig(moduleCode, rows) {
  const config = defaultTenantFieldConfig(moduleCode)
  ;(Array.isArray(rows) ? rows : []).forEach((row) => {
    const key = String(row?.fieldKey || '').trim()
    if (!key) return
    const custom = isCustomTenantFieldKey(key)
    if (!config[key] && !custom) return
    config[key] = {
      ...config[key],
      key,
      label: row.fieldLabel || config[key]?.label || key,
      visible: row.visible !== false,
      required: row.required === true,
      sortNo: Number.isFinite(Number(row.sortNo)) ? Number(row.sortNo) : (config[key]?.sortNo || 999),
      fieldType: row.fieldType || config[key]?.fieldType || 'text',
      custom
    }
  })
  return config
}

export function customTenantFields(config) {
  return visibleTenantFields(config).filter((item) => item?.custom === true || isCustomTenantFieldKey(item?.key))
}

export function visibleTenantFields(config, fallbackKey) {
  const fields = Object.values(config || {})
    .filter((item) => item?.visible !== false)
    .sort((a, b) => Number(a.sortNo || 999) - Number(b.sortNo || 999))
  if (fields.length > 0) {
    return fields
  }
  const fallback = fallbackKey ? config?.[fallbackKey] : null
  return fallback ? [{ ...fallback, visible: true }] : []
}

export function tenantFieldLabel(config, key, fallback) {
  return config?.[key]?.label || fallback || key
}

export function tenantFieldRequired(config, key) {
  return config?.[key]?.required === true
}

export function tenantFieldVisible(config, key) {
  return config?.[key]?.visible !== false
}
