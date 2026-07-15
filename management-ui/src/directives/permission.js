import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { createPermissionDirectiveLifecycle } from './permissionState'

function normalizePermissions(value) {
  if (!value) return []
  return Array.isArray(value) ? value.filter(Boolean) : [value]
}

function canAccess(value) {
  const permissions = normalizePermissions(value)
  if (!permissions.length) return true
  const userStore = useUserStore()
  return userStore.hasAnyPermission(permissions)
}

export default createPermissionDirectiveLifecycle({
  isAllowed: canAccess,
  onBlocked: () => ElMessage.warning('当前账号暂无权限')
})
