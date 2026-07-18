import assert from 'node:assert/strict';
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

test('unified employee login migration guards duplicates before adding the tenant phone unique index', () => {
  const sql = fs.readFileSync(migrationPath, 'utf8');

  assert.match(sql, /GROUP BY\s+tenant_code\s*,\s*phone_hash[\s\S]*HAVING COUNT\(\*\) > 1/i);
  assert.match(sql, /SIGNAL SQLSTATE '45000'/);
  assert.match(sql, /uk_user_tenant_phone_hash/);
  assert.doesNotMatch(sql, /DELETE\s+FROM\s+`?user`?/i);

  const guardOffset = sql.search(/SIGNAL SQLSTATE '45000'/i);
  const alterOffset = sql.search(/ALTER\s+TABLE\s+`?user`?/i);
  assert.ok(guardOffset >= 0 && guardOffset < alterOffset, 'duplicate guard must precede ALTER TABLE');
  assert.doesNotMatch(sql, /(?:UPDATE|INSERT\s+INTO)\s+`?user`?/i);
});

test('unified employee login migration is appended to the manifest with its checksum', () => {
  const manifest = fs.readFileSync(manifestPath, 'utf8').trim().split(/\r?\n/).filter(Boolean);
  const checksums = fs.readFileSync(checksumPath, 'utf8');

  assert.equal(manifest.at(-1), relativeMigration);
  assert.match(checksums, new RegExp(`^[a-f0-9]{64}  ${relativeMigration.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`, 'm'));
});

test('unified employee login audit script reports tenant-less rows and fails only for duplicate tenant phone hashes', () => {
  const script = fs.readFileSync(auditScriptPath, 'utf8');

  assert.match(script, /source\s+\.\/\.env/);
  assert.match(script, /tenant_code IS NOT NULL AND tenant_code <> ''/i);
  assert.match(script, /phone_hash IS NOT NULL AND phone_hash <> ''/i);
  assert.match(script, /GROUP BY\s+tenant_code\s*,\s*phone_hash[\s\S]*HAVING COUNT\(\*\) > 1/i);
  assert.match(script, /SELECT\s+id\s*,\s*phone_mask\s*,\s*status[\s\S]*tenant_code IS NULL OR tenant_code = ''/i);
  assert.doesNotMatch(script, /(?:DELETE|UPDATE|INSERT|ALTER|DROP|TRUNCATE)\b/i);
});
