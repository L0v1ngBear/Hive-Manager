import assert from 'node:assert/strict'
import test from 'node:test'
import { readFileSync } from 'node:fs'
import { releaseVersionPlugin } from '../scripts/release-version-plugin.mjs'

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
