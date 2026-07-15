export function createLatestRequest() {
  let sequence = 0
  return {
    begin() {
      const requestId = ++sequence
      return {
        commit(callback) {
          if (requestId !== sequence) return false
          callback()
          return true
        }
      }
    }
  }
}
