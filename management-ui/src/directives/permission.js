import { useUserStore } from '@/stores/user'

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

function applyPermission(el, binding) {
  if (el.__permissionDisplay === undefined) {
    el.__permissionDisplay = el.style.display || ''
  }
  el.style.display = canAccess(binding.value) ? el.__permissionDisplay : 'none'
}

export default {
  mounted: applyPermission,
  updated: applyPermission
}
