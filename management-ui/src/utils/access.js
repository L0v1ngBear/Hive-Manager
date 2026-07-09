export function resolveAccessState(userStore, item = {}, featureMap = {}) {
  if (item.developerOnly && !userStore.isPlatformTenant) {
    return {
      ...item,
      disabled: true,
      disabledReason: '该入口仅平台管理员可访问'
    }
  }

  const requiredFeatures = item.features || (featureMap[item.path] ? [featureMap[item.path]] : [])
  const featureAllowed = !requiredFeatures.length || userStore.hasAnyFeature(requiredFeatures)
  const permissionAllowed = !item.permissions || userStore.hasAnyPermission(item.permissions)
  const disabled = !featureAllowed || !permissionAllowed
  const disabledReason = !featureAllowed
    ? '当前套餐暂未启用该功能'
    : !permissionAllowed
      ? '当前账号暂无权限'
      : ''

  return {
    ...item,
    disabled,
    disabledReason
  }
}

export function decorateAccessItems(userStore, items = [], featureMap = {}) {
  return items.map((item) => resolveAccessState(userStore, item, featureMap))
}

export function routeAccessDenied(userStore, meta = {}) {
  if (meta.developerOnly && !userStore.isPlatformTenant) {
    return '该页面仅平台管理员可访问'
  }
  if (Array.isArray(meta.features) && meta.features.length && !userStore.hasAnyFeature(meta.features)) {
    return '当前套餐暂未启用该功能'
  }
  if (Array.isArray(meta.permissions) && meta.permissions.length && !userStore.hasAnyPermission(meta.permissions)) {
    return '当前账号暂无权限'
  }
  return ''
}
