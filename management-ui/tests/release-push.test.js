import assert from 'node:assert/strict'
import test from 'node:test'
import { readFileSync } from 'node:fs'
import { startReleaseUpdates } from '../src/utils/releaseUpdates.js'

function harness({ html = 'new', failed = false, last = null } = {}) {
  const storage = new Map([['token', 'keep-login'], ['hive-last-auto-refresh', last]])
  const calls = [], navigations = []
  let connection
  class Stream extends EventTarget {
    constructor(url) { super(); this.url = String(url); this.closed = false; connection = this }
    close() { this.closed = true }
  }
  const win = {
    location: { href: 'https://example.test/function/order?filter=mine#row', replace: url => navigations.push(url) },
    sessionStorage: { getItem: key => storage.get(key), setItem: (key, value) => storage.set(key, value) },
  }
  const updater = startReleaseUpdates({ buildId: 'old', windowObject: win, EventSourceImpl: Stream,
    fetchImpl: async (url, options) => {
      calls.push({ url: String(url), options })
      if (failed) throw Error('offline')
      return { ok: true, text: async () => `<meta name="hive-build-id" content="${html}">` }
    },
  })
  const emit = async data => {
    connection.dispatchEvent(new MessageEvent('release', { data: JSON.stringify(data) }))
    await new Promise(resolve => setImmediate(resolve))
  }
  return { updater, storage, calls, navigations, emit, connection }
}

test('one persistent connection stays idle without polling or focus listeners', async () => {
  const h = harness()
  assert.equal(h.connection.url, 'https://example.test/api/release/events')
  assert.equal(h.calls.length, 0)
  await h.emit({ buildId: 'old' })
  assert.equal(h.calls.length, 0)
  assert.equal(h.navigations.length, 0)
  h.updater.stop()
  assert.equal(h.connection.closed, true)
})

test('a new release event refreshes immediately and silently, retaining login and address', async () => {
  const h = harness()
  await h.emit({ buildId: 'new' })
  assert.equal(h.calls.length, 1)
  assert.equal(h.calls[0].options.cache, 'no-store')
  assert.ok(h.calls[0].url.includes('index.html'))
  const url = new URL(h.navigations[0])
  assert.equal(url.pathname, '/function/order')
  assert.equal(url.searchParams.get('filter'), 'mine')
  assert.equal(url.searchParams.get('_hive_release'), 'new')
  assert.equal(url.hash, '#row')
  assert.equal(h.storage.get('token'), 'keep-login')
  assert.equal(h.connection.closed, true)
  await h.emit({ buildId: 'new' })
  assert.equal(h.navigations.length, 1)
})

test('invalid events, unavailable HTML, partial deployment and repeated releases do not refresh', async () => {
  for (const options of [{ failed: true }, { html: 'old' }, { last: 'new' }]) {
    const h = harness(options)
    await h.emit({ buildId: 'new' })
    await h.emit({ buildId: '<invalid>' })
    assert.equal(h.navigations.length, 0)
    h.updater.stop()
  }
})

test('the production entry and updater contain no polling, visibility checks or update toast', () => {
  const source = readFileSync(new URL('../src/utils/releaseUpdates.js', import.meta.url), 'utf8')
  const main = readFileSync(new URL('../src/main.js', import.meta.url), 'utf8')
  assert.doesNotMatch(source, /setInterval|setTimeout|visibilitychange|['"]focus['"]|notify|version\.json/)
  assert.doesNotMatch(main, /ElMessage|updates\.check|notify/)
  const nginx = readFileSync(new URL('../../deploy/nginx/conf.d/hive.conf', import.meta.url), 'utf8')
  assert.match(nginx, /location = \/api\/release\/events[\s\S]*?proxy_buffering off/)
  const compose = readFileSync(new URL('../../deploy/docker-compose.yml', import.meta.url), 'utf8')
  assert.match(compose, /HIVE_UI_RELEASE_VERSION_FILE: \/app\/ui-release\/version\.json/)
  assert.match(compose, /\.\/management-ui\/dist\/version\.json:\/app\/ui-release\/version\.json:ro/)
})
