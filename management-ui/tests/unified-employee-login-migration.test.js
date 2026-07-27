import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const migrationsDir = path.resolve(__dirname, '../../db-migrations');
const relativeMigration = 'migrations/V20260718_001_unified_employee_login.sql';
const migrationPath = path.join(migrationsDir, relativeMigration);
const manifestPath = path.join(migrationsDir, 'migration_manifest.txt');
const checksumPath = path.join(migrationsDir, 'migration_checksums.sha256');
const auditScriptPath = path.join(migrationsDir, 'scripts/audit-unified-employee-login.sh');
const databaseHelperPath = path.join(migrationsDir, 'scripts/lib/database.sh');

test('unified employee login migration guards duplicates before adding the tenant phone unique index', () => {
  const sql = fs.readFileSync(migrationPath, 'utf8');

  assert.match(sql, /GROUP BY\s+tenant_code\s*,\s*phone_hash[\s\S]*HAVING COUNT\(\*\) > 1/i);
  assert.match(sql, /SIGNAL SQLSTATE '45000'/);
  assert.match(sql, /uk_user_tenant_phone_hash/);
  assert.doesNotMatch(sql, /DELETE\s+FROM\s+`?user`?/i);

  const guardOffset = sql.search(/SIGNAL SQLSTATE '45000'/i);
  const normalizeOffset = sql.search(/UPDATE\s+`?user`?\s+SET\s+tenant_code\s*=\s*NULL\s+WHERE\s+tenant_code\s*=\s*''/i);
  const alterOffset = sql.search(/ALTER\s+TABLE\s+`?user`?/i);
  const dropProcedureOffset = sql.search(/DROP\s+PROCEDURE\s+IF\s+EXISTS\s+guard_unified_employee_login_phone_uniqueness/i);
  const createProcedureOffset = sql.search(/CREATE\s+PROCEDURE\s+guard_unified_employee_login_phone_uniqueness/i);
  assert.ok(guardOffset >= 0 && guardOffset < alterOffset, 'duplicate guard must precede ALTER TABLE');
  assert.ok(normalizeOffset >= 0 && normalizeOffset < alterOffset,
    'blank tenant codes must be normalized to NULL before ALTER TABLE');
  assert.ok(dropProcedureOffset >= 0 && dropProcedureOffset < createProcedureOffset,
    'retry cleanup must drop an interrupted migration procedure before recreating it');
  assert.match(sql, /index_name\s*=\s*'uk_user_tenant_phone_hash'[\s\S]*non_unique\s*=\s*0/i);
  assert.match(sql, /HAVING\s+COUNT\(\*\)\s*=\s*2/i);
  assert.match(sql, /SUM\(CASE\s+WHEN\s+non_unique\s*=\s*0\s+THEN\s+1\s+ELSE\s+0\s+END\)\s*=\s*2/i);
  assert.match(sql, /SUM\(CASE\s+WHEN\s+expression\s+IS\s+NULL\s+THEN\s+1\s+ELSE\s+0\s+END\)\s*=\s*2/i);
  assert.match(sql, /SUM\(CASE\s+WHEN\s+sub_part\s+IS\s+NULL\s+THEN\s+1\s+ELSE\s+0\s+END\)\s*=\s*2/i);
  assert.match(sql, /GROUP_CONCAT\(column_name\s+ORDER\s+BY\s+seq_in_index\s+SEPARATOR\s+','\)\s*=\s*'tenant_code,phone_hash'/i);
  assert.match(sql, /SIGNAL SQLSTATE '45000'[\s\S]*uk_user_tenant_phone_hash/i);
  assert.doesNotMatch(sql, /INSERT\s+INTO\s+`?user`?/i);
});

test('unified employee login migration remains registered with its checksum', () => {
  const manifest = fs.readFileSync(manifestPath, 'utf8').trim().split(/\r?\n/).filter(Boolean);
  const checksums = fs.readFileSync(checksumPath, 'utf8');
  const migrationChecksum = createHash('sha256').update(fs.readFileSync(migrationPath)).digest('hex');

  assert.equal(manifest.filter((entry) => entry === relativeMigration).length, 1);
  assert.match(checksums, new RegExp(`^${migrationChecksum}  ${relativeMigration.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`, 'm'));
});

test('unified employee login audit reports every remediation category and blocks duplicate real-tenant hashes', () => {
  const script = fs.readFileSync(auditScriptPath, 'utf8');
  const databaseHelper = fs.readFileSync(databaseHelperPath, 'utf8');

  assert.match(script, /source\s+"\$\{SCRIPT_DIR\}\/lib\/database\.sh"/);
  assert.doesNotMatch(script, /^mysql_root_db\(\)/m);
  assert.doesNotMatch(script, /source\s+\.\/\.env/);
  assert.match(databaseHelper, /load_database_env\(\)/);
  assert.match(databaseHelper, /mysql_root_db\(\)/);
  const auditIndexEntry = execFileSync(
    'git',
    ['ls-files', '-s', 'db-migrations/scripts/audit-unified-employee-login.sh'],
    { cwd: path.resolve(migrationsDir, '..'), encoding: 'utf8' },
  );
  assert.match(auditIndexEntry, /^100755\s/);
  assert.match(script, /tenant_code IS NOT NULL AND tenant_code <> ''/i);
  assert.match(script, /phone_hash IS NOT NULL AND phone_hash <> ''/i);
  assert.match(script, /GROUP BY\s+tenant_code\s*,\s*phone_hash[\s\S]*HAVING COUNT\(\*\) > 1/i);
  assert.match(script, /SELECT\s+id\s*,\s*phone_mask\s*,\s*status[\s\S]*tenant_code IS NULL OR tenant_code = ''/i);
  assert.match(script, /Missing employee extension/i);
  assert.match(script, /LEFT JOIN\s+emp_employee_ext\s+ext[\s\S]*ext\.user_id IS NULL/i);
  assert.match(script, /Missing role assignments/i);
  assert.match(script, /NOT EXISTS\s*\([\s\S]*FROM\s+sys_user_role\s+ur[\s\S]*ur\.user_id\s*=\s*u\.id/i);
  assert.match(script, /Invalid employee statuses/i);
  assert.match(script, /u\.status IS NULL OR u\.status NOT IN\s*\(0\s*,\s*1\s*,\s*2\)/i);
  assert.match(script, /Blank phone hashes for login-capable tenant employees/i);
  assert.match(script, /u\.status IN\s*\(1\s*,\s*2\)[\s\S]*u\.phone_hash IS NULL OR u\.phone_hash = ''/i);
  assert.match(script, /fail "Duplicate tenant phone hashes must be resolved before unified employee login migration"/i);
  assert.doesNotMatch(script, /fail "(?:Missing employee extension|Missing role assignments|Invalid employee statuses|Blank phone hashes)/i);
  assert.doesNotMatch(script, /(?:DELETE|UPDATE|INSERT|ALTER|DROP|TRUNCATE)\b/i);
});
