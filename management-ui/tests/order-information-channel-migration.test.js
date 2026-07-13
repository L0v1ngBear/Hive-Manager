import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
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
assert.match(
  migration,
  /DATE_FORMAT\(CAST\(`delivery_date` AS DATE\), ''%Y-%m-%d''\)/,
  'recognizable historical dates must be normalized to YYYY-MM-DD'
)
assert.match(
  migration,
  /DROP INDEX[\s\S]+DROP COLUMN `delivery_date`/,
  'migration must remove indexes depending on delivery_date before dropping the old column'
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

const baselineRebuild = read('db-migrations/scripts/rebuild-mysql-from-baseline.sh')
assert.ok(
  baselineRebuild.includes('bash "${SCRIPT_DIR}/verify-schema-only-baseline.sh"'),
  'schema-only baseline rebuild must use the schema-only verifier'
)
assert.ok(
  !/echo "9\/9[\s\S]+verify-online-schema\.sh/.test(baselineRebuild),
  'schema-only baseline rebuild must not require schema_migration_history before migrations run'
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
