import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const repoRoot = path.resolve(uiRoot, '..')

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

test('release integrity verifies every artifact family and source commit', () => {
  const script = read('deploy/scripts/verify-release-integrity.sh')
  for (const field of [
    'BackendJarSha256',
    'ManagementUiSha256',
    'ManagementUiFileCount',
    'ManagementUiIndexSha256',
    'MigrationManifestSha256',
    'MigrationChecksumsSha256',
    'MigrationCount',
    'SourceGitCommit',
    'ReleasePackageGitCommit',
    'ManagementUiSourceGitCommit',
    'MiniProgramSourceGitCommit'
  ]) {
    assert.ok(script.includes(field), `release integrity must verify ${field}`)
  }
  assert.match(script, /git\s+-C\s+"\$\{git_root\}"\s+cat-file\s+-e/u)
  assert.match(script, /MINI_PROGRAM_SOURCE_REPOSITORY_ROOT/u)
  assert.match(script, /management-ui\/dist/u)
  assert.match(script, /migration_checksums\.sha256/u)
  assert.match(script, /customer:page/u)
  assert.match(script, /table:export/u)
})

test('upload package verifier rejects secrets, certificates, and runtime data', () => {
  const script = read('deploy/scripts/verify-upload-package.sh')
  for (const forbidden of [
    '.env',
    '*.key',
    '*.pem',
    'mysql/data',
    'redis/data',
    'rabbitmq/data',
    'uploads',
    'smoke-reports',
    'release-snapshots'
  ]) {
    assert.ok(script.includes(forbidden), `upload verifier must reject ${forbidden}`)
  }
})

test('runtime health and upload cleanliness remain separate gates', () => {
  const runtimeHealth = read('deploy/scripts/check-deploy-health.sh')
  const uploadHealth = read('deploy/scripts/verify-upload-package.sh')
  assert.match(runtimeHealth, /require_file \.env/u)
  assert.match(runtimeHealth, /nginx\/certs\/hellohive\.top\.key/u)
  assert.doesNotMatch(uploadHealth, /require_file \.env/u)
})
