import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const healthSource = readFileSync(new URL('../../deploy/scripts/check-deploy-health.sh', import.meta.url), 'utf8')
const inspectSource = readFileSync(new URL('../../deploy/scripts/inspect-backend-artifact.sh', import.meta.url), 'utf8')
const attributesSource = readFileSync(new URL('../../.gitattributes', import.meta.url), 'utf8')
const startSource = readFileSync(new URL('../../deploy/scripts/start.sh', import.meta.url), 'utf8')
const restartSource = readFileSync(new URL('../../deploy/scripts/restart.sh', import.meta.url), 'utf8')

test('unified backend compose detection accepts Linux LF and uploaded Windows CRLF', () => {
  assert.match(healthSource, /grep -Eq '\^  backend:\[\[:space:\]\]\*\$'/)
  assert.match(attributesSource, /deploy\/docker-compose\.yml text eol=lf/)
})

test('backend artifact inspection does not require a host JDK jar command', () => {
  assert.doesNotMatch(inspectSource, /require_command jar/)
  assert.match(inspectSource, /command -v jar/)
  assert.match(inspectSource, /command -v unzip/)
  assert.match(inspectSource, /command -v python3/)
  assert.match(inspectSource, /jar, unzip or python3/)
})

test('source repository metadata remains optional on package-only release hosts', () => {
  const integritySource = readFileSync(new URL('../../deploy/scripts/verify-release-integrity.sh', import.meta.url), 'utf8')
  assert.match(integritySource, /SOURCE_REPOSITORY_ROOT:-/)
  assert.match(integritySource, /MINI_PROGRAM_SOURCE_REPOSITORY_ROOT:-/)
  assert.doesNotMatch(integritySource, /fail "(?:Mini-program )?Git metadata resolution deferred/)
})

test('start and restart normalize Windows env files before any health check reads them', () => {
  for (const source of [startSource, restartSource]) {
    const normalizeIndex = source.indexOf('bash scripts/normalize-env.sh')
    const healthIndex = source.indexOf('bash scripts/check-deploy-health.sh')
    assert.notEqual(normalizeIndex, -1)
    assert.ok(normalizeIndex < healthIndex)
  }
})
