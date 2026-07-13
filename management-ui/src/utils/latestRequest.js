export function createLatestRequestRunner({ onLoading, onSuccess, onError } = {}) {
  let requestId = 0

  return {
    async run(load) {
      const currentRequestId = ++requestId
      onLoading?.(true)
      try {
        const value = await load()
        if (currentRequestId === requestId) onSuccess?.(value)
        return value
      } catch (error) {
        if (currentRequestId === requestId) onError?.(error)
        return undefined
      } finally {
        if (currentRequestId === requestId) onLoading?.(false)
      }
    },
    invalidate() {
      requestId += 1
      onLoading?.(false)
    }
  }
}
