import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { pathToFileURL } from 'node:url'
import assert from 'node:assert/strict'

const testsDirectory = path.resolve('tests')
const helperPath = path.join(testsDirectory, 'deploy-test-root.js')

assert.ok(fs.existsSync(helperPath), 'deployment tests must share a configurable deploy-root helper')

const { resolveDeployRoot } = await import(pathToFileURL(helperPath))
const configuredRoot = path.resolve('D:/isolated-hive-deploy')
assert.equal(
  resolveDeployRoot({ HIVE_DEPLOY_ROOT: configuredRoot }, 'C:/ignored-home'),
  configuredRoot,
  'HIVE_DEPLOY_ROOT must override the local desktop fallback'
)

const localHome = path.resolve('C:/local-test-home')
assert.equal(
  resolveDeployRoot({}, localHome),
  path.resolve(localHome, 'Desktop', 'hive部署_全新配置'),
  'the desktop deployment path must be used only as a local fallback'
)

const deploymentTests = fs.readdirSync(testsDirectory)
  .filter((name) => name.endsWith('.test.js') && name !== path.basename(import.meta.filename))
  .filter((name) => fs.readFileSync(path.join(testsDirectory, name), 'utf8').includes('deployRoot'))

for (const testName of deploymentTests) {
  const content = fs.readFileSync(path.join(testsDirectory, testName), 'utf8')
  assert.ok(
    content.includes("from './deploy-test-root.js'"),
    `${testName} must use the shared HIVE_DEPLOY_ROOT-aware helper`
  )
}

console.log('deploy test root checks passed')
