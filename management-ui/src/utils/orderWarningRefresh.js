export const ORDER_WARNING_CHANGED_EVENT = 'hive-order-warning-changed'

export function notifyOrderWarningChanged(count) {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent(ORDER_WARNING_CHANGED_EVENT, {
    detail: { count: Number(count || 0) }
  }))
}

export function listenOrderWarningChanged(handler) {
  if (typeof window === 'undefined' || typeof handler !== 'function') {
    return () => {}
  }
  window.addEventListener(ORDER_WARNING_CHANGED_EVENT, handler)
  return () => window.removeEventListener(ORDER_WARNING_CHANGED_EVENT, handler)
}
