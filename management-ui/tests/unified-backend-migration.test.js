import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')
const migrationRoot = path.join(repoRoot, 'db-migrations')
const manifestPath = path.join(migrationRoot, 'migration_manifest.txt')
const checksumPath = path.join(migrationRoot, 'migration_checksums.sha256')

const sha256 = (file) => createHash('sha256').update(fs.readFileSync(file)).digest('hex')
const posix = (value) => value.replaceAll(path.sep, '/')

function manifestEntries() {
  return fs.readFileSync(manifestPath, 'utf8')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('#'))
}

function checksumEntries() {
  return new Map(fs.readFileSync(checksumPath, 'utf8')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const match = line.match(/^([a-f0-9]{64})\s{2}(.+)$/)
      assert.ok(match, `invalid checksum line: ${line}`)
      return [match[2], match[1]]
    }))
}

test('manifest exactly matches versioned migrations and checksums protect current bootstrap assets', () => {
  const manifest = manifestEntries()
  const disk = fs.readdirSync(path.join(migrationRoot, 'migrations'))
    .filter((name) => /^V\d{8}_\d{3}_.+\.sql$/.test(name))
    .map((name) => `migrations/${name}`)
    .sort()

  assert.deepEqual(manifest, disk, 'manifest must list every versioned SQL exactly once and in order')

  const checksums = checksumEntries()
  const protectedAssets = [
    ...disk,
    'baseline/hive_schema_baseline_v2.sql',
    'seeds/system_permission_catalog_v3.sql'
  ].sort()
  assert.deepEqual([...checksums.keys()].sort(), protectedAssets, 'checksum snapshot must cover migrations and current bootstrap assets')
  for (const relativePath of protectedAssets) {
    assert.equal(
      sha256(path.join(migrationRoot, ...relativePath.split('/'))),
      checksums.get(relativePath),
      `protected database asset changed without refreshing the checksum catalog: ${relativePath}`
    )
  }

  const protectedHistory = new Map([
    ['migrations/V20260530_001_second_tenant_seed.sql', 'b9da086e1b5b533b7ceedc629115d26612283b1fadcadcc3c6cbaf23114d5ace'],
    ['migrations/V20260705_004_installation_task_schema.sql', '9ab18545b9f6ef0142a943d54a27ac726a813c3361f4a81f4763f6222d98cd2d'],
    ['migrations/V20260713_003_permission_catalog_v3.sql', 'c06b1b741db4349f10ff7226df4cd424dbbad10491ecc5b6d1fc3609bcb0ea8a'],
    ['migrations/V20260714_001_permission_relation_convergence.sql', '19a51dab5fe754d37f5764a4d07e9b76b4081cba06e86eaeedb81d879e0673f6']
  ])
  for (const [relativePath, expected] of protectedHistory) {
    assert.equal(sha256(path.join(migrationRoot, ...relativePath.split('/'))), expected)
  }
})

test('repository exposes one migration entry and never executes application resource SQL', () => {
  const entrypoint = path.join(repoRoot, 'scripts', 'migrate-db.sh')
  assert.ok(fs.existsSync(entrypoint), 'scripts/migrate-db.sh must be the repository migration entrypoint')
  assert.ok(
    !fs.existsSync(path.join(repoRoot, 'management', 'src', 'main', 'resources', 'sql')),
    'retired application resource SQL must be removed instead of acting as a second migration source'
  )

  const entrypointSource = fs.readFileSync(entrypoint, 'utf8')
  assert.match(entrypointSource, /db-migrations\/scripts\/run-versioned-migrations\.sh/)
  assert.match(entrypointSource, /db-migrations\/migration_manifest\.txt/)
  for (const helper of [
    'normalize-env.sh',
    'verify-latest-backup.sh'
  ]) {
    assert.ok(fs.existsSync(path.join(repoRoot, 'scripts', helper)), `missing migration helper: ${helper}`)
  }
  for (const retiredHelper of [
    'verify-order-information-channel-artifacts.sh',
    'diagnose-migration-drift.sh'
  ]) {
    assert.equal(fs.existsSync(path.join(repoRoot, 'scripts', retiredHelper)), false)
  }

  const scriptFiles = []
  for (const root of [path.join(repoRoot, 'scripts'), path.join(migrationRoot, 'scripts')]) {
    for (const name of fs.readdirSync(root)) {
      if (name.endsWith('.sh')) scriptFiles.push(path.join(root, name))
    }
  }
  const scriptSource = scriptFiles.map((file) => fs.readFileSync(file, 'utf8')).join('\n')
  assert.doesNotMatch(scriptSource, /(?:management\/)?src\/main\/resources\/sql|classpath:sql\//)

  const directRunners = scriptFiles
    .filter((file) => path.dirname(file) === path.join(repoRoot, 'scripts'))
    .filter((file) => fs.readFileSync(file, 'utf8').includes('run-versioned-migrations.sh'))
    .map((file) => posix(path.relative(repoRoot, file)))
  assert.deepEqual(directRunners, ['scripts/migrate-db.sh'])
})
