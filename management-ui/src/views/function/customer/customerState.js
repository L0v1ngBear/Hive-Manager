function responseStatus(error) {
  return Number(
    error?.response?.status
    || error?.response?.data?.code
    || error?.status
    || error?.code
    || 0
  )
}

export function resolveCustomerDetailOutcome(error) {
  if (!error) return { empty: true, error: null }
  const status = responseStatus(error)
  if (status === 401) {
    return { empty: false, error: { icon: 'warning', title: '登录状态已失效', message: '请重新登录后再查看客户详情。' } }
  }
  if (status === 403) {
    return { empty: false, error: { icon: 'warning', title: '暂无客户详情权限', message: '当前账号没有 customer:detail 权限，请联系管理员。' } }
  }
  if (status >= 500) {
    return { empty: false, error: { icon: 'error', title: '客户服务暂时不可用', message: '服务器处理失败，请稍后重试。' } }
  }
  return { empty: false, error: { icon: 'error', title: '客户详情加载失败', message: '网络连接异常，请检查网络后重试。' } }
}
