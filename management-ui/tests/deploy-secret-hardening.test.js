import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { spawnSync } from 'node:child_process'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const repoRoot = path.resolve(uiRoot, '..')
const deployRoot = path.resolve(uiRoot, '..', 'deploy')

function read(relativePath) {
  return fs.readFileSync(path.join(deployRoot, relativePath), 'utf8')
}

function readRepo(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

const deployHealth = read('scripts/check-deploy-health.sh')
const deployReadme = read('README.md')
const composeSource = read('docker-compose.yml')
const envExample = read('.env.example')
const runtimeConfig = readRepo('management/src/main/resources/application.yaml')
const deploymentDoc = readRepo('docs/deployment/unified-backend-deployment.md')

assert.ok(
  deployReadme.includes('test -f .env || cp .env.example .env'),
  'deployment instructions must never overwrite an existing server-owned .env'
)
assert.doesNotMatch(
  deployReadme,
  /^cp \.env\.example \.env$/m,
  'deployment instructions must not contain an unconditional .env copy command'
)

assert.ok(
  deployHealth.includes('RESPONSE_ENCRYPT_KEY'),
  'deploy health check must validate RESPONSE_ENCRYPT_KEY'
)
assert.ok(
  /for weak_key in[\s\S]*RESPONSE_ENCRYPT_KEY/.test(deployHealth),
  'RESPONSE_ENCRYPT_KEY must be included in weak default rejection loop'
)
assert.ok(
  deployHealth.includes('sygav9Iec4kZiRvivnwSVe3iWq66cTCleo8gr3qL2GyXTcQHXJ1E57ZqfhqfIyWp70Imy0rJ7ZkS5SI4T0asRQ=='),
  'deploy health check must reject the public development response encryption key'
)
assert.ok(
  deployHealth.includes('CHANGE_ME*') || deployHealth.includes('CHANGE_ME_RESPONSE_ENCRYPT_KEY'),
  'deploy health check must reject .env.example placeholder secrets'
)
assert.ok(
  deployHealth.includes('(^|,)[[:space:]]*\\*[[:space:]]*(,|$)'),
  'deploy health check must reject bare wildcard CORS origins even inside comma-separated lists'
)
assert.ok(
  /NOTIFICATION_SMS_ENABLED[\s\S]*NOTIFICATION_SMS_ACCESS_KEY/.test(deployHealth),
  'deploy health check must require SMS access key when SMS sending is enabled'
)
assert.ok(
  /NOTIFICATION_SMS_ENABLED[\s\S]*NOTIFICATION_SMS_ACCESS_SECRET/.test(deployHealth),
  'deploy health check must require SMS access secret when SMS sending is enabled'
)
assert.ok(
  /NOTIFICATION_SMS_ENABLED[\s\S]*NOTIFICATION_SMS_SIGN_NAME/.test(deployHealth),
  'deploy health check must require SMS sign name when SMS sending is enabled'
)
assert.ok(
  /NOTIFICATION_SMS_ENABLED[\s\S]*NOTIFICATION_SMS_TEMPLATE_CODE/.test(deployHealth),
  'deploy health check must require SMS template code when SMS sending is enabled'
)

for (const source of [runtimeConfig, composeSource, envExample, deployHealth, deploymentDoc]) {
  assert.doesNotMatch(source, new RegExp(['KUAIDI', '100'].join('')), 'retired logistics variables must be absent')
}

assert.match(runtimeConfig, /provider: \$\{LOGISTICS_PROVIDER:apispace\}/)
assert.match(runtimeConfig, /enabled: \$\{APISPACE_LOGISTICS_ENABLED:false\}/)
assert.match(runtimeConfig, /token: \$\{APISPACE_LOGISTICS_TOKEN:\}/)
assert.match(runtimeConfig, /connect-timeout: \$\{APISPACE_LOGISTICS_CONNECT_TIMEOUT:5s\}/)
assert.match(runtimeConfig, /request-timeout: \$\{APISPACE_LOGISTICS_REQUEST_TIMEOUT:10s\}/)
assert.match(runtimeConfig, /provider: \$\{FILE_STORAGE_PROVIDER:local\}/)
assert.match(runtimeConfig, /enabled: \$\{ALIYUN_OSS_ENABLED:false\}/)

assert.match(composeSource, /LOGISTICS_PROVIDER: \$\{LOGISTICS_PROVIDER:-apispace\}/)
assert.match(composeSource, /APISPACE_LOGISTICS_ENABLED: \$\{APISPACE_LOGISTICS_ENABLED:-false\}/)
assert.match(composeSource, /APISPACE_LOGISTICS_TOKEN: \$\{APISPACE_LOGISTICS_TOKEN:-\}/)
assert.match(composeSource, /APISPACE_LOGISTICS_CONNECT_TIMEOUT: \$\{APISPACE_LOGISTICS_CONNECT_TIMEOUT:-5s\}/)
assert.match(composeSource, /APISPACE_LOGISTICS_REQUEST_TIMEOUT: \$\{APISPACE_LOGISTICS_REQUEST_TIMEOUT:-10s\}/)
assert.match(composeSource, /FILE_STORAGE_PROVIDER: \$\{FILE_STORAGE_PROVIDER:-local\}/)
assert.match(composeSource, /ALIYUN_OSS_ENABLED: \$\{ALIYUN_OSS_ENABLED:-false\}/)

assert.match(envExample, /^LOGISTICS_PROVIDER=apispace$/m)
assert.match(envExample, /^APISPACE_LOGISTICS_ENABLED=false$/m)
assert.match(envExample, /^APISPACE_LOGISTICS_TOKEN=$/m)
assert.match(envExample, /^APISPACE_LOGISTICS_CONNECT_TIMEOUT=5s$/m)
assert.match(envExample, /^APISPACE_LOGISTICS_REQUEST_TIMEOUT=10s$/m)
assert.match(envExample, /^FILE_STORAGE_PROVIDER=local$/m)
assert.match(envExample, /^ALIYUN_OSS_ENABLED=false$/m)
assert.doesNotMatch(envExample, /APISPACE_LOGISTICS_TOKEN=\S+/)

for (const source of [runtimeConfig, composeSource, envExample]) {
  for (const line of source.split(/\r?\n/).filter((item) => item.includes('APISPACE_LOGISTICS_TOKEN'))) {
    assert.match(
      line,
      /^(?:\s*token:\s*\$\{APISPACE_LOGISTICS_TOKEN:\}|\s*APISPACE_LOGISTICS_TOKEN:\s*\$\{APISPACE_LOGISTICS_TOKEN:-\}|APISPACE_LOGISTICS_TOKEN=)$/
    )
  }
}

assert.match(
  deployHealth,
  /if env_true APISPACE_LOGISTICS_ENABLED[\s\S]*for key in APISPACE_LOGISTICS_TOKEN/
)
assert.match(
  deployHealth,
  /FILE_STORAGE_PROVIDER[\s\S]*aliyun-oss[\s\S]*ALIYUN_OSS_ENABLED[\s\S]*ALIYUN_OSS_ENDPOINT[\s\S]*ALIYUN_OSS_BUCKET[\s\S]*ALIYUN_OSS_ACCESS_KEY_ID[\s\S]*ALIYUN_OSS_ACCESS_KEY_SECRET/
)

for (const variable of [
  'ALIYUN_OSS_ENABLED',
  'ALIYUN_OSS_ENDPOINT',
  'ALIYUN_OSS_BUCKET',
  'ALIYUN_OSS_ACCESS_KEY_ID',
  'ALIYUN_OSS_ACCESS_KEY_SECRET',
  'ALIYUN_OSS_PUBLIC_BASE_URL',
  'ALIYUN_OSS_PATH_PREFIX',
  'ALIYUN_OSS_MAX_FILE_SIZE_MB',
  'ALIYUN_OSS_ALLOWED_EXTENSIONS',
  'ALIYUN_OSS_ALLOWED_CONTENT_TYPES'
]) {
  assert.match(composeSource, new RegExp(`${variable}: \\$\\{${variable}:-`))
  assert.match(envExample, new RegExp(`^${variable}=`, 'm'))
  assert.match(runtimeConfig, new RegExp(`\\$\\{${variable}:`))
}

console.log('deploy secret hardening checks passed')

function canRunBash(command) {
  if (!command) {
    return false
  }
  if (path.isAbsolute(command) && !fs.existsSync(command)) {
    return false
  }
  const result = spawnSync(command, ['-c', 'exit 0'], { stdio: 'ignore', windowsHide: true })
  return !result.error && result.status === 0
}

function resolveBash() {
  const candidates = []
  const override = process.env.HIVE_TEST_BASH?.trim()
  if (override) {
    candidates.push(override)
  }
  candidates.push('bash')

  if (process.platform === 'win32') {
    for (const programFiles of [process.env.ProgramFiles, process.env['ProgramFiles(x86)']]) {
      if (programFiles) {
        candidates.push(path.join(programFiles, 'Git', 'bin', 'bash.exe'))
      }
    }

    const gitLookup = spawnSync('where.exe', ['git'], { encoding: 'utf8', windowsHide: true })
    if (!gitLookup.error && gitLookup.status === 0) {
      for (const gitPath of gitLookup.stdout.split(/\r?\n/).map((item) => item.trim()).filter(Boolean)) {
        candidates.push(path.join(path.dirname(path.dirname(gitPath)), 'bin', 'bash.exe'))
      }
    }
  }

  for (const candidate of [...new Set(candidates)]) {
    if (canRunBash(candidate)) {
      return candidate
    }
  }
  throw new Error('Bash is required for deploy health behavioral tests; set HIVE_TEST_BASH to a runnable Bash executable')
}

const bashCommand = resolveBash()

function createHealthFixture(overrides = {}) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'hive-deploy-health-'))
  const directories = [
    'backend',
    'management-ui/dist',
    'nginx/conf.d',
    'nginx/certs',
    'db-migrations/baseline',
    'db-migrations/seeds',
    'db-migrations/migrations',
    'scripts'
  ]
  for (const directory of directories) {
    fs.mkdirSync(path.join(root, directory), { recursive: true })
  }

  fs.copyFileSync(path.join(deployRoot, 'scripts/check-deploy-health.sh'), path.join(root, 'scripts/check-deploy-health.sh'))
  fs.copyFileSync(path.join(deployRoot, 'scripts/common.sh'), path.join(root, 'scripts/common.sh'))
  fs.writeFileSync(path.join(root, 'backend/hive-backend.jar'), '')
  fs.writeFileSync(path.join(root, 'management-ui/dist/index.html'), '<!doctype html>')
  fs.writeFileSync(path.join(root, 'nginx/conf.d/hive.conf'), 'proxy_pass http://backend:8080;\nlocation /api/ { }\n')
  fs.writeFileSync(path.join(root, 'nginx/certs/hellohive.top.pem'), '')
  fs.writeFileSync(path.join(root, 'nginx/certs/hellohive.top.key'), '')
  fs.writeFileSync(path.join(root, 'docker-compose.yml'), '  backend:\n    container_name: hive-backend\n')
  fs.writeFileSync(path.join(root, 'scripts/migrate-db.sh'), '')
  fs.writeFileSync(path.join(root, 'scripts/inspect-backend-artifact.sh'), 'exit 0\n')
  for (const file of [
    'migration_manifest.txt',
    'baseline/hive_schema_baseline_v2.sql',
    'seeds/system_permission_catalog_v3.sql',
    'migrations/V20260710_001_installation_task_unique_key_repair.sql',
    'migrations/V20260710_003_builtin_role_permission_matrix.sql',
    'migrations/V20260710_004_order_role_status_scope.sql',
    'migrations/V20260715_001_order_notes_and_material_approval.sql'
  ]) {
    fs.writeFileSync(path.join(root, 'db-migrations', file), '')
  }
  const checksumFile = path.join(root, 'db-migrations/checksum-fixture.txt')
  fs.writeFileSync(checksumFile, 'fixture\n')
  const checksum = createHash('sha256').update('fixture\n').digest('hex')
  fs.writeFileSync(path.join(root, 'db-migrations/migration_checksums.sha256'), `${checksum}  checksum-fixture.txt\n`)

  const env = {
    MYSQL_ROOT_PASSWORD: 'fixture-root-password',
    DB_APP_USERNAME: 'fixture-app',
    DB_APP_PASSWORD: 'fixture-app-password',
    AUTH_TOKEN_SECRET: 'fixture-auth-secret',
    RESPONSE_ENCRYPT_KEY: 'fixture-response-key',
    PRIVACY_HASH_SECRET: 'fixture-privacy-secret',
    EMPLOYEE_DEFAULT_PASSWORD: 'fixture-employee-password',
    TENANT_OWNER_DEFAULT_PASSWORD: 'fixture-owner-password',
    CORS_ALLOWED_ORIGINS: 'https://example.test',
    LOGISTICS_PROVIDER: 'apispace',
    APISPACE_LOGISTICS_ENABLED: 'false',
    APISPACE_LOGISTICS_TOKEN: '',
    FILE_STORAGE_PROVIDER: 'local',
    ALIYUN_OSS_ENABLED: 'false',
    ALIYUN_OSS_ENDPOINT: '',
    ALIYUN_OSS_BUCKET: '',
    ALIYUN_OSS_ACCESS_KEY_ID: '',
    ALIYUN_OSS_ACCESS_KEY_SECRET: '',
    ...overrides
  }
  fs.writeFileSync(path.join(root, '.env'), `${Object.entries(env).map(([key, value]) => `${key}=${value}`).join('\n')}\n`)
  return root
}

