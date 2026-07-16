import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { fileURLToPath } from 'node:url'

const uiRoot = fileURLToPath(new URL('..', import.meta.url))
const repoRoot = path.resolve(uiRoot, '..')

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

const historicalInstallationTaskMigration = read('db-migrations/migrations/V20260705_004_installation_task_schema.sql')
assert.equal(
  createHash('sha256').update(historicalInstallationTaskMigration).digest('hex'),
  '9ab18545b9f6ef0142a943d54a27ac726a813c3361f4a81f4763f6222d98cd2d',
  'V20260705_004 must remain byte-for-byte identical to the migration already recorded by the server'
)
assert.ok(
  !historicalInstallationTaskMigration.includes('special_exception_note'),
  'V20260705_004 is a historical executed migration and must not contain later special_exception_note changes'
)
assert.ok(
  historicalInstallationTaskMigration.includes('express_company') &&
    historicalInstallationTaskMigration.includes('express_no'),
  'V20260705_004 must retain the logistics columns present in the executed historical file'
)

const additiveMigration = read('db-migrations/migrations/V20260706_001_installation_task_special_exception_note.sql')
assert.ok(
  additiveMigration.includes('special_exception_note'),
  'special_exception_note must be introduced by a new additive migration'
)

const convergenceMigration = read('db-migrations/migrations/V20260707_001_installation_task_schema_convergence.sql')
assert.ok(
  convergenceMigration.includes('express_company') &&
    convergenceMigration.includes('express_no') &&
    convergenceMigration.includes('special_exception_note'),
  'later installation_task columns must be introduced by the convergence migration'
)

const uniqueKeyRepairMigration = read('db-migrations/migrations/V20260710_001_installation_task_unique_key_repair.sql')
assert.ok(
  uniqueKeyRepairMigration.includes('ADD UNIQUE KEY') &&
    uniqueKeyRepairMigration.includes('`tenant_code`, `order_id`'),
  'installation_task convergence must enforce one task per tenant order'
)

const manifest = read('db-migrations/migration_manifest.txt')
assert.ok(
  manifest.includes('migrations/V20260706_001_installation_task_special_exception_note.sql'),
  'new additive migration must be in migration_manifest.txt'
)
assert.ok(
  manifest.includes('migrations/V20260707_001_installation_task_schema_convergence.sql'),
  'installation_task convergence migration must be in migration_manifest.txt'
)
assert.ok(
  manifest.includes('migrations/V20260710_001_installation_task_unique_key_repair.sql'),
  'installation_task unique-key repair migration must be in migration_manifest.txt'
)
assert.ok(
  manifest.includes('migrations/V20260710_002_retire_production_order_print_tasks.sql'),
  'retired production-order print tasks must be removed from active printer queues'
)
assert.ok(
  manifest.includes('migrations/V20260710_003_builtin_role_permission_matrix.sql'),
  'built-in role permission matrix must be versioned after all historical migrations'
)
assert.ok(
  manifest.includes('migrations/V20260710_004_order_role_status_scope.sql'),
  'sales and production order scopes must be versioned after the role matrix'
)

const orderNoteMigration = read('db-migrations/migrations/V20260715_001_order_notes_and_material_approval.sql')
for (const token of [
  'CREATE TABLE IF NOT EXISTS `sales_order_note`',
  'DROP COLUMN `remark`',
  'order:note:view',
  'order:note:create',
  'order:note:update',
  'order:audit:material'
]) {
  assert.ok(orderNoteMigration.includes(token), `order note migration must contain: ${token}`)
}
assert.ok(
  manifest.includes('migrations/V20260715_001_order_notes_and_material_approval.sql'),
  'order notes and material approval migration must be in migration_manifest.txt'
)

const deployHealth = read('deploy/scripts/check-deploy-health.sh')
assert.ok(
  deployHealth.includes('V20260710_001_installation_task_unique_key_repair.sql'),
  'deploy health check must guard the installation_task unique-key repair migration'
)
assert.ok(
  deployHealth.includes('V20260710_003_builtin_role_permission_matrix.sql'),
  'deploy health check must guard the built-in role permission matrix migration'
)
assert.ok(
  deployHealth.includes('V20260710_004_order_role_status_scope.sql'),
  'deploy health check must guard the sales/production order scope migration'
)
assert.ok(
  deployHealth.includes('V20260715_001_order_notes_and_material_approval.sql'),
  'deploy health check must guard the order notes and material approval migration'
)

console.log('deploy migration immutability passed')
