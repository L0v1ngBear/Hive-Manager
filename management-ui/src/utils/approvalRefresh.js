export const APPROVAL_CHANGED_EVENT = 'hive:approval-changed'

export function notifyApprovalChanged() {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent(APPROVAL_CHANGED_EVENT))
}

export function listenApprovalChanged(handler) {
  if (typeof window === 'undefined' || typeof handler !== 'function') {
    return () => {}
  }
  window.addEventListener(APPROVAL_CHANGED_EVENT, handler)
  return () => window.removeEventListener(APPROVAL_CHANGED_EVENT, handler)
}
