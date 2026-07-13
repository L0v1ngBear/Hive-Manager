export function createOverviewRequestGate() {
  let latestRequestId = 0
  return {
    begin: () => ++latestRequestId,
    isLatest: (requestId) => requestId === latestRequestId
  }
}
