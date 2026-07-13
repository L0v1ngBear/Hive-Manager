import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { fileURLToPath } from 'node:url'
import { deployRoot } from './deploy-test-root.js'

const migrationPath = 'db-migrations/migrations/V20260713_001_order_information_channel_and_cancel_reason.sql'
const manifestEntry = 'migrations/V20260713_001_order_information_channel_and_cancel_reason.sql'

function read(relativePath) {
  return fs.readFileSync(path.join(deployRoot, relativePath), 'utf8')
}

function tableDefinition(baseline, tableName) {
  const match = baseline.match(
    new RegExp('CREATE TABLE `' + tableName + '` \\([\\s\\S]*?\\n\\) ENGINE=')
  )
  assert.ok(match, `schema baseline must define ${tableName}`)
  return match[0]
}

const migrationAbsolutePath = path.join(deployRoot, migrationPath)
assert.ok(
  fs.existsSync(migrationAbsolutePath),
  'order information-channel migration file must exist in the deploy package'
)
const migration = fs.readFileSync(migrationAbsolutePath, 'utf8')
assert.ok(
  migration.includes('CREATE PROCEDURE hive_migrate_delivery_date_to_information_channel'),
  'migration must use the established information_schema procedure guard style'
)
assert.match(
  migration,
  /information_schema\.columns[\s\S]+information_channel/,
  'migration must guard each information_channel add with information_schema'
)
for (const tableName of ['sales_order', 'production_order', 'installation_task']) {
  assert.ok(
    migration.includes(`CALL hive_migrate_delivery_date_to_information_channel('${tableName}')`),
    `migration must converge ${tableName}`
  )
}
assert.match(
  migration,
  /ADD COLUMN `information_channel` varchar\(100\)/,
  'migration must add information_channel VARCHAR(100)'
)
assert.match(
  migration,
  /MODIFY COLUMN `information_channel` varchar\(100\)/,
  'migration must converge an existing information_channel to VARCHAR(100)'
)
assert.ok(
  migration.includes("data_type <> 'varchar'") &&
    migration.includes('character_maximum_length <> 100'),
  'migration must inspect the existing information_channel type and length'
)
assert.ok(
  migration.includes('Existing information_channel value exceeds VARCHAR(100)') &&
    migration.includes("SIGNAL SQLSTATE '45000'"),
  'migration must fail before narrowing an oversized information_channel value'
)
assert.match(
  migration,
  /ADD COLUMN `cancel_reason` varchar\(500\)/,
  'migration must add sales_order.cancel_reason VARCHAR(500)'
)
assert.ok(
  migration.includes('历史交付日期：'),
  'migration must preserve historical delivery values in information_channel'
)
assert.ok(
  migration.includes('Historical delivery data cannot fit in information_channel VARCHAR(100)'),
  'migration must fail when two non-empty columns cannot be merged without truncation'
)
assert.match(
  migration,
  /CONCAT\(CAST\(`information_channel` AS CHAR\), ''；'',/,
  'migration must append a non-empty delivery value to a non-empty information channel'
)
assert.ok(
  !/\b(?:LEFT|SUBSTRING)\s*\(/i.test(migration),
  'migration must never truncate information_channel or preserved delivery data'
)
assert.ok(
  migration.includes("STR_TO_DATE(CAST(`delivery_date` AS CHAR), ''%Y-%m-%d'')") &&
    migration.includes("''%Y-%m-%d''"),
  'recognizable string dates must be normalized to YYYY-MM-DD without discarding datetime seconds'
)
assert.match(
  migration,
  /DROP INDEX[\s\S]+DROP COLUMN `delivery_date`/,
  'migration must remove indexes depending on delivery_date before dropping the old column'
)
assert.doesNotMatch(
  migration,
  /DECLARE[\s\S]+CURSOR|OPEN\s+dependent_indexes/,
  'migration must not keep a nonholdable cursor open across index DDL'
)
assert.match(
  migration,
  /GROUP_CONCAT\([\s\S]+DROP INDEX/,
  'migration must aggregate dependent index drops into one MySQL 8 DDL statement'
)
assert.ok(
  migration.includes("'%Y-%m-%d %H:%i:%s'") &&
    migration.includes("data_type IN ('datetime', 'timestamp')"),
  'DATETIME and TIMESTAMP delivery values must retain seconds'
)
assert.ok(
  migration.includes("data_type = 'date'") && migration.includes("'%Y-%m-%d'"),
  'DATE delivery values must retain date precision without inventing a time'
)
assert.match(
  migration,
  /MODIFY COLUMN `cancel_reason` varchar\(500\) DEFAULT NULL/,
  'migration must converge an existing cancel_reason to nullable VARCHAR(500)'
)
assert.ok(
  migration.includes('Existing cancel_reason value exceeds VARCHAR(500)') &&
    migration.includes('CHAR_LENGTH(CAST(`cancel_reason` AS CHAR)) > 500'),
  'migration must fail before narrowing an oversized cancel_reason'
)
assert.ok(
  migration.indexOf('CALL hive_converge_sales_order_cancel_reason()') <
    migration.indexOf("CALL hive_migrate_delivery_date_to_information_channel('sales_order')"),
  'cancel_reason must fail fast before any delivery_date column can be dropped'
)

const manifestEntries = read('db-migrations/migration_manifest.txt')
  .split(/\r?\n/)
  .map((line) => line.trim())
  .filter((line) => line && !line.startsWith('#'))
assert.equal(
  manifestEntries.at(-1),
  manifestEntry,
  'order information-channel migration must be appended after every historical migration'
)
assert.equal(
  manifestEntries.filter((entry) => entry === manifestEntry).length,
  1,
  'order information-channel migration must appear exactly once in the manifest'
)

const verifier = read('db-migrations/scripts/verify-online-schema.sh')
for (const tableName of ['sales_order', 'production_order', 'installation_task']) {
  assert.ok(
    verifier.includes(`SELECT '${tableName}', 'information_channel'`),
    `schema verifier must require ${tableName}.information_channel`
  )
  assert.ok(
    verifier.includes(`SELECT '${tableName}', 'delivery_date'`),
    `schema verifier must reject legacy ${tableName}.delivery_date`
  )
}
assert.ok(
  verifier.includes("SELECT 'sales_order', 'cancel_reason'"),
  'schema verifier must require sales_order.cancel_reason'
)
assert.ok(
  verifier.includes('V20260713_001_order_information_channel_and_cancel_reason'),
  'schema verifier must require the new migration to be SUCCESS'
)
assert.ok(
  verifier.includes('Legacy delivery_date columns remain'),
  'schema verifier must fail when a retired delivery_date column remains'
)
assert.ok(
  verifier.includes("c.data_type <> 'varchar'") &&
    verifier.includes('c.character_maximum_length <> 100'),
  'schema verifier must require every information_channel to be VARCHAR(100)'
)
assert.ok(
  verifier.includes("c.is_nullable <> 'YES'") && verifier.includes('c.column_default IS NOT NULL'),
  'schema verifier must require nullable columns with a NULL default'
)
assert.ok(
  verifier.includes("'cancel_reason', 500") &&
    verifier.includes('Invalid order column definitions'),
  'schema verifier must enforce the full cancel_reason contract'
)
assert.ok(
  verifier.includes("'sales_order', 'PRIMARY', 'order_id'") &&
    verifier.includes("'production_order', 'UNIQUE', 'order_id'") &&
    verifier.includes("'installation_task', 'UNIQUE', 'tenant_code,order_id'"),
  'schema verifier must enforce semantic primary and unique index contracts'
)

const baselineRebuild = read('db-migrations/scripts/rebuild-mysql-from-baseline.sh')
assert.ok(
  baselineRebuild.includes('bash "${SCRIPT_DIR}/verify-online-schema.sh"'),
  'baseline rebuild must online-verify its registered baseline migration state'
)
assert.ok(
  !baselineRebuild.includes('run-versioned-migrations.sh') &&
    !baselineRebuild.includes('scripts/migrate-db.sh'),
  'baseline rebuild must not replay historical migrations against the latest baseline'
)
assert.ok(
  baselineRebuild.includes('verify-order-information-channel-artifacts.sh'),
  'baseline rebuild must reject old application artifacts before replacing the database schema'
)

const baselineImport = read('db-migrations/scripts/import-baseline-to-shadow.sh')
assert.ok(
  baselineImport.includes('BASELINE_MIGRATION_VERSION') &&
    baselineImport.includes('migration_manifest.txt') &&
    baselineImport.includes('checksum_sha256') &&
    baselineImport.includes("status = 'SUCCESS'"),
  'baseline import must register a checksummed baseline and checksummed manifest history'
)

const rebuildAll = read('scripts/rebuild-all.sh')
assert.ok(
  rebuildAll.includes('db-migrations/scripts/verify-online-schema.sh') &&
    !rebuildAll.includes('SKIP_BACKUP=YES bash scripts/migrate-db.sh'),
  'full rebuild must verify the registered baseline without replaying historical migrations'
)

const integrity = read('scripts/verify-release-integrity.sh')
assert.ok(
  integrity.includes('V20260705_004_installation_task_schema.sql') &&
    integrity.includes('V20260707_001_installation_task_schema_convergence.sql') &&
    integrity.includes('V20260710_004_order_role_status_scope.sql'),
  'release integrity must pin every protected historical migration hash'
)
assert.ok(
  integrity.includes('DROP[[:space:]]+(TABLE|COLUMN|INDEX)') &&
    integrity.includes('explicitly allowlisted'),
  'release integrity must detect destructive DDL inside dynamic SQL strings'
)

const rehearsalTest = fs.readFileSync(
  path.join(path.dirname(fileURLToPath(import.meta.url)), 'order-information-channel-mysql8-rehearsal.test.js'),
  'utf8'
)
assert.ok(
  rehearsalTest.includes("from 'node:test'") && !rehearsalTest.includes('process.exit(0)'),
  'Docker-unavailable rehearsal must report a node:test skip'
)
assert.ok(
  rehearsalTest.includes("'mysql:8.0.42'") &&
    /finally[\s\S]+docker\(\['rm', '--force', containerName\]\)/.test(rehearsalTest),
  'rehearsal must pin MySQL 8 and always clean the attempted container name'
)
assert.ok(
  rehearsalTest.includes("createDatabase('hive_old_schema'") &&
    rehearsalTest.includes("createDatabase('hive_partial_schema'") &&
    rehearsalTest.includes("createDatabase('hive_completed_schema'"),
  'rehearsal must execute V001 against old, partial, and already-completed structures'
)

const baseline = read('db-migrations/baseline/hive_schema_baseline.sql')
for (const tableName of ['sales_order', 'production_order', 'installation_task']) {
  const definition = tableDefinition(baseline, tableName)
  assert.match(
    definition,
    /`information_channel` varchar\(100\) DEFAULT NULL/,
    `schema baseline must include ${tableName}.information_channel without old-field compatibility`
  )
  assert.ok(
    !definition.includes('`delivery_date`'),
    `schema baseline must not retain ${tableName}.delivery_date compatibility`
  )
}
assert.match(
  tableDefinition(baseline, 'sales_order'),
  /`cancel_reason` varchar\(500\) DEFAULT NULL/,
  'schema baseline must include sales_order.cancel_reason'
)

const deployHealth = read('scripts/check-deploy-health.sh')
assert.ok(
  deployHealth.includes(`require_file "db-migrations/${manifestEntry}"`),
  'deploy health must require the migration file to exist'
)
assert.ok(
  deployHealth.includes(manifestEntry),
  'deploy health must require the migration manifest entry'
)

const integrityVerifier = read('scripts/verify-release-integrity.sh')
assert.ok(
  integrityVerifier.includes(manifestEntry),
  'release integrity must allow only this versioned removal migration to drop retired columns'
)

console.log('order information-channel migration checks passed')
