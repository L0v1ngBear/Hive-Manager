import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { fileURLToPath } from 'node:url'

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const deployRoot = path.resolve(uiRoot, '..', 'deploy')

function read(relativePath) {
  return fs.readFileSync(path.join(deployRoot, relativePath), 'utf8')
}

const deployHealth = read('scripts/check-deploy-health.sh')

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

console.log('deploy secret hardening checks passed')
