import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { fileURLToPath } from 'node:url'

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const repoRoot = path.resolve(uiRoot, '..')
const verifier = fs.readFileSync(
  path.join(repoRoot, 'db-migrations/scripts/verify-online-schema.sh'),
  'utf8'
)

assert.match(
  verifier,
  /failed_migrations="\$\(mysql_root_db "\$\{DATABASE_NAME\}" -N -B -e "SELECT COUNT\(\*\) FROM schema_migration_history WHERE status <> 'SUCCESS';"\)"/,
  'schema verifier must count failed migrations instead of only printing them'
)

assert.match(
  verifier,
  /if \[ "\$\{failed_migrations\}" != "0" \]; then[\s\S]+fail "Found failed database migrations"/,
  'schema verifier must fail when any migration history row is not SUCCESS'
)

assert.match(
  verifier,
  /missing_columns="\$\(mysql_root_db "\$\{DATABASE_NAME\}" -N -B <<'EOSQL'/,
  'schema verifier must capture required-column query output'
)

assert.ok(
  verifier.includes("SELECT 'installation_task', 'express_company'"),
  'schema verifier must verify installation task logistics company column'
)

assert.ok(
  verifier.includes("SELECT 'installation_task', 'express_no'"),
  'schema verifier must verify installation task logistics number column'
)

for (const column of [
  'tenant_code',
  'installation_task_id',
  'installer_name',
  'installer_phone',
  'sort_order',
  'create_time',
  'update_time'
]) {
  assert.ok(
    verifier.includes(`SELECT 'installation_task_installer', '${column}'`),
    `schema verifier must require installation_task_installer.${column}`
  )
}

assert.ok(
  !verifier.includes("SELECT 'installation_task', 'construction_personnel'"),
  'schema verifier must not require the retired construction_personnel column'
)

assert.ok(
  !verifier.includes("SELECT 'installation_task', 'construction_phone'"),
  'schema verifier must not require the retired construction_phone column'
)

assert.ok(
  verifier.includes("SELECT 'installation_task_installer', 'INDEX', 'tenant_code,installation_task_id'") &&
    verifier.includes("SELECT 'installation_task_installer', 'UNIQUE', 'tenant_code,installation_task_id,sort_order'"),
  'schema verifier must require installer lookup and sort-order index contracts'
)

assert.match(
  verifier,
  /NOT \(src\.table_name = 'installation_task'\s+AND src\.column_name IN \('construction_personnel', 'construction_phone'\)\)/,
  'baseline comparison must explicitly exclude the intentionally retired installation task columns'
)

assert.match(
  verifier,
  /if \[ -n "\$\{missing_columns\}" \]; then\s+echo "\$\{missing_columns\}"\s+fail "Missing required schema columns"/,
  'schema verifier must fail when required tables or columns are missing'
)

assert.match(
  verifier,
  /missing_baseline_columns="\$\(mysql_root_no_db -N -B -e "/,
  'schema verifier must capture baseline comparison output'
)

assert.match(
  verifier,
  /if \[ -n "\$\{missing_baseline_columns\}" \]; then\s+echo "\$\{missing_baseline_columns\}"\s+fail "Target schema is missing columns from baseline schema"/,
  'schema verifier must fail when target schema misses baseline columns'
)

console.log('deploy schema verifier checks passed')
