import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')
const migrationRoot = path.join(repoRoot, 'db-migrations')
const version = 'V20260715_002_installation_task_installer.sql'

function sha256(relativePath) {
  return createHash('sha256').update(fs.readFileSync(path.join(repoRoot, relativePath))).digest('hex')
}

test('historical installation migrations remain byte-for-byte immutable', () => {
  const protectedHistory = new Map([
    ['db-migrations/migrations/V20260705_004_installation_task_schema.sql', '9ab18545b9f6ef0142a943d54a27ac726a813c3361f4a81f4763f6222d98cd2d'],
    ['db-migrations/migrations/V20260707_001_installation_task_schema_convergence.sql', 'd9fc573187377b1f37d7aa5af23bdfbdf864b36563438a9482ef76a6fa7e32e6'],
    ['db-migrations/migrations/V20260710_001_installation_task_unique_key_repair.sql', '3daed4c0bc515e4bcca5aac13ec6a01f878b90267bf6f70db45ef59d6e3b818e']
  ])
  for (const [relativePath, expected] of protectedHistory) {
    assert.equal(sha256(relativePath), expected, `${path.basename(relativePath)} must not be edited`)
  }
})

test('new migration creates tenant-isolated ordered installer details and removes retired columns', () => {
  const migrationPath = path.join(migrationRoot, 'migrations', version)
  assert.ok(fs.existsSync(migrationPath), `${version} must be added as a new migration`)
  const sql = fs.readFileSync(migrationPath, 'utf8')

  for (const required of [
    'installation_task_installer',
    'tenant_code',
    'installation_task_id',
    'installer_name',
    'installer_phone',
    'sort_order',
    'create_time',
    'update_time',
    'idx_installation_task_installer_task',
    'uk_installation_task_installer_sort',
    'DROP COLUMN construction_personnel',
    'DROP COLUMN construction_phone'
  ]) {
    assert.ok(sql.includes(required), `migration must contain ${required}`)
  }
  assert.match(sql, /KEY `idx_installation_task_installer_task` \(`tenant_code`, `installation_task_id`\)/)
  assert.match(sql, /UNIQUE KEY `uk_installation_task_installer_sort` \(`tenant_code`, `installation_task_id`, `sort_order`\)/)
})

test('new migration is appended to the manifest and full checksum snapshot', () => {
  const relative = `migrations/${version}`
  const manifest = fs.readFileSync(path.join(migrationRoot, 'migration_manifest.txt'), 'utf8')
    .split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  assert.ok(manifest.includes(relative), 'manifest must contain the installer migration')
  assert.ok(manifest.indexOf(relative) < manifest.indexOf('migrations/V20260716_001_operation_log_table.sql'))

  const checksumLines = fs.readFileSync(path.join(migrationRoot, 'migration_checksums.sha256'), 'utf8')
    .split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  const newLine = checksumLines.find((line) => line.endsWith(`  ${relative}`))
  assert.ok(newLine, 'checksum snapshot must contain the new migration')
  assert.equal(newLine.slice(0, 64), sha256(`db-migrations/${relative}`))
})
