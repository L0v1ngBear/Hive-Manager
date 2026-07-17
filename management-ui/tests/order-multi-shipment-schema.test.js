import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { createHash } from 'node:crypto'
import { fileURLToPath } from 'node:url'

const uiRoot = fileURLToPath(new URL('..', import.meta.url))
const repoRoot = path.resolve(uiRoot, '..')

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function tableBlock(schema, tableName) {
  const match = schema.match(new RegExp('CREATE TABLE `' + tableName + '` \\(([\\s\\S]*?)\\n\\) ENGINE=', 'u'))
  assert.ok(match, `baseline must define ${tableName}`)
  return match[1]
}

test('order logistics use a normalized non-deletable shipment table', () => {
  const baseline = read('db-migrations/baseline/hive_schema_baseline.sql')
  const migrationPath = 'db-migrations/migrations/V20260717_001_order_multi_shipment.sql'
  assert.ok(fs.existsSync(path.join(repoRoot, migrationPath)), 'order shipment migration must exist')
  const migration = read(migrationPath)
  const manifest = read('db-migrations/migration_manifest.txt')
  const checksums = read('db-migrations/migration_checksums.sha256')

  assert.match(baseline, /CREATE TABLE `sales_order_shipment`/u)
  for (const column of ['logistics_company', 'tracking_no', 'sort_order', 'version', 'updater_name']) {
    assert.match(baseline, new RegExp('`' + column + '`', 'u'))
  }
  const salesOrderBlock = tableBlock(baseline, 'sales_order')
  assert.doesNotMatch(salesOrderBlock, /`express_company`|`express_no`/u)
  assert.match(migration, /CREATE TABLE `sales_order_shipment`/u)
  assert.match(migration, /DROP COLUMN `express_company`/u)
  assert.match(migration, /DROP COLUMN `express_no`/u)
  assert.match(manifest, /migrations\/V20260717_001_order_multi_shipment\.sql\s*$/mu)

  const migrationHash = createHash('sha256').update(read(migrationPath)).digest('hex')
  assert.match(checksums, new RegExp('^' + migrationHash + '  migrations/V20260717_001_order_multi_shipment\\.sql$', 'mu'))
})
