import path from 'node:path'
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const testsRoot = path.dirname(fileURLToPath(import.meta.url))
const managementUiRoot = path.resolve(testsRoot, '..')
const checkoutRoot = path.resolve(managementUiRoot, '..')
const targetTest = path.join(testsRoot, 'deploy-test-root.test.js')

for (const cwd of [checkoutRoot, managementUiRoot]) {
  const result = spawnSync(process.execPath, [targetTest], {
    cwd,
    env: process.env,
    encoding: 'utf8'
  })
  assert.equal(
    result.status,
    0,
    `deployment tests must run from ${cwd}\n${result.stdout}\n${result.stderr}`
  )
}

console.log('deploy cwd compatibility checks passed')
