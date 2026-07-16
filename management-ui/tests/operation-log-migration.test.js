import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const repoRoot = path.resolve(import.meta.dirname, '../..')
const migrationPath = 'migrations/V20260716_001_operation_log_table.sql'
const migrationFile = path.join(repoRoot, 'db-migrations', migrationPath)

test('online migration creates and verifies the operation_log table', () => {
  assert.equal(existsSync(migrationFile), true, 'operation_log migration must exist')
  const sql = readFileSync(migrationFile, 'utf8')
  assert.match(sql, /CREATE TABLE IF NOT EXISTS `operation_log`/)
  assert.match(sql, /`trace_id` varchar\(64\) NOT NULL/)
  assert.match(sql, /`duration_ms` bigint NOT NULL DEFAULT 0/)

  const manifest = readFileSync(path.join(repoRoot, 'db-migrations/migration_manifest.txt'), 'utf8')
  assert.match(manifest, new RegExp(`^${migrationPath.replaceAll('/', '\\/')}$`, 'm'))

  const expectedHash = createHash('sha256').update(readFileSync(migrationFile)).digest('hex')
  const checksums = readFileSync(path.join(repoRoot, 'db-migrations/migration_checksums.sha256'), 'utf8')
  assert.match(checksums, new RegExp(`^${expectedHash}  ${migrationPath.replaceAll('/', '\\/')}$`, 'm'))

  const verifier = readFileSync(path.join(repoRoot, 'db-migrations/scripts/verify-online-schema.sh'), 'utf8')
  assert.match(verifier, /table_name = 'operation_log'/)
})
