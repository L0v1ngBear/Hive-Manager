import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const repoRoot = path.resolve(import.meta.dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

test('routine restart never recreates data services and never starts backend after migration failure', () => {
  const restart = read('deploy/scripts/restart.sh')
  assert.doesNotMatch(restart, /force-recreate[^\n]*(mysql|redis)|(mysql|redis)[^\n]*force-recreate/)
  assert.match(restart, /docker compose up -d --force-recreate --remove-orphans backend nginx/)
  assert.doesNotMatch(restart, /restore_on_failure[\s\S]*docker compose up[\s\S]*backend/)
})

test('database rebuild stops the unified backend before touching MySQL', () => {
  const rebuild = read('db-migrations/scripts/rebuild-mysql-from-baseline.sh')
  assert.match(rebuild, /for service in[^\n]*\sbackend(?:\s|;)/)
  const stopBackend = rebuild.indexOf('docker compose stop')
  const stopMysql = rebuild.indexOf('docker compose stop mysql')
  assert.ok(stopBackend >= 0 && stopBackend < stopMysql)
})

test('backup verification checks required Hive tables and guarded restore uses a shadow database', () => {
  const verifier = read('deploy/scripts/verify-latest-backup.sh')
  for (const table of ['tenant', 'user', 'sys_permission', 'schema_migration_history']) {
    assert.match(verifier, new RegExp(table))
  }

  const restorePath = path.join(repoRoot, 'db-migrations/scripts/restore-verified-backup.sh')
  assert.equal(fs.existsSync(restorePath), true)
  const restore = fs.readFileSync(restorePath, 'utf8')
  assert.match(restore, /CONFIRM_DATABASE_RESTORE/)
  assert.match(restore, /hive_restore_verify/)
  assert.match(restore, /verify-online-schema\.sh/)
  assert.match(restore, /docker compose stop backend/)
})

test('release synchronization is allowlisted and preserves runtime-owned paths', () => {
  const syncPath = path.join(repoRoot, 'deploy/scripts/sync-release-files.sh')
  assert.equal(fs.existsSync(syncPath), true)
  const sync = fs.readFileSync(syncPath, 'utf8')
  assert.match(sync, /RELEASE_SOURCE_DIR/)
  assert.match(sync, /backend\/hive-backend\.jar/)
  assert.match(sync, /management-ui\/dist/)
  for (const runtimePath of ['.env', 'mysql/data', 'redis/data', 'nginx/certs', 'uploads', 'backups']) {
    assert.match(sync, new RegExp(runtimePath.replace('/', '\\/')))
  }
})
