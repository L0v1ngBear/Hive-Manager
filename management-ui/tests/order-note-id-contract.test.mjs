import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const orderPageUrl = new URL('../src/views/function/order/order.vue', import.meta.url)
const migrationEntry = 'migrations/V20260730_001_sales_order_note_auto_increment.sql'
const migrationUrl = new URL(`../../db-migrations/${migrationEntry}`, import.meta.url)
const manifestUrl = new URL('../../db-migrations/migration_manifest.txt', import.meta.url)
const checksumsUrl = new URL('../../db-migrations/migration_checksums.sha256', import.meta.url)

test('order note IDs remain strings when an existing order is edited', async () => {
  const source = await readFile(orderPageUrl, 'utf8')

  assert.match(source, /id:\s*note\.id == null \? null : String\(note\.id\)/)
  assert.doesNotMatch(source, /(?:Number|parseInt)\(note\.id/)
})

test('order note auto-increment migration is the latest checksummed migration', async () => {
  const [migration, manifest, checksums] = await Promise.all([
    readFile(migrationUrl),
    readFile(manifestUrl, 'utf8'),
    readFile(checksumsUrl, 'utf8')
  ])
  const entries = manifest.trim().split(/\r?\n/)
  const digest = crypto.createHash('sha256').update(migration).digest('hex')

  assert.equal(entries.at(-1), migrationEntry)
  assert.equal(entries.filter((entry) => entry === migrationEntry).length, 1)
  assert.match(checksums, new RegExp(`^${digest}  ${migrationEntry.replace('.', '\\.')}$`, 'm'))
})
