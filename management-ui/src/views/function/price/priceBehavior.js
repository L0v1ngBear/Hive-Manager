export function normalizeOptionalNumber(value) {
  return value === '' || value == null ? undefined : Number(value)
}

export function presentPriceOverrides(rows) {
  return rows
    .filter((item) => item.customerId && item.price !== '' && item.price != null)
    .map((item) => ({ ...item, customerId: Number(item.customerId), price: Number(item.price) }))
}

export function createLatestRequestGate() {
  let sequence = 0
  return {
    begin() { sequence += 1; return sequence },
    invalidate() { sequence += 1 },
    isCurrent(requestId) { return requestId === sequence }
  }
}
