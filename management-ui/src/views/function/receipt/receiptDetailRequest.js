export function createReceiptDetailRequestController({ setLoading, clearDetail }) {
  let requestId = 0

  return {
    begin() {
      const currentRequestId = ++requestId
      setLoading(true)
      clearDetail()
      return currentRequestId
    },
    invalidate() {
      requestId += 1
      clearDetail()
      setLoading(false)
    },
    isCurrent(currentRequestId) {
      return currentRequestId === requestId
    },
    finish(currentRequestId) {
      if (currentRequestId !== requestId) return false
      setLoading(false)
      return true
    }
  }
}
