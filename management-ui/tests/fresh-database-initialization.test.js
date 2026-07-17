import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const repoRoot = path.resolve(import.meta.dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

test('fresh database initialization uses a current schema-only baseline', () => {
  const baselinePath = path.join(repoRoot, 'db-migrations/baseline/hive_schema_baseline_v2.sql')
  assert.equal(fs.existsSync(baselinePath), true, 'baseline v2 must exist')
  const baseline = fs.readFileSync(baselinePath, 'utf8')

  for (const table of [
    'tenant',
    'user',
    'sys_permission',
    'system_event',
    'enterprise_announcement',
    'approval_default_auditor',
    'equipment_device',
    'operation_log',
    'sales_order_note',
    'installation_task_installer'
  ]) {
    assert.match(baseline, new RegExp('CREATE TABLE `' + table + '`'), `baseline v2 must contain ${table}`)
  }
  assert.doesNotMatch(baseline, /^\s*(INSERT|UPDATE|DELETE|REPLACE)\s+/im)
})

test('routine migration rejects missing or incomplete baseline schemas', () => {
  const stateCheck = read('db-migrations/scripts/check-database-state.sh')
  for (const table of ['tenant', 'user', 'sys_permission', 'sys_role']) {
    assert.match(stateCheck, new RegExp(`['\"]${table}['\"]`))
  }
  assert.match(stateCheck, /FRESH_EMPTY/)
  assert.match(stateCheck, /READY/)
  assert.match(stateCheck, /INCOMPLETE/)
  assert.match(stateCheck, /history_count/)
  assert.match(stateCheck, /history_count[^\n]*-gt 0/)

  const migrate = read('scripts/migrate-db.sh')
  const stateIndex = migrate.indexOf('check-database-state.sh')
  const migrationIndex = migrate.indexOf('run-versioned-migrations.sh')
  assert.ok(stateIndex >= 0 && stateIndex < migrationIndex)
  assert.match(migrate, /ALLOW_FRESH_DATABASE=NO/)
})

test('fresh initialization is explicit, stops writes, and verifies the result', () => {
  const initializer = read('db-migrations/scripts/initialize-fresh-database.sh')
  const baseline = read('db-migrations/baseline/hive_schema_baseline_v2.sql')
  assert.match(initializer, /CONFIRM_FRESH_DATABASE_INITIALIZATION/)
  assert.match(initializer, /docker compose stop backend/)
  assert.match(initializer, /hive_schema_baseline_v2\.sql/)
  assert.match(initializer, /verify-online-schema\.sh/)
  assert.match(initializer, /HIVE_ALLOWED_TENANT_CODES/)
  assert.match(initializer, /TENANT_001/)

  const importer = read('db-migrations/scripts/import-baseline-to-shadow.sh')
  assert.match(importer, /hive_schema_baseline_v2\.sql/)
  assert.match(importer, /baseline\/hive_schema_baseline_v2/)
  assert.match(importer, /BASELINE_CUTOFF="\$\{BASELINE_CUTOFF:-migrations\/V20260716_001_operation_log_table\.sql\}"/)
  assert.doesNotMatch(importer, /BASELINE_CUTOFF="[^\n]*V20260717_001_order_multi_shipment\.sql/)

  const salesOrder = baseline.match(/CREATE TABLE `sales_order` \(([\s\S]*?)\n\) ENGINE=/u)?.[1] || ''
  assert.match(salesOrder, /`express_company`/u)
  assert.match(salesOrder, /`express_no`/u)
  assert.doesNotMatch(baseline, /CREATE TABLE `sales_order_shipment`/u)
  assert.match(read('db-migrations/migrations/V20260717_001_order_multi_shipment.sql'), /CREATE TABLE `sales_order_shipment`/u)
})

test('pending destructive order migration fails closed unless sales_order is empty', () => {
  const helperPath = path.join(repoRoot, 'db-migrations/scripts/check-order-multi-shipment-clean-launch.sh')
  assert.equal(fs.existsSync(helperPath), true, 'clean-launch runtime gate helper must exist')
  const helper = fs.readFileSync(helperPath, 'utf8')
  const migrate = read('scripts/migrate-db.sh')

  const gateIndex = migrate.indexOf('check-order-multi-shipment-clean-launch.sh')
  const runnerIndex = migrate.indexOf('run-versioned-migrations.sh')
  assert.ok(gateIndex >= 0 && gateIndex < runnerIndex, 'clean-launch gate must run before versioned migrations')
  assert.match(helper, /migrations\/V20260717_001_order_multi_shipment/)
  assert.match(helper, /schema_migration_history/)
  assert.match(helper, /sales_order/)
  assert.match(helper, /SELECT COUNT\(\*\)/)
  assert.match(helper, /formal cleanup process/i)
  assert.doesNotMatch(helper, /(?:SKIP|BYPASS|ALLOW)_CLEAN_LAUNCH/i)

  if (process.platform !== 'win32') {
    const script = [
      `source ${JSON.stringify(helperPath)}`,
      'assert_empty_sales_order 0',
      '! assert_empty_sales_order 1',
      '! assert_empty_sales_order invalid'
    ].join('; ')
    const result = spawnSync('bash', ['-c', script], { encoding: 'utf8' })
    assert.equal(result.status, 0, result.stderr || result.stdout)
  } else {
    assert.match(helper, /assert_empty_sales_order\(\)/)
    assert.match(helper, /0\)\s*return 0/)
    assert.match(helper, /\*\)[\s\S]*formal cleanup process[\s\S]*return 1/i)
  }
})

test('baseline-registered destructive migration bypasses row gate without re-execution', () => {
  const helper = read('db-migrations/scripts/check-order-multi-shipment-clean-launch.sh')
  const successCheck = helper.indexOf('SUCCESS')
  const orderCountCheck = helper.indexOf('SELECT COUNT(*) FROM sales_order')

  assert.ok(successCheck >= 0 && successCheck < orderCountCheck)
  assert.match(helper, /migration_state[\s\S]*SUCCESS[\s\S]*(?:exit|return) 0/)
})

test('baseline import seeds the active permission catalog before full verification', () => {
  const importer = read('db-migrations/scripts/import-baseline-to-shadow.sh')
  const initializer = read('db-migrations/scripts/initialize-fresh-database.sh')
  const seedReference = 'system_permission_catalog_v3.sql'
  const seedIndex = importer.indexOf(seedReference)
  const verifyIndex = importer.indexOf('verify-online-schema.sh')

  assert.ok(seedIndex >= 0, 'baseline importer must reference the active permission seed')
  assert.ok(verifyIndex > seedIndex, 'permission seed must be imported before full schema verification')
  assert.doesNotMatch(initializer, /mysql_root_db\s*<\s*"\$\{PERMISSION_SEED_FILE\}"/)
})
