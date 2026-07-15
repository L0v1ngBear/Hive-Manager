const permissionDisabledClass = 'is-permission-disabled'
const permissionDeniedTitle = '当前账号暂无权限'
const activationKeys = new Set(['Enter', ' ', 'Spacebar'])

function restoreAttribute(element, name, imposedValue, previousValue) {
  if (element.getAttribute(name) !== imposedValue) return
  if (previousValue === null) {
    element.removeAttribute(name)
  } else {
    element.setAttribute(name, previousValue)
  }
}

function createPermissionDenialController({ onBlocked = () => {} } = {}) {
  const deniedElements = new WeakMap()

  function enterDenied(element) {
    if (deniedElements.has(element)) return

    const blockActivation = (event) => {
      if (event.type === 'keydown' && !activationKeys.has(event.key)) return
      event.preventDefault()
      event.stopImmediatePropagation()
      onBlocked()
    }
    const state = {
      ariaDisabled: element.getAttribute('aria-disabled'),
      blockActivation,
      cursor: element.style.cursor,
      hadPermissionClass: element.classList.contains(permissionDisabledClass),
      title: element.getAttribute('title')
    }
    deniedElements.set(element, state)

    element.setAttribute('aria-disabled', 'true')
    element.setAttribute('title', permissionDeniedTitle)
    element.classList.add(permissionDisabledClass)
    element.style.cursor = 'not-allowed'
    element.addEventListener('click', blockActivation, true)
    element.addEventListener('keydown', blockActivation, true)
  }

  function leaveDenied(element) {
    const state = deniedElements.get(element)
    if (!state) return

    element.removeEventListener('click', state.blockActivation, true)
    element.removeEventListener('keydown', state.blockActivation, true)
    restoreAttribute(element, 'aria-disabled', 'true', state.ariaDisabled)
    restoreAttribute(element, 'title', permissionDeniedTitle, state.title)
    if (element.style.cursor === 'not-allowed') {
      element.style.cursor = state.cursor
    }
    if (!state.hadPermissionClass) {
      element.classList.remove(permissionDisabledClass)
    }
    deniedElements.delete(element)
  }

  return {
    cleanup: leaveDenied,
    update(element, denied) {
      if (denied) {
        enterDenied(element)
      } else {
        leaveDenied(element)
      }
    }
  }
}

export function createPermissionDirectiveLifecycle({ isAllowed, onBlocked } = {}) {
  const permissionDenial = createPermissionDenialController({ onBlocked })
  const applyPermission = (element, binding) => {
    permissionDenial.update(element, !isAllowed(binding.value))
  }

  return {
    beforeUpdate(element) {
      permissionDenial.cleanup(element)
    },
    mounted: applyPermission,
    updated: applyPermission,
    unmounted(element) {
      permissionDenial.cleanup(element)
    }
  }
}
