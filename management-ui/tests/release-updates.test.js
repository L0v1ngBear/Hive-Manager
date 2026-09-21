import assert from 'node:assert/strict'
import test from 'node:test'
import { readFileSync } from 'node:fs'
import { startReleaseUpdates } from '../src/utils/releaseUpdates.js'
import { releaseVersionPlugin } from '../scripts/release-version-plugin.mjs'

function harness({ remote = 'new', html = 'new', failed = false, storageBlocked = false } = {}) {
  const win = new EventTarget()
  const doc = new EventTarget()
  doc.visibilityState = 'visible'
  const timers = new Map(), storage = new Map([['token', 'keep-login']]), calls = [], navigations = [], notices = []
  let timerId = 0
  win.location = { href: 'https://example.test/function/order?filter=mine#row', replace: value => navigations.push(value) }
  win.setInterval = (fn, delay) => { const id = ++timerId; timers.set(id, { fn, delay }); return id }
  win.setTimeout = win.setInterval
  win.clearInterval = id => timers.delete(id)
  win.clearTimeout = win.clearInterval
  win.sessionStorage = { getItem: key => { if (storageBlocked) throw Error('blocked'); return storage.get(key) }, setItem: (key, value) => storage.set(key, value) }
  const fetchImpl = async (url, options) => {
    calls.push({ url: String(url), options })
    if (failed) throw Error('offline')
    return { ok: true, json: async () => ({ buildId: remote }), text: async () => `<meta name="hive-build-id" content="${html}">` }
  }
  const updater = startReleaseUpdates({ buildId: 'old', windowObject: win, documentObject: doc, fetchImpl, notify: message => notices.push(message) })
  return { updater, win, doc, timers, storage, calls, navigations, notices }
}

test('a new ready release warns then refreshes without clearing login or route', async () => {
  const h = harness()
  await h.updater.check()
  assert.equal(h.calls.length, 2)
  assert.ok(h.calls.every(c => c.options.cache === 'no-store'))
  assert.equal(h.notices.length, 1)
  assert.equal(h.navigations.length, 0)
  const timer = [...h.timers.values()].find(t => t.delay === 10_000)
  assert.ok(timer)
  timer.fn()
  const url = new URL(h.navigations[0])
  assert.equal(url.pathname, '/function/order')
  assert.equal(url.searchParams.get('filter'), 'mine')
  assert.equal(url.searchParams.get('_hive_release'), 'new')
  assert.equal(url.hash, '#row')
  assert.equal(h.storage.get('token'), 'keep-login')
  h.updater.stop()
})

test('same version, failed checks, and partial deployments never refresh', async () => {
  for (const options of [{ remote: 'old' }, { failed: true }, { html: 'old' }, { remote: '<invalid>' }]) {
    const h = harness(options)
    await h.updater.check()
    assert.equal(h.notices.length, 0)
    assert.equal(h.navigations.length, 0)
    h.updater.stop()
  }
})

test('one tab refreshes at most once for each announced release', async () => {
  const h = harness()
  h.storage.set('hive-last-auto-refresh', 'new')
  await h.updater.check()
  assert.equal(h.notices.length, 0)
  h.updater.stop()
  const restricted = harness({ storageBlocked: true })
  restricted.win.location.href = 'https://example.test/function/order?_hive_release=new'
  await restricted.updater.check()
  assert.equal(restricted.notices.length, 0)
  restricted.updater.stop()
})

test('storage restrictions do not block update and pending checks cannot schedule duplicates', async () => {
  const h = harness({ storageBlocked: true })
  await Promise.all([h.updater.check(), h.updater.check()])
  await h.updater.check()
  assert.equal(h.notices.length, 1)
  h.updater.stop()
  assert.equal(h.timers.size, 0)
})

test('hidden tabs check on return, polling and preload failures also trigger detection', async () => {
  const h = harness({ remote: 'old' })
  h.doc.visibilityState = 'hidden'
  await h.updater.check()
  assert.equal(h.calls.length, 0)
  h.doc.visibilityState = 'visible'
  h.doc.dispatchEvent(new Event('visibilitychange'))
  await new Promise(resolve => setImmediate(resolve))
  h.win.dispatchEvent(new Event('focus'))
  await new Promise(resolve => setImmediate(resolve))
  h.win.dispatchEvent(new Event('vite:preloadError'))
  await new Promise(resolve => setImmediate(resolve))
  await [...h.timers.values()].find(t => t.delay === 30_000).fn()
  assert.equal(h.calls.length, 4)
  h.updater.stop()
  h.win.dispatchEvent(new Event('focus'))
  assert.equal(h.calls.length, 4)
})

test('each production build embeds the same identifier in code, HTML and version metadata', () => {
  const plugin = releaseVersionPlugin('build-123')
  assert.equal(plugin.config().define['import.meta.env.VITE_HIVE_BUILD_ID'], '"build-123"')
  assert.deepEqual(plugin.transformIndexHtml()[0].attrs, { name: 'hive-build-id', content: 'build-123' })
  const emitted = []
  plugin.generateBundle.call({ emitFile: asset => emitted.push(asset) })
  assert.equal(emitted[0].fileName, 'version.json')
  assert.equal(JSON.parse(emitted[0].source).buildId, 'build-123')
})

test('entry documents bypass caches while hashed assets retain immutable caching', () => {
  const config = readFileSync(new URL('../../deploy/nginx/conf.d/hive.conf', import.meta.url), 'utf8')
  const entry = config.slice(config.indexOf('root /usr/share/nginx/html;', config.indexOf('location /assets/')))
  assert.match(entry, /Cache-Control "no-store, no-cache, must-revalidate" always/)
  assert.match(entry, /try_files \$uri \$uri\/ \/index.html/)
  assert.match(config, /location \/assets\/[\s\S]*Cache-Control "public, immutable"/)
  const main = readFileSync(new URL('../src/main.js', import.meta.url), 'utf8')
  assert.match(main, /import\.meta\.env\.PROD/)
  assert.match(main, /startReleaseUpdates/)
})
