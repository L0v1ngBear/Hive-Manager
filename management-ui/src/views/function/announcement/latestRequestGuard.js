export function createLatestRequestGuard() {
  let latestRequestId = 0

  return {
    begin() {
      latestRequestId += 1
      return latestRequestId
    },
    commit(requestId, update) {
      if (requestId !== latestRequestId) return false
      update()
      return true
    }
  }
}
