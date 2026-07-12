import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'

const desktop = path.join(os.homedir(), 'Desktop')
const deployDirectory = fs.readdirSync(desktop).find((name) => {
  const candidate = path.join(desktop, name)
  return name.toLowerCase().startsWith('hive') &&
    fs.existsSync(path.join(candidate, 'docker-compose.yml')) &&
    fs.existsSync(path.join(candidate, 'scripts', 'restart.sh'))
})

assert.ok(deployDirectory, 'Hive deployment package must exist on the desktop')
const deployRoot = path.join(desktop, deployDirectory)
const wrapperRelativePath = 'scripts/manual-reset-tenant-001.sh'
const sqlRelativePath = 'db-migrations/manual/V20260712_001_reset_tenant_001_business_data.sql'
const wrapperPath = path.join(deployRoot, wrapperRelativePath)
const sqlPath = path.join(deployRoot, sqlRelativePath)

assert.ok(fs.existsSync(wrapperPath), 'TENANT_001 reset wrapper must exist')
assert.ok(fs.existsSync(sqlPath), 'TENANT_001 reset SQL must exist')

const read = (relativePath) => fs.readFileSync(path.join(deployRoot, relativePath), 'utf8')
const wrapper = read(wrapperRelativePath)
const resetSql = read(sqlRelativePath)
const manifest = read('db-migrations/migration_manifest.txt')
const deployHealth = read('scripts/check-deploy-health.sh')

assert.ok(wrapper.includes("readonly TARGET_TENANT='TENANT_001'"), 'reset target must be fixed')
assert.ok(wrapper.includes('preview_tenant_reset'), 'wrapper must provide read-only preview')
assert.ok(wrapper.includes('verify_tenant_reset'), 'wrapper must verify removable residue is zero')
assert.ok(wrapper.includes('count_preserved_administrators'), 'preview must identify preserved administrators')
assert.ok(wrapper.includes('count_active_system_roles'), 'preview must count active built-in roles')

const confirmationCapture = wrapper.indexOf('readonly operator_confirmation="${CONFIRM_RESET_TENANT_001:-NO}"')
const envSource = wrapper.indexOf('source ./.env')
const confirmationGate = wrapper.indexOf('if [ "${operator_confirmation}" != \'YES\' ]')
const backup = wrapper.indexOf('backup-online.sh')
const executeSql = wrapper.indexOf('mysql_root_db < "${RESET_SQL}"')
assert.ok(confirmationCapture >= 0 && confirmationCapture < envSource,
  'confirmation must be captured before .env is sourced')
assert.ok(confirmationGate > envSource, 'destructive mode must have an explicit gate')
assert.ok(!wrapper.slice(0, confirmationGate).includes('backup-online.sh'),
  'preview must not create a backup or mutate runtime state')
assert.ok(backup > confirmationGate && executeSql > backup,
  'verified backup must precede reset SQL')
assert.ok(wrapper.includes('verify-latest-backup.sh'), 'database backup must be verified')
assert.ok(!wrapper.includes('docker compose up'), 'reset wrapper must not start or recreate services')
assert.doesNotMatch(wrapper, /\b(?:ssh|scp)\b|hellohive\.top/i,
  'local reset wrapper must not connect to the server')

assert.ok(wrapper.includes("--pattern '*TENANT_001*'"), 'Redis scan must use the fixed tenant')
assert.ok(wrapper.includes('redis_key_has_target_segment'), 'Redis deletion must require an exact tenant segment')
assert.ok(wrapper.includes('-name "${TARGET_TENANT}"'), 'upload cleanup must match exact tenant directories')
assert.ok(wrapper.includes('realpath -m'), 'upload cleanup must enforce resolved path boundaries')

for (const token of [
  "SET @hive_reset_tenant := 'TENANT_001'",
  'tmp_hive_preserved_admin_user',
  'tmp_hive_preserved_system_role',
  'tmp_hive_preserved_department',
  'tmp_hive_preserved_position',
  'preserved_admin_count < 1',
  'active_system_role_count < 20',
  'information_schema.COLUMNS',
  'nontransactional_table_count',
  'START TRANSACTION',
  'ROLLBACK',
  'RESIGNAL',
  'DELETE FROM `sys_user_permission`',
  'DELETE role_permission',
  'DELETE user_role',
  'DELETE employee_ext',
  'DELETE user_item',
  'DELETE role_item',
  'UPDATE `user` user_item'
]) {
  assert.ok(resetSql.includes(token), `reset SQL missing safety behavior: ${token}`)
}

assert.match(resetSql, /role_item\.`is_system`\s*=\s*1/,
  'active built-in roles must define the preserved role set')
assert.match(resetSql, /role_item\.`role_code`\s*=\s*BINARY 'ADMIN'/,
  'preserved users must be active administrators')
assert.doesNotMatch(resetSql, /DELETE\s+FROM\s+`?tenant`?/i, 'tenant row must never be deleted')
assert.doesNotMatch(resetSql, /DELETE\s+FROM\s+`?sys_permission`?/i,
  'global permission catalog must never be deleted')
assert.ok(!manifest.includes('V20260712_001_reset_tenant_001_business_data.sql'),
  'reset SQL must not be an automatic migration')
assert.ok(deployHealth.includes(wrapperRelativePath), 'deploy health must require the reset wrapper')
assert.ok(deployHealth.includes(sqlRelativePath), 'deploy health must require the reset SQL')

console.log('TENANT_001 reset safety checks passed')
