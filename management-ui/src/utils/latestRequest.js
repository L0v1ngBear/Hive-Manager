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

export function createLatestRequest() {
  let sequence = 0
  return {
    begin() {
      const requestId = ++sequence
      return {
        isLatest: () => requestId === sequence,
        commit(callback) {
          if (requestId !== sequence) return false
          callback()
          return true
        }
      }
    }
  }
}

export function createSubmitGuard() {
  let pending = false
  return {
    get pending() {
      return pending
    },
    async run(callback) {
      if (pending) return false
      pending = true
      try {
        await callback()
        return true
      } finally {
        pending = false
      }
    }
  }
}
