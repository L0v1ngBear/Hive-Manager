// Poll plain static metadata independently of authentication and API response encryption.
export function startReleaseUpdates({
  buildId,
  baseUrl = '/',
  windowObject = window,
  documentObject = document,
  fetchImpl = fetch,
  notify = () => {},
}) {
  const storageKey = 'hive-last-auto-refresh'
  let stopped = false
  let checking = false
  let pending = false
  let refreshTimer

  function lastRefresh() {
    try { return windowObject.sessionStorage.getItem(storageKey) } catch { return null }
  }

  async function check() {
    if (!buildId || stopped || pending || checking || documentObject.visibilityState === 'hidden') return
    checking = true
    try {
      const options = { cache: 'no-store', credentials: 'same-origin', signal: AbortSignal.timeout(5000) }
      const base = new URL(baseUrl, windowObject.location.href)
      const versionUrl = new URL('version.json', base)
      versionUrl.searchParams.set('_', String(Date.now()))
      const response = await fetchImpl(versionUrl, options)
      if (!response.ok) return
      const remote = (await response.json()).buildId
      if (typeof remote !== 'string' || !/^[a-zA-Z0-9._-]{1,100}$/.test(remote)
        || remote === buildId || lastRefresh() === remote
        || new URL(windowObject.location.href).searchParams.get('_hive_release') === remote) return

      // During upload, version.json and index.html may temporarily belong to different builds.
      const indexUrl = new URL('index.html', base)
      indexUrl.searchParams.set('_hive_release', remote)
      const index = await fetchImpl(indexUrl, options)
      if (!index.ok) return
      const html = await index.text()
      const htmlVersion = html.match(/<meta\s+name="hive-build-id"\s+content="([^"]+)"/)?.[1]
      if (stopped || htmlVersion !== remote) return

      pending = true
      notify('系统已更新，10秒后自动刷新页面，请及时保存当前内容。')
      refreshTimer = windowObject.setTimeout(() => {
        if (stopped) return
        try { windowObject.sessionStorage.setItem(storageKey, remote) } catch { /* Storage may be disabled. */ }
        const target = new URL(windowObject.location.href)
        target.searchParams.set('_hive_release', remote)
        windowObject.location.replace(target.href)
      }, 10_000)
    } catch {
      // Offline, proxy errors and an incomplete upload must leave the current page usable.
    } finally {
      checking = false
    }
  }

  const onVisible = () => { void check() }
  const interval = windowObject.setInterval(check, 30_000)
  windowObject.addEventListener('focus', onVisible)
  windowObject.addEventListener('vite:preloadError', onVisible)
  documentObject.addEventListener('visibilitychange', onVisible)
  return {
    check,
    stop() {
      stopped = true
      windowObject.clearInterval(interval)
      windowObject.clearTimeout(refreshTimer)
      windowObject.removeEventListener('focus', onVisible)
      windowObject.removeEventListener('vite:preloadError', onVisible)
      documentObject.removeEventListener('visibilitychange', onVisible)
    },
  }
}