function runHealth(overrides) {
  const root = createHealthFixture(overrides)
  const bashRoot = root.replace(/^([A-Za-z]):[\\/]/, (_, drive) => `/${drive.toLowerCase()}/`).replaceAll('\\', '/')
  try {
    return spawnSync(bashCommand, [path.join(root, 'scripts/check-deploy-health.sh')], {
      cwd: root,
      env: { ...process.env, HIVE_RELEASE_ROOT: bashRoot, ALLOW_MISSING_DOCKER: 'YES' },
      encoding: 'utf8'
    })
  } finally {
    fs.rmSync(root, { recursive: true, force: true })
  }
}

test('deploy health rejects a whitespace-only APISpace token', () => {
  const result = runHealth({
    APISPACE_LOGISTICS_ENABLED: 'true',
    APISPACE_LOGISTICS_TOKEN: ' \t '
  })
  assert.equal(result.error, undefined)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /APISpace logistics is enabled but APISPACE_LOGISTICS_TOKEN is empty/)
})

test('deploy health normalizes the storage provider before applying the OSS gate', () => {
  const result = runHealth({
    FILE_STORAGE_PROVIDER: ' ALIYUN-OSS ',
    ALIYUN_OSS_ENABLED: 'true'
  })
  assert.equal(result.error, undefined)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /aliyun-oss storage requires ALIYUN_OSS_ENDPOINT/)
})

test('deploy health rejects unknown logistics providers after normalization', () => {
  const result = runHealth({ LOGISTICS_PROVIDER: ' unsupported-provider ' })
  assert.equal(result.error, undefined)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /unsupported logistics provider: unsupported-provider/)
})

test('deploy health rejects unknown file storage providers after normalization', () => {
  const result = runHealth({ FILE_STORAGE_PROVIDER: ' unsupported-provider ' })
  assert.equal(result.error, undefined)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /unsupported file storage provider: unsupported-provider/)
})
