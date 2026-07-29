import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import assert from 'node:assert/strict'
import { fileURLToPath } from 'node:url'

const migrationName = 'V20260713_003_permission_catalog_v3.sql'
const migrationRelativePath = `db-migrations/migrations/${migrationName}`
const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

test('permission catalog v3 migration is present and complete', () => {
  const migrationPath = path.join(repoRoot, migrationRelativePath)
  assert.ok(fs.existsSync(migrationPath), `${migrationName} must exist`)
  const sql = fs.readFileSync(migrationPath, 'utf8')

  for (const fragment of [
    "'sys_permission', 'module_code'",
    "'sys_permission', 'assignable'",
    "'sys_permission', 'status'",
    "'user', 'permission_version'",
    "'user', 'auth_version'",
    'CREATE TABLE IF NOT EXISTS `sys_permission_catalog`',
    'CREATE TABLE IF NOT EXISTS `order_responsibility`',
    "'order:status:pending-confirm:view'",
    "'order:status:pending-confirm:advance'",
    "'order:scope:sales:self'",
    "'order:scope:production:department'",
    "'order:scope:installation:department'",
    "'employee:permission:manage'",
    "'approval:auditor:setting'"
  ]) {
    assert.ok(sql.includes(fragment), `migration must contain ${fragment}`)
  }

  assert.doesNotMatch(sql, /permission_v3_code_map/i)
  assert.doesNotMatch(sql, /FROM\s+sys_user_permission\s+source/i)
  assert.doesNotMatch(sql, /FROM\s+sys_role_permission\s+rp[\s\S]+old_code/i)
  assert.match(sql, /UPDATE\s+`?sys_permission`?[\s\S]+SET[\s\S]+`?is_deleted`?\s*=\s*1/i)
  const catalogSeed = sql.match(/INSERT INTO permission_v3_catalog[\s\S]+?;\r?\n\r?\nDELIMITER/)
  assert.ok(catalogSeed, 'migration must have a bounded permission_v3_catalog seed')
  assert.doesNotMatch(catalogSeed[0], /^\('[^']*:\*'/m)
})

test('permission catalog v3 remains ordered once in the migration manifest', () => {
  const entries = read('db-migrations/migration_manifest.txt')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('#'))

  const expected = `migrations/${migrationName}`
  assert.equal(entries.filter((entry) => entry === expected).length, 1)
  assert.ok(
    entries.indexOf(expected) < entries.indexOf('migrations/V20260714_001_permission_relation_convergence.sql'),
    'permission V3 must remain before its relation convergence migration'
  )
})

test('online schema verifier enforces permission v3 invariants', () => {
  const verifier = read('db-migrations/scripts/verify-online-schema.sh')
  for (const fragment of [
    migrationName,
    'permission_version',
    'auth_version',
    'sys_permission_catalog',
    'assignable wildcard permissions remain',
    'orphan user permission relations remain',
    'invalid role permission relations remain',
    'invalid user permission relations remain',
    'invalid user permission effects remain'
  ]) {
    assert.ok(verifier.includes(fragment), `schema verifier must contain ${fragment}`)
  }
})

test('permission catalog and built-in matrix reference only valid leaves', () => {
  const sql = read(migrationRelativePath)
  const catalogSeed = sql.match(/INSERT INTO permission_v3_catalog[\s\S]+?;\r?\n\r?\nDELIMITER/)
  assert.ok(catalogSeed)

  const definitions = [...catalogSeed[0].matchAll(
    /^\('([^']+)',\s*(?:NULL|'[^']+'),\s*'[^']+',\s*(\d+),\s*(\d+),/gm
  )].map((match) => ({ code: match[1], type: Number(match[2]), assignable: Number(match[3]) }))
  const codes = new Set(definitions.map((item) => item.code))
  assert.equal(codes.size, definitions.length, 'permission codes must be unique')
  assert.ok(definitions.filter((item) => item.type === 1).every((item) => item.assignable === 0))

  const matrixSeed = sql.match(/INSERT IGNORE INTO builtin_role_permission_v3[\s\S]+?;\r?\n\r?\nUPDATE sys_role_permission/)
  assert.ok(matrixSeed)
  const matrixCodes = [...matrixSeed[0].matchAll(/^\('[^']+','([^']+)'\)/gm)].map((match) => match[1])
  for (const code of matrixCodes) {
    const definition = definitions.find((item) => item.code === code)
    assert.ok(definition, `built-in role permission must exist: ${code}`)
    assert.equal(definition.assignable, 1, `built-in role permission must be assignable: ${code}`)
  }
})

test('fresh baseline imports execute migrations newer than the baseline cutoff', () => {
  const importer = read('db-migrations/scripts/import-baseline-to-shadow.sh')
  assert.ok(
    importer.includes('BASELINE_CUTOFF') &&
      importer.includes('migrations/V20260716_001_operation_log_table.sql'),
    'baseline importer must declare the current checksummed baseline cutoff'
  )
  assert.ok(
    importer.includes('run-versioned-migrations.sh') && importer.includes('RUN_PREFLIGHT=NO'),
    'baseline importer must execute manifest entries newer than the baseline cutoff'
  )
  assert.ok(
    importer.includes('baseline_cutoff_reached'),
    'baseline importer must stop pre-registering migrations after the declared cutoff'
  )
})

test('deploy health requires the permission v3 migration', () => {
  const health = read('deploy/scripts/check-deploy-health.sh')
  assert.ok(health.includes('db-migrations/seeds/system_permission_catalog_v3.sql'))
  assert.ok(health.includes('db-migrations/migration_checksums.sha256'))
})
