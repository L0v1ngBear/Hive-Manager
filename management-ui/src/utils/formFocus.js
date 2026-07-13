import { nextTick } from 'vue'
import { ElMessage } from 'element-plus'

function escapeSelector(value) {
  if (window.CSS && typeof window.CSS.escape === 'function') {
    return window.CSS.escape(value)
  }
  return String(value).replace(/["\\]/g, '\\$&')
}

function findFocusable(element) {
  if (!element) return null
  if (/^(INPUT|SELECT|TEXTAREA|BUTTON)$/.test(element.tagName) && !element.disabled) {
    return element
  }
  return element.querySelector('input:not([disabled]), select:not([disabled]), textarea:not([disabled]), button:not([disabled])')
}

export async function focusFormField(field, options = {}) {
  if (!field) return
  await nextTick()

  const root = options.root || document
  const selector = [
    `[data-field="${escapeSelector(field)}"]`,
    `[data-required-field="${escapeSelector(field)}"]`,
    `[name="${escapeSelector(field)}"]`
  ].join(',')
  const element = root.querySelector(selector)
  if (!element) return

  element.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' })
  const focusTarget = findFocusable(element)
  window.setTimeout(() => {
    focusTarget?.focus?.({ preventScroll: true })
    element.classList.add('form-field-attention')
    window.setTimeout(() => element.classList.remove('form-field-attention'), 1800)
  }, 180)
}

export function warnAndFocusField(message, field, options = {}) {
  ElMessage.warning(message)
  focusFormField(field, options)
  return false
}
