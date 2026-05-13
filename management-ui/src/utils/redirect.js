const LOGIN_PATH = '/login'
const DEFAULT_HOME_PATH = '/dashboard'

/**
 * 统一清洗登录后的跳转地址，避免 /login?redirect=/login?... 反复嵌套。
 * 只允许站内绝对路径，外部链接或登录页自身都会回到默认首页。
 */
export function normalizeLoginRedirect(value, fallback = DEFAULT_HOME_PATH) {
  if (typeof value !== 'string' || !value.trim()) {
    return fallback
  }

  let candidate = value.trim()
  for (let index = 0; index < 8; index += 1) {
    if (!candidate.startsWith('/') || candidate.startsWith('//')) {
      return fallback
    }

    try {
      const url = new URL(candidate, window.location.origin)
      if (url.origin !== window.location.origin) {
        return fallback
      }

      if (url.pathname === LOGIN_PATH) {
        const nestedRedirect = url.searchParams.get('redirect')
        if (nestedRedirect && nestedRedirect !== candidate) {
          candidate = nestedRedirect
          continue
        }
        return fallback
      }

      return `${url.pathname}${url.search}${url.hash}` || fallback
    } catch {
      return fallback
    }
  }

  return fallback
}

export function isLoginPath(value) {
  if (typeof value !== 'string' || !value) {
    return false
  }

  try {
    return new URL(value, window.location.origin).pathname === LOGIN_PATH
  } catch {
    return value === LOGIN_PATH
  }
}

export function buildLoginQuery(targetPath) {
  if (!targetPath || targetPath === '/' || isLoginPath(targetPath)) {
    return {}
  }

  const redirect = normalizeLoginRedirect(targetPath, '')
  return redirect ? { redirect } : {}
}
