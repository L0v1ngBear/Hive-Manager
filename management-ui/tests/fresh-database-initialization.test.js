import assert from 'node:assert/strict'
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
  assert.match(initializer, /CONFIRM_FRESH_DATABASE_INITIALIZATION/)
  assert.match(initializer, /docker compose stop backend/)
  assert.match(initializer, /hive_schema_baseline_v2\.sql/)
  assert.match(initializer, /verify-online-schema\.sh/)
  assert.match(initializer, /HIVE_ALLOWED_TENANT_CODES/)
  assert.match(initializer, /TENANT_001/)

  const importer = read('db-migrations/scripts/import-baseline-to-shadow.sh')
  assert.match(importer, /hive_schema_baseline_v2\.sql/)
  assert.match(importer, /baseline\/hive_schema_baseline_v2/)
  assert.match(importer, /V20260716_001_operation_log_table\.sql/)
})
