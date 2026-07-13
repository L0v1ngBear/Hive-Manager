import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { deployRoot } from './deploy-test-root.js'

const testsRoot = path.dirname(fileURLToPath(import.meta.url))
const contractHelper = path.join(testsRoot, 'order-information-channel-artifact-contract.js')
assert.ok(fs.existsSync(contractHelper), 'artifact contract helper must exist')

const { inspectDeploymentArtifacts, inspectArtifactDirectories } = await import(
  pathToFileURL(contractHelper)
)

const current = inspectDeploymentArtifacts(deployRoot)
assert.equal(current.ok, false, 'current legacy JAR/dist package must remain blocked')
assert.match(
  current.message,
  /integrate and rebuild all four order information-channel artifacts/i,
  'blocked package must explain the four-artifact rebuild requirement'
)
assert.ok(current.issues.some((issue) => /deliveryDate|delivery_date/.test(issue)))
for (const artifactName of ['mini backend', 'management backend', 'management UI', 'mini program']) {
  assert.ok(
    current.issues.some((issue) => issue.startsWith(`${artifactName}:`)),
    `current package must report why ${artifactName} is not release-compatible`
  )
}

const releaseInfo = fs.readFileSync(path.join(deployRoot, 'RELEASE_BUILD_INFO.txt'), 'utf8')
assert.ok(releaseInfo.includes('OrderInformationChannelContract=BLOCKED'))
assert.ok(!/^OrderInformationChannelContract=READY$/m.test(releaseInfo))
const declaredManifestHash = releaseInfo.match(/^MigrationManifestSHA256=([a-f0-9]{64})$/m)?.[1]
const actualManifestHash = createHash('sha256')
  .update(fs.readFileSync(path.join(deployRoot, 'db-migrations/migration_manifest.txt')))
  .digest('hex')
assert.equal(declaredManifestHash, actualManifestHash, 'release metadata must hash the current manifest')

const fixtureRoot = path.join(testsRoot, '.order-artifact-contract-fixture')
try {
  fs.rmSync(fixtureRoot, { recursive: true, force: true })
  const artifactDirectories = [
    'mini-backend',
    'management-backend',
    'management-ui',
    'mini-program'
  ].map((name) => path.join(fixtureRoot, name))
  for (const directory of artifactDirectories) {
    fs.mkdirSync(directory, { recursive: true })
    fs.writeFileSync(
      path.join(directory, 'contract.txt'),
      'informationChannel information_channel cancelReason cancel_reason',
      'utf8'
    )
  }

  const blockedSimulation = inspectArtifactDirectories(
    artifactDirectories,
    'OrderInformationChannelContract=BLOCKED'
  )
  assert.equal(blockedSimulation.ok, false, 'rebuilt artifacts still require an explicit READY marker')

  const simulated = inspectArtifactDirectories(
    artifactDirectories,
    'OrderInformationChannelContract=READY'
  )
  assert.equal(simulated.ok, true, simulated.message)
} finally {
  fs.rmSync(fixtureRoot, { recursive: true, force: true })
}

const artifactGatePath = path.join(deployRoot, 'scripts/verify-order-information-channel-artifacts.sh')
assert.ok(fs.existsSync(artifactGatePath), 'deploy package must include an artifact contract gate')
const artifactGate = fs.readFileSync(artifactGatePath, 'utf8')
assert.ok(artifactGate.includes('deliveryDate') && artifactGate.includes('informationChannel'))
assert.ok(artifactGate.includes('OrderInformationChannelContract=READY'))

for (const relativePath of [
  'scripts/verify-release-integrity.sh',
  'scripts/check-deploy-health.sh',
  'scripts/migrate-db.sh'
]) {
  const script = fs.readFileSync(path.join(deployRoot, relativePath), 'utf8')
  assert.ok(
    script.includes('verify-order-information-channel-artifacts.sh'),
    `${relativePath} must enforce the artifact contract before migration or release`
  )
}

console.log('order information-channel release gate checks passed')
