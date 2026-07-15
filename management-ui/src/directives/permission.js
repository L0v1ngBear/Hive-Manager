import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

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
  const allowed = canAccess(binding.value)
  if (!el.__permissionState) {
    el.__permissionState = {
      disabled: el.disabled,
      ariaDisabled: el.getAttribute('aria-disabled'),
      title: el.getAttribute('title'),
      cursor: el.style.cursor,
      permissionDisabledClass: el.classList.contains('is-permission-disabled')
    }
  }

  if (allowed) {
    restorePermission(el)
    return
  }

  disablePermission(el)
}

function disablePermission(el) {
  el.setAttribute('aria-disabled', 'true')
  el.setAttribute('title', '当前账号暂无权限')
  el.classList.add('is-permission-disabled')
  el.style.cursor = 'not-allowed'
  if ('disabled' in el) {
    el.disabled = true
  }
  if (!el.__permissionBlocker) {
    el.__permissionBlocker = (event) => {
      event.preventDefault()
      event.stopImmediatePropagation()
      ElMessage.warning('当前账号暂无权限')
    }
    el.addEventListener('click', el.__permissionBlocker, true)
  }
}

function restorePermission(el) {
  const state = el.__permissionState || {}
  if ('disabled' in el) {
    el.disabled = Boolean(state.disabled)
  }
  if (state.ariaDisabled === null || state.ariaDisabled === undefined) {
    el.removeAttribute('aria-disabled')
  } else {
    el.setAttribute('aria-disabled', state.ariaDisabled)
  }
  if (state.title === null || state.title === undefined) {
    el.removeAttribute('title')
  } else {
    el.setAttribute('title', state.title)
  }
  if (!state.permissionDisabledClass) {
    el.classList.remove('is-permission-disabled')
  }
  el.style.cursor = state.cursor || ''
  if (el.__permissionBlocker) {
    el.removeEventListener('click', el.__permissionBlocker, true)
    el.__permissionBlocker = null
  }
}

export default {
  mounted: applyPermission,
  updated: applyPermission,
  unmounted(el) {
    if (el.__permissionBlocker) {
      el.removeEventListener('click', el.__permissionBlocker, true)
    }
  }
}
