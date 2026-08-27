import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const repositoryRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')
const composeSource = fs.readFileSync(path.join(repositoryRoot, 'deploy/docker-compose.yml'), 'utf8')

test('Compose stamps hive-net with the desired subnet configuration hash', () => {
  assert.match(
    composeSource,
    /com\.hive\.network\.config-hash:\s*["']hive-net-v1:\$\{HIVE_DOCKER_SUBNET:-172\.30\.0\.0\/24\}["']/
  )
})

function canRunBash(command) {
  if (!command || (path.isAbsolute(command) && !fs.existsSync(command))) return false
  const result = spawnSync(command, ['-c', 'exit 0'], { stdio: 'ignore', windowsHide: true })
  return !result.error && result.status === 0
}

function resolveBash() {
  const candidates = [process.env.HIVE_TEST_BASH?.trim(), 'bash']
  if (process.platform === 'win32') {
    for (const programFiles of [process.env.ProgramFiles, process.env['ProgramFiles(x86)']]) {
      if (programFiles) candidates.push(path.join(programFiles, 'Git', 'bin', 'bash.exe'))
    }
    const lookup = spawnSync('where.exe', ['git'], { encoding: 'utf8', windowsHide: true })
    if (!lookup.error && lookup.status === 0) {
      for (const gitPath of lookup.stdout.split(/\r?\n/).map((item) => item.trim()).filter(Boolean)) {
        candidates.push(path.join(path.dirname(path.dirname(gitPath)), 'bin', 'bash.exe'))
      }
    }
  }
  const resolved = [...new Set(candidates.filter(Boolean))].find(canRunBash)
  if (!resolved) throw new Error('Bash is required for restart network migration tests')
  return resolved
}

const bash = resolveBash()

function toBashPath(value) {
  return value
    .replace(/^([A-Za-z]):[\\/]/, (_, drive) => `/${drive.toLowerCase()}/`)
    .replaceAll('\\', '/')
}

function createFixture() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'hive-network-migration-'))
  const scripts = path.join(root, 'scripts')
  const bin = path.join(root, 'bin')
  fs.mkdirSync(scripts, { recursive: true })
  fs.mkdirSync(bin, { recursive: true })
  fs.mkdirSync(path.join(root, 'management-ui/dist'), { recursive: true })

  for (const name of ['restart.sh', 'common.sh']) {
    fs.copyFileSync(path.join(repositoryRoot, 'deploy/scripts', name), path.join(scripts, name))
  }
  for (const name of [
    'normalize-env.sh',
    'prune-management-ui.sh',
    'prune-db-migrations.sh',
    'check-deploy-health.sh',
    'migrate-db.sh',
    'verify-release-integrity.sh',
    'smoke-test.sh'
  ]) {
    fs.writeFileSync(path.join(scripts, name), '#!/bin/bash\nexit 0\n')
  }

  fs.writeFileSync(path.join(root, '.env'), [
    'HIVE_DOCKER_SUBNET=172.30.0.0/24',
    'OPERATION_LOG_QUEUE_TYPE=memory',
    'XXL_JOB_ENABLED=false',
    ''
  ].join('\n'))
  fs.writeFileSync(path.join(root, 'docker-compose.yml'), 'name: fixture-project\n')
  fs.writeFileSync(path.join(root, 'management-ui/dist/index.html'), '<!doctype html>')

  const docker = `#!/bin/bash
set -eu
printf '%s\\n' "$*" >> "$MOCK_DOCKER_LOG"
phase="$(cat "$MOCK_DOCKER_STATE")"

if [ "$1" = "compose" ] && [ "\${2:-}" = "config" ]; then
  cat <<'JSON'
{
  "name": "fixture-project",
  "networks": {
    "hive-net": {
      "name": "fixture-hive-net"
    }
  }
}
JSON
  exit 0
fi

if [ "$1" = "network" ] && [ "\${2:-}" = "inspect" ]; then
  if [ "$MOCK_NETWORK_SCENARIO" = "inspection-error" ] && [ "$phase" = "before" ]; then exit 1; fi
  if [ "$MOCK_NETWORK_SCENARIO" = "absent" ] && [ "$phase" = "before" ]; then exit 1; fi
  if [ "$MOCK_NETWORK_SCENARIO" = "post-mismatch" ] && [ "$phase" = "before" ]; then exit 1; fi
  args="$*"
  if [[ "$args" == *"IPAM.Config"* ]]; then
    if [ "$phase" = "after" ] && [ "$MOCK_NETWORK_SCENARIO" = "post-mismatch" ]; then echo '172.31.0.0/24';
    elif [ "$phase" = "before" ] && [[ "$MOCK_NETWORK_SCENARIO" =~ ^(mismatch|down-error|up-error)$ ]]; then echo '172.29.0.0/24';
    else echo '172.30.0.0/24'; fi
  elif [[ "$args" == *"com.hive.network.config-hash"* ]]; then
    if [ "$phase" = "before" ] && [ "$MOCK_NETWORK_SCENARIO" = "legacy" ]; then echo '';
    elif [ "$phase" = "before" ] && [[ "$MOCK_NETWORK_SCENARIO" =~ ^(mismatch|down-error|up-error)$ ]]; then echo 'hive-net-v0:172.29.0.0/24';
    else echo 'hive-net-v1:172.30.0.0/24'; fi
  elif [[ "$args" == *".Containers"* ]] && [ "$phase" = "before" ] && [ "$MOCK_NETWORK_SCENARIO" = "foreign" ]; then
    echo 'foreign-container-id'
  fi
  exit 0
fi

if [ "$1" = "network" ] && [ "\${2:-}" = "ls" ]; then
  if [ "$phase" = "before" ] && [[ "$MOCK_NETWORK_SCENARIO" =~ ^(absent|post-mismatch)$ ]]; then exit 0; fi
  echo 'fixture-hive-net'
  exit 0
fi

if [ "$1" = "inspect" ]; then
  if [ "\${@: -1}" = "foreign-container-id" ]; then echo 'foreign-project'; else echo 'healthy'; fi
  exit 0
fi

if [ "$1" = "compose" ] && [ "\${2:-}" = "down" ]; then
  if [ "$MOCK_NETWORK_SCENARIO" = "down-error" ]; then exit 1; fi
  echo down > "$MOCK_DOCKER_STATE"
  exit 0
fi
if [ "$1" = "compose" ] && [ "\${2:-}" = "up" ]; then
  if [ "$MOCK_NETWORK_SCENARIO" = "up-error" ]; then exit 1; fi
  echo after > "$MOCK_DOCKER_STATE"
  exit 0
fi
if [ "$1" = "compose" ] && [ "\${2:-}" = "ps" ]; then exit 0; fi
exit 0
`
  const dockerPath = path.join(bin, 'docker')
  fs.writeFileSync(dockerPath, docker)
  fs.chmodSync(dockerPath, 0o755)
  return { root, bin }
}

