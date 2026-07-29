function toLocalDateValue(value) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function buildOrderSummaryRoute(type, now = new Date()) {
  if (type === 'warning') {
    return {
      path: '/function/order',
      query: { staleOnly: '1' }
    }
  }

  const monthStart = new Date(now.getFullYear(), now.getMonth(), 1)
  return {
    path: '/function/order',
    query: {
      createStart: toLocalDateValue(monthStart),
      createEnd: toLocalDateValue(now)
    }
  }
}
