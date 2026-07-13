import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const deployRoot = path.resolve('C:/Users/HUAWEI/Desktop/hive部署_全新配置')
const migrationName = 'V20260710_003_builtin_role_permission_matrix.sql'
const migrationPath = path.join(deployRoot, 'db-migrations/migrations', migrationName)
const scopeMigrationName = 'V20260710_004_order_role_status_scope.sql'
const scopeMigrationPath = path.join(deployRoot, 'db-migrations/migrations', scopeMigrationName)

assert.ok(fs.existsSync(migrationPath), `${migrationName} must exist`)
assert.ok(fs.existsSync(scopeMigrationPath), `${scopeMigrationName} must exist`)

const migration = fs.readFileSync(migrationPath, 'utf8')
const scopeMigration = fs.readFileSync(scopeMigrationPath, 'utf8')
const manifest = fs.readFileSync(path.join(deployRoot, 'db-migrations/migration_manifest.txt'), 'utf8')
const deployHealth = fs.readFileSync(path.join(deployRoot, 'scripts/check-deploy-health.sh'), 'utf8')

const expectedRoles = [
  'ADMIN',
  'EMPLOYEE',
  'SALES_STAFF',
  'SALES_MANAGER',
  'WAREHOUSE_STAFF',
  'WAREHOUSE_MANAGER',
  'PRODUCTION_STAFF',
  'PRODUCTION_MANAGER',
  'QUALITY_STAFF',
  'QUALITY_MANAGER',
  'FINANCE_STAFF',
  'FINANCE_MANAGER',
  'HR_STAFF',
  'HR_MANAGER',
  'INSTALLATION_STAFF',
  'INSTALLATION_MANAGER',
  'APPROVAL_MANAGER',
  'DOCUMENT_MANAGER',
  'EQUIPMENT_STAFF',
  'EQUIPMENT_MANAGER'
]

for (const roleCode of expectedRoles) {
  assert.ok(migration.includes(`'${roleCode}'`), `role matrix must include ${roleCode}`)
}

for (const permissionCode of [
  'attendance:punch',
  'attendance:record:list',
  'approval:leave:submit',
  'approval:finance:submit',
  'approval:resignation:submit',
  'document:list',
  'document:breadcrumbs',
  'notification:announcement:list',
  'installation:*',
  'installation:list',
  'installation:update',
  'installation:attachment:upload',
  'installation:attachment:download'
]) {
  assert.ok(migration.includes(`'${permissionCode}'`), `role matrix must include ${permissionCode}`)
}

assert.ok(
  migration.includes("BINARY old_role.role_code = BINARY 'TENANT_OWNER'") &&
    migration.includes("BINARY old_role.role_code = BINARY 'AI_MANAGER'"),
  'deprecated role users must be migrated before the roles are retired'
)
assert.ok(
  migration.includes("permission.perm_code LIKE BINARY 'dashboard:ai%'") &&
    migration.includes('permission.is_deleted = 1'),
  'retired AI permissions must be soft deleted'
)
assert.ok(
  !/^\s*(DELETE\s+FROM|DROP\s+TABLE|ALTER\s+TABLE.+DROP\s+COLUMN)/im.test(migration),
  'automatic role migration must not use destructive cleanup'
)
assert.ok(
  manifest.includes(`migrations/${migrationName}`),
  'role matrix migration must be listed in migration_manifest.txt'
)
assert.ok(
  deployHealth.includes(migrationName),
  'deploy health must require the role matrix migration'
)
assert.ok(
  scopeMigration.includes("BINARY role_item.role_code IN (BINARY 'SALES_STAFF', BINARY 'SALES_MANAGER')") &&
    scopeMigration.includes("BINARY role_item.role_code IN (BINARY 'PRODUCTION_STAFF', BINARY 'PRODUCTION_MANAGER')"),
  'sales and production roles must receive separate order-status scopes'
)
assert.ok(
  scopeMigration.includes("('SALES_MANAGER', 'order:status:pending-confirm')") &&
    scopeMigration.includes("('PRODUCTION_MANAGER', 'order:status:producing')") &&
    !scopeMigration.includes("('SALES_MANAGER', 'order:status:*')"),
  'sales managers must not retain unrestricted order-stage access'
)
assert.ok(manifest.includes(`migrations/${scopeMigrationName}`), 'order role scope migration must be in the manifest')
assert.ok(deployHealth.includes(scopeMigrationName), 'deploy health must require the order role scope migration')

console.log('built-in role matrix checks passed')
