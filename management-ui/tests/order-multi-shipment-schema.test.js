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
  const releaseBaseline = read('db-migrations/baseline/hive_schema_baseline_v2.sql')
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
  const releaseSalesOrderBlock = tableBlock(releaseBaseline, 'sales_order')
  assert.doesNotMatch(salesOrderBlock, /`express_company`|`express_no`/u)
  assert.match(releaseSalesOrderBlock, /`express_company`/u)
  assert.match(releaseSalesOrderBlock, /`express_no`/u)
  assert.doesNotMatch(releaseBaseline, /CREATE TABLE `sales_order_shipment`/u)
  assert.match(migration, /CREATE TABLE `sales_order_shipment`/u)
  assert.match(migration, /DROP COLUMN `express_company`/u)
  assert.match(migration, /DROP COLUMN `express_no`/u)
  assert.match(manifest, /migrations\/V20260717_001_order_multi_shipment\.sql\s*$/mu)

  const migrationHash = createHash('sha256').update(read(migrationPath)).digest('hex')
  assert.match(checksums, new RegExp('^' + migrationHash + '  migrations/V20260717_001_order_multi_shipment\\.sql$', 'mu'))
})

test('immutable baseline defers the shipment migration to the versioned runner', () => {
  const importer = read('db-migrations/scripts/import-baseline-to-shadow.sh')
  const cutoff = 'migrations/V20260716_001_operation_log_table.sql'

  assert.match(importer, new RegExp('BASELINE_CUTOFF="\\$\\{BASELINE_CUTOFF:-' + cutoff.replace('.', '\\.') + '\\}"', 'u'))
  assert.match(importer, /if \[ "\$\{entry\}" = "\$\{BASELINE_CUTOFF\}" \]/u)
  assert.ok(
    importer.indexOf('Register checksummed baseline migration state') < importer.indexOf('run-versioned-migrations.sh'),
    'baseline migration history must be registered before the migration runner starts'
  )
})

test('release documentation gates the destructive clean-launch contract', () => {
  const documents = [
    'docs/superpowers/specs/2026-07-17-order-multi-shipment-design.md',
    'docs/migrations/unified-backend-migrations.md',
    'docs/deployment/unified-backend-deployment.md',
    'deploy/README.md'
  ].map(read)

  for (const document of documents) {
    assert.match(document, /V20260717_001/u)
    assert.match(document, /clean-launch destructive contract/iu)
    assert.match(document, /clear all (?:legacy|old) business data/iu)
    assert.match(document, /express_company/u)
    assert.match(document, /express_no/u)
    assert.match(document, /(?:no|does not|do not)[^\n]*(?:backfill|compatibility)/iu)
  }
})
