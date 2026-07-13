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
