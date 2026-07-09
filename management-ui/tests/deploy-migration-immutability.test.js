import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const deployRoot = path.resolve('C:/Users/HUAWEI/Desktop/hive部署_全新配置')

function read(relativePath) {
  return fs.readFileSync(path.join(deployRoot, relativePath), 'utf8')
}

const historicalInstallationTaskMigration = read('db-migrations/migrations/V20260705_004_installation_task_schema.sql')
assert.ok(
  !historicalInstallationTaskMigration.includes('special_exception_note'),
  'V20260705_004 is a historical executed migration and must not contain later special_exception_note changes'
)
assert.ok(
  !historicalInstallationTaskMigration.includes('express_company') &&
    !historicalInstallationTaskMigration.includes('express_no'),
  'V20260705_004 is a historical executed migration and must not contain later logistics changes'
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

const manifest = read('db-migrations/migration_manifest.txt')
assert.ok(
  manifest.includes('migrations/V20260706_001_installation_task_special_exception_note.sql'),
  'new additive migration must be in migration_manifest.txt'
)
assert.ok(
  manifest.includes('migrations/V20260707_001_installation_task_schema_convergence.sql'),
  'installation_task convergence migration must be in migration_manifest.txt'
)

const deployHealth = read('scripts/check-deploy-health.sh')
assert.ok(
  deployHealth.includes('V20260707_001_installation_task_schema_convergence.sql'),
  'deploy health check must guard the installation_task convergence migration'
)

console.log('deploy migration immutability passed')