function runRestart(scenario) {
  const fixture = createFixture()
  const log = path.join(fixture.root, 'docker.log')
  const state = path.join(fixture.root, 'docker.state')
  fs.writeFileSync(log, '')
  fs.writeFileSync(state, 'before\n')
  try {
    const result = spawnSync(bash, [path.join(fixture.root, 'scripts/restart.sh')], {
      cwd: fixture.root,
      env: {
        ...process.env,
        HIVE_RELEASE_ROOT: toBashPath(fixture.root),
        PATH: `${fixture.bin}${path.delimiter}${process.env.PATH}`,
        MOCK_DOCKER_LOG: toBashPath(log),
        MOCK_DOCKER_STATE: toBashPath(state),
        MOCK_NETWORK_SCENARIO: scenario
      },
      encoding: 'utf8',
      windowsHide: true
    })
    return { result, commands: fs.readFileSync(log, 'utf8') }
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true })
  }
}

for (const scenario of ['absent', 'matching']) {
  test(`${scenario} hive-net keeps the limited backend and Nginx restart`, () => {
    const { result, commands } = runRestart(scenario)
    assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`)
    assert.match(commands, /^compose up -d --force-recreate --remove-orphans backend nginx$/m)
    assert.doesNotMatch(commands, /^compose down /m)
    assert.ok(commands.match(/^network inspect /gm)?.length >= 2, 'network must be checked before and after up')
  })
}

for (const scenario of ['mismatch', 'legacy']) {
  test(`${scenario} hive-net performs one volume-preserving full project migration`, () => {
    const { result, commands } = runRestart(scenario)
    assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`)
    assert.match(commands, /^compose down --remove-orphans$/m)
    assert.match(commands, /^compose up -d$/m)
    assert.doesNotMatch(commands, /^compose down .* (?:-v|--volumes)(?: |$)/m)
    assert.doesNotMatch(commands, /compose up -d .*backend nginx/)
  })
}

test('foreign containers attached to hive-net fail before migration', () => {
  const { result, commands } = runRestart('foreign')
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /非当前 Compose 项目|foreign/i)
  assert.doesNotMatch(commands, /^compose down /m)
})

test('an existing network that cannot be inspected fails before service changes', () => {
  const { result, commands } = runRestart('inspection-error')
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /inspect|检查/i)
  assert.doesNotMatch(commands, /^compose (?:build|stop|down|up) /m)
})

test('network teardown failure stops migration without deleting volumes', () => {
  const { result, commands } = runRestart('down-error')
  assert.notEqual(result.status, 0)
  assert.match(commands, /^compose down --remove-orphans$/m)
  assert.doesNotMatch(commands, /^compose up /m)
  assert.doesNotMatch(commands, /^compose down .* (?:-v|--volumes)(?: |$)/m)
})

test('network recreation failure reports the outage and never deletes volumes', () => {
  const { result, commands } = runRestart('up-error')
  assert.notEqual(result.status, 0)
  assert.match(commands, /^compose down --remove-orphans$/m)
  assert.match(commands, /^compose up -d$/m)
  assert.doesNotMatch(commands, /^compose down .* (?:-v|--volumes)(?: |$)/m)
  assert.doesNotMatch(commands, /^inspect --format .*hive-backend$/m)
})

test('post-up subnet mismatch fails before health checks', () => {
  const { result, commands } = runRestart('post-mismatch')
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /子网|subnet/i)
  assert.match(commands, /^compose up -d --force-recreate --remove-orphans backend nginx$/m)
  assert.doesNotMatch(commands, /^inspect --format .*hive-backend$/m)
})
