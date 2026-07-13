import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { deployRoot } from './deploy-test-root.js'

const read = (relativePath) => fs.readFileSync(path.join(deployRoot, relativePath), 'utf8')

const compose = read('docker-compose.yml')
const restart = read('scripts/restart.sh')
const deployHealth = read('scripts/check-deploy-health.sh')
const lowCostVerifier = read('scripts/verify-low-cost-mode.sh')
const nginxConfig = read('nginx/nginx.conf')
const aiWrapper = read('scripts/manual-remove-retired-ai-artifacts.sh')
const aiSql = read('db-migrations/manual/V20260706_002_remove_ai_advice_artifacts.sql')
const behaviorSql = read('db-migrations/manual/V20260706_003_remove_behavior_event_artifacts.sql')
const uploadGuide = read('UPLOAD_STEPS.md')

assert.match(compose, /image:\s*nginx:1\.30\.3-alpine\b/,
  'Nginx must use the pinned official stable Alpine image')
assert.ok(!compose.includes('image: nginx:alpine'), 'floating nginx:alpine tag must not be used')

const pullNginx = restart.indexOf('docker compose pull nginx')
const validateNginx = restart.indexOf('docker compose run --rm --no-deps nginx nginx -t')
const stopBackends = restart.indexOf('docker compose stop backend-1 management-backend-1')
const recreateServices = restart.indexOf('docker compose up -d --force-recreate --remove-orphans mysql redis backend-1 management-backend-1 nginx')
const verifyNginxVersion = restart.lastIndexOf('verify_running_nginx_version')
assert.ok(pullNginx >= 0 && pullNginx < stopBackends,
  'Nginx image must be pulled before backend writes are stopped')
assert.ok(validateNginx > pullNginx && validateNginx < stopBackends,
  'pulled Nginx image must validate mounted configuration before downtime')
assert.ok(verifyNginxVersion > recreateServices,
  'running Nginx version must be verified after recreation')
assert.ok(restart.includes("EXPECTED_NGINX_VERSION='nginx/1.30.3'"),
  'runtime verification must use the pinned expected version')
assert.ok(nginxConfig.includes('server_tokens off;'), 'Nginx must hide version tokens')
assert.ok(deployHealth.includes('nginx:1.30.3-alpine'),
  'deploy health must reject an unpinned or unexpected Nginx image')
assert.ok(deployHealth.includes('server_tokens off'),
  'deploy health must require hidden Nginx version tokens')
assert.ok(lowCostVerifier.includes('docker compose pull nginx'),
  'low-cost verifier must require the Nginx pull gate')
assert.ok(lowCostVerifier.includes('docker compose run --rm --no-deps nginx nginx -t'),
  'low-cost verifier must require Nginx preflight validation')

assert.ok(aiWrapper.includes('preview_retired_ai_artifacts'),
  'AI cleanup must provide a read-only preview')
const confirmationCapture = aiWrapper.indexOf('readonly operator_confirmation="${CONFIRM_REMOVE_RETIRED_AI_ARTIFACTS:-NO}"')
const envSource = aiWrapper.indexOf('source ./.env')
const confirmationGate = aiWrapper.indexOf('if [ "${operator_confirmation}" != \'YES\' ]')
const backup = aiWrapper.indexOf('backup-online.sh')
const executeAiSql = aiWrapper.indexOf('mysql_root_db < "${AI_CLEANUP_SQL}"')
assert.ok(confirmationCapture >= 0 && confirmationCapture < envSource,
  'AI cleanup confirmation must be captured before .env is sourced')
assert.ok(confirmationGate > envSource, 'AI cleanup must gate destructive mode')
assert.ok(!aiWrapper.slice(0, confirmationGate).includes('backup-online.sh'),
  'preview mode must not create a backup or mutate runtime state')
assert.ok(backup > confirmationGate && executeAiSql > backup,
  'verified backup must precede destructive AI SQL')
assert.ok(aiWrapper.includes('verify-latest-backup.sh'), 'AI cleanup backup must be verified')
assert.ok(aiWrapper.includes('verify_retired_ai_artifacts_removed'),
  'AI cleanup must verify every retired artifact is gone')

for (const token of [
  'DELETE FROM sys_user_permission',
  'DELETE FROM sys_role_permission',
  'DELETE FROM sys_user_role',
  'DELETE FROM sys_permission',
  'DELETE FROM sys_role',
  'DROP TABLE IF EXISTS ai_advice_snapshot',
  'DROP TABLE IF EXISTS ai_advice_training_sample',
  'DELETE FROM tenant_usage_meter'
]) {
  assert.ok(aiSql.includes(token), `AI cleanup SQL must physically remove: ${token}`)
}
assert.ok(!aiSql.includes('UPDATE sys_permission'), 'retired AI permissions must not be soft-deleted')
assert.ok(!aiSql.includes('UPDATE sys_role'), 'retired AI role must not be soft-deleted')
assert.ok(behaviorSql.includes('DROP TABLE IF EXISTS behavior_event'),
  'retired behavior event table must be removed')
assert.ok(deployHealth.includes('manual-remove-retired-ai-artifacts.sh'),
  'deploy health must require the AI cleanup wrapper')
assert.ok(deployHealth.includes('V20260706_002_remove_ai_advice_artifacts.sql'),
  'deploy health must require AI cleanup SQL')
assert.ok(uploadGuide.indexOf('bash scripts/manual-remove-retired-ai-artifacts.sh') >= 0,
  'upload guide must document AI preview')
assert.ok(
  uploadGuide.indexOf('CONFIRM_REMOVE_RETIRED_AI_ARTIFACTS=YES bash scripts/manual-remove-retired-ai-artifacts.sh') >
    uploadGuide.indexOf('bash scripts/manual-remove-retired-ai-artifacts.sh'),
  'upload guide must document preview before confirmed AI cleanup'
)

console.log('Commercial cutover hardening checks passed')
