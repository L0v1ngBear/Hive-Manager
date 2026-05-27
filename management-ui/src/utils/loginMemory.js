const LOGIN_MEMORY_KEY = 'hive.web.login.memory'

function normalizeUsername(value) {
  return String(value || '').trim().slice(0, 80)
}

function safeLocalStorage() {
  return typeof window !== 'undefined' ? window.localStorage : null
}

export function readLoginMemory() {
  const storage = safeLocalStorage()
  if (!storage) {
    return { remember: false, username: '' }
  }

  try {
    const payload = JSON.parse(storage.getItem(LOGIN_MEMORY_KEY) || '{}')
    return {
      remember: payload?.remember === true,
      username: normalizeUsername(payload?.username)
    }
  } catch {
    storage.removeItem(LOGIN_MEMORY_KEY)
    return { remember: false, username: '' }
  }
}

export function saveLoginMemory({ remember, username } = {}) {
  const storage = safeLocalStorage()
  if (!storage) {
    return
  }

  if (!remember) {
    storage.removeItem(LOGIN_MEMORY_KEY)
    return
  }

  storage.setItem(LOGIN_MEMORY_KEY, JSON.stringify({
    remember: true,
    username: normalizeUsername(username)
  }))
}
