export function createLatestLoadingController(setLoading) {
  let requestId = 0
  return {
    begin() { requestId += 1; setLoading(true); return requestId },
    invalidate() { requestId += 1; setLoading(false) },
    isCurrent(id) { return id === requestId },
    finish(id) { if (id === requestId) setLoading(false) }
  }
}
