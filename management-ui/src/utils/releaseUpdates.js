// A persistent release event stream is idle until the server announces its deployed build.
export function startReleaseUpdates({
  buildId,
  baseUrl = '/',
  windowObject = window,
  EventSourceImpl = EventSource,
  fetchImpl = fetch,
}) {
  const storageKey = 'hive-last-auto-refresh'
  let stopped = false
  let checking = false
  const base = new URL(baseUrl, windowObject.location.href)
  const stream = new EventSourceImpl(new URL('api/release/events', base))

  function lastRefresh() {
    try { return windowObject.sessionStorage.getItem(storageKey) } catch { return null }
  }

  async function onRelease(event) {
    if (!buildId || stopped || checking) return
    checking = true
    try {
      const remote = JSON.parse(event.data).buildId
      if (typeof remote !== 'string' || !/^[a-zA-Z0-9._-]{1,100}$/.test(remote)
        || remote === buildId || lastRefresh() === remote
        || new URL(windowObject.location.href).searchParams.get('_hive_release') === remote) return
      // Verify the deployed HTML before leaving a working page. No periodic version requests.
      const indexUrl = new URL('index.html', base)
      indexUrl.searchParams.set('_hive_release', remote)
      const index = await fetchImpl(indexUrl, {
        cache: 'no-store', credentials: 'same-origin', signal: AbortSignal.timeout(5000),
      })
      if (!index.ok) return
      const html = await index.text()
      const htmlVersion = html.match(/<meta\s+name="hive-build-id"\s+content="([^"]+)"/)?.[1]
      if (stopped || htmlVersion !== remote) return
      try { windowObject.sessionStorage.setItem(storageKey, remote) } catch { /* Storage may be disabled. */ }
      const target = new URL(windowObject.location.href)
      target.searchParams.set('_hive_release', remote)
      stopped = true
      stream.close()
      windowObject.location.replace(target.href)
    } catch {
      // A malformed event, disconnect or incomplete upload must not interrupt the current page.
    } finally {
      checking = false
    }
  }

  stream.addEventListener('release', onRelease)
  return {
    stop() {
      stopped = true
      stream.removeEventListener('release', onRelease)
      stream.close()
    },
  }
}
