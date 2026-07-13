import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { deployRoot } from './deploy-test-root.js'

const read = (relativePath) => fs.readFileSync(path.join(deployRoot, relativePath), 'utf8')
const readBuffer = (relativePath) => fs.readFileSync(path.join(deployRoot, relativePath))

const wrapperPath = path.join(deployRoot, 'scripts/manual-remove-tenant-002.sh')
const sqlPath = path.join(deployRoot, 'db-migrations/manual/V20260711_001_remove_tenant_002.sql')
assert.ok(fs.existsSync(wrapperPath), 'TENANT_002 cleanup wrapper must exist')
assert.ok(fs.existsSync(sqlPath), 'TENANT_002 cleanup SQL must exist')

const wrapper = read('scripts/manual-remove-tenant-002.sh')
const cleanupSql = read('db-migrations/manual/V20260711_001_remove_tenant_002.sql')
const manifest = read('db-migrations/migration_manifest.txt')
const historical = readBuffer('db-migrations/migrations/V20260530_001_second_tenant_seed.sql')
const deployHealth = read('scripts/check-deploy-health.sh')
const uploadGuide = read('UPLOAD_STEPS.md')
const automaticScripts = [
  'scripts/start.sh',
  'scripts/restart.sh',
  'scripts/rebuild-all.sh',
  'scripts/migrate-db.sh',
  'scripts/accept-low-cost-release.sh'
].map((relativePath) => [relativePath, read(relativePath)])

assert.ok(wrapper.includes('CONFIRM_REMOVE_TENANT_002'), 'cleanup must require explicit confirmation')
assert.ok(wrapper.includes('preview_tenant_data'), 'cleanup must provide read-only preview')
const confirmationCapture = wrapper.indexOf('readonly operator_confirmation="${CONFIRM_REMOVE_TENANT_002:-NO}"')
const envSource = wrapper.indexOf('source ./.env')
const confirmationGate = wrapper.indexOf('if [ "${operator_confirmation}" != \'YES\' ]')
assert.ok(confirmationCapture >= 0 && confirmationCapture < envSource,
  'operator confirmation must be captured immutably before .env is sourced')
assert.ok(confirmationGate > envSource, 'destructive mode must use the captured operator confirmation')
assert.ok(!wrapper.slice(0, confirmationGate).includes('docker compose up'),
  'preview mode must not start or recreate containers')
assert.ok(
  wrapper.indexOf('backup-online.sh') < wrapper.indexOf('mysql_root_db < "${CLEANUP_SQL}"'),
  'backup must run before destructive SQL'
)
for (const destructiveToken of ['mysql_root_db < "${CLEANUP_SQL}"', ' UNLINK ', 'rm -rf --']) {
  assert.ok(wrapper.indexOf(destructiveToken) > confirmationGate,
    `${destructiveToken} must run only after explicit confirmation`)
}
assert.ok(wrapper.includes('verify-latest-backup.sh'), 'backup must be verified')
assert.ok(wrapper.includes("readonly TARGET_TENANT='TENANT_002'"), 'target must be fixed and immutable')
assert.ok(wrapper.includes("readonly CLEANUP_SQL='db-migrations/manual/V20260711_001_remove_tenant_002.sql'"),
  'cleanup SQL path must be fixed and immutable')
assert.ok(wrapper.includes("--pattern '*TENANT_002*'"), 'Redis cleanup must be tenant-scoped')
assert.ok(wrapper.includes('redis_key_has_target_segment'), 'Redis keys must pass a tenant segment check')
assert.ok(wrapper.includes('*":${TARGET_TENANT}:"*'), 'Redis tenant marker must be colon-delimited')
assert.ok(wrapper.includes('-name "${TARGET_TENANT}"'), 'upload cleanup must match the exact tenant directory')
assert.ok(wrapper.includes('realpath -m'), 'upload cleanup must verify resolved paths')

assert.ok(cleanupSql.includes("SET @hive_target_tenant := 'TENANT_002'"), 'SQL target must be fixed')
assert.ok(cleanupSql.includes('START TRANSACTION'), 'cleanup must be transactional')
assert.ok(cleanupSql.includes('ROLLBACK'), 'cleanup must roll back on failure')
assert.ok(cleanupSql.includes('RESIGNAL'), 'cleanup must preserve SQL failure')
assert.ok(cleanupSql.includes('information_schema.COLUMNS'), 'cleanup must cover every tenant_code table')
assert.ok(cleanupSql.includes('nontransactional_table_count'), 'cleanup must detect nontransactional tables')
assert.ok(cleanupSql.includes("UPPER(table_item.ENGINE) <> 'INNODB'"),
  'cleanup must reject non-InnoDB tenant tables')
assert.ok(cleanupSql.includes('DELETE role_permission'), 'role permissions require explicit child cleanup')
assert.ok(
  cleanupSql.lastIndexOf('DELETE FROM `tenant`') > cleanupSql.lastIndexOf('EXECUTE cleanup_stmt'),
  'tenant row must be deleted last'
)
assert.ok(!cleanupSql.includes("tenant_code = 'TENANT_001'"), 'cleanup must not target TENANT_001')
assert.ok(!cleanupSql.includes("tenant_code = 'super'"), 'cleanup must not target super')

assert.ok(
  !manifest.includes('V20260711_001_remove_tenant_002.sql'),
  'destructive cleanup must not run as an automatic migration'
)
for (const [relativePath, content] of automaticScripts) {
  assert.ok(!content.includes('manual-remove-tenant-002.sh'), `${relativePath} must not run tenant cleanup`)
  assert.ok(!content.includes('V20260711_001_remove_tenant_002.sql'), `${relativePath} must not run cleanup SQL`)
}
assert.equal(
  createHash('sha256').update(historical).digest('hex'),
  'b9da086e1b5b533b7ceedc629115d26612283b1fadcadcc3c6cbaf23114d5ace',
  'historical second-tenant seed must remain byte-for-byte unchanged'
)
assert.ok(deployHealth.includes('manual-remove-tenant-002.sh'), 'deploy health must require the wrapper')
assert.ok(deployHealth.includes('V20260711_001_remove_tenant_002.sql'), 'deploy health must require the SQL')
const previewCommand = uploadGuide.indexOf('bash scripts/manual-remove-tenant-002.sh')
const confirmedCommand = uploadGuide.indexOf('CONFIRM_REMOVE_TENANT_002=YES bash scripts/manual-remove-tenant-002.sh')
assert.ok(previewCommand >= 0 && confirmedCommand > previewCommand,
  'upload guide must document preview before confirmed execution')

console.log('TENANT_002 cleanup safety checks passed')
