# TENANT_002 Removal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a guarded, manual, post-migration workflow that permanently removes `TENANT_002` from MySQL, Redis, and tenant upload directories without touching any other tenant or migration history.

**Architecture:** A Node static contract test defines the deployment safety requirements first. A fixed-target MySQL script performs tenant-row deletion atomically, while a Bash wrapper owns preview mode, backup verification, destructive confirmation, Redis cleanup, upload path checks, and post-cleanup verification. The workflow remains outside `migration_manifest.txt` and is documented as a post-release manual operation.

**Tech Stack:** MySQL 8 stored procedure/dynamic SQL, Bash, Docker Compose, Redis CLI, Node.js test runner.

## Global Constraints

- Never modify `db-migrations/migrations/V20260530_001_second_tenant_seed.sql` or any executed historical migration.
- Never add the destructive cleanup SQL to `db-migrations/migration_manifest.txt` or an automatic restart path.
- The target tenant is fixed to `TENANT_002`; it is not a caller-supplied parameter.
- Running without `CONFIRM_REMOVE_TENANT_002=YES` must be read-only.
- Preview mode must not start or recreate containers, and `.env` must not be able to enable destructive mode.
- A fresh verified MySQL backup must complete before the destructive SQL runs.
- `TENANT_001`, `super`, global permissions, and schema migration history must remain untouched.
- Database deletion must commit atomically or roll back and restore foreign-key checking.
- Database deletion must reject any affected non-InnoDB table before starting the transaction.
- Redis deletion must require `TENANT_002` as an exact colon-delimited key segment; filesystem deletion must use the exact tenant directory name.

---

### Task 1: Define The Deployment Safety Contract

**Files:**
- Create: `D:/HiveManager/management-ui/tests/deploy-tenant-002-cleanup.test.js`

**Interfaces:**
- Consumes: canonical deploy root `C:/Users/HUAWEI/Desktop/hive部署_全新配置`.
- Produces: a failing static contract that later tasks must satisfy.

- [ ] **Step 1: Write the failing test**

Create a Node test that reads the wrapper, SQL, manifest, historical migration, health check, and upload guide. The assertions must include this complete contract:

```js
import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'

const deployRoot = path.resolve('C:/Users/HUAWEI/Desktop/hive部署_全新配置')
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

assert.ok(wrapper.includes('CONFIRM_REMOVE_TENANT_002'), 'cleanup must require explicit confirmation')
assert.ok(wrapper.includes('preview_tenant_data'), 'cleanup must provide read-only preview')
assert.ok(!wrapper.slice(0, wrapper.indexOf('if [ "${operator_confirmation}"')).includes('docker compose up'),
  'preview must not start or recreate containers')
assert.ok(wrapper.indexOf('backup-online.sh') < wrapper.indexOf('mysql_root_db < "${CLEANUP_SQL}"'),
  'backup must run before destructive SQL')
assert.ok(wrapper.includes('verify-latest-backup.sh'), 'backup must be verified')
assert.ok(wrapper.includes("TARGET_TENANT='TENANT_002'"), 'target must be fixed')
assert.ok(wrapper.includes("--pattern '*TENANT_002*'"), 'Redis cleanup must be tenant-scoped')
assert.ok(wrapper.includes("-name \"${TARGET_TENANT}\""), 'upload cleanup must match the exact tenant directory')
assert.ok(wrapper.includes('realpath -m'), 'upload cleanup must verify resolved paths')

assert.ok(cleanupSql.includes("SET @hive_target_tenant := 'TENANT_002'"), 'SQL target must be fixed')
assert.ok(cleanupSql.includes('START TRANSACTION'), 'cleanup must be transactional')
assert.ok(cleanupSql.includes('ROLLBACK'), 'cleanup must roll back on failure')
assert.ok(cleanupSql.includes('RESIGNAL'), 'cleanup must preserve SQL failure')
assert.ok(cleanupSql.includes('information_schema.COLUMNS'), 'cleanup must cover every tenant_code table')
assert.ok(cleanupSql.includes('nontransactional_table_count'), 'cleanup must reject nontransactional tables')
assert.ok(cleanupSql.includes('DELETE role_permission'), 'role permissions require explicit child cleanup')
assert.ok(cleanupSql.lastIndexOf('DELETE FROM `tenant`') > cleanupSql.lastIndexOf('EXECUTE cleanup_stmt'),
  'tenant row must be deleted last')
assert.ok(!cleanupSql.includes("tenant_code = 'TENANT_001'"), 'cleanup must not target TENANT_001')
assert.ok(!cleanupSql.includes("tenant_code = 'super'"), 'cleanup must not target super')

assert.ok(!manifest.includes('V20260711_001_remove_tenant_002.sql'),
  'destructive cleanup must not run as an automatic migration')
assert.equal(createHash('sha256').update(historical).digest('hex'),
  'b9da086e1b5b533b7ceedc629115d26612283b1fadcadcc3c6cbaf23114d5ace',
  'historical second-tenant seed must remain byte-for-byte unchanged')
assert.ok(deployHealth.includes('manual-remove-tenant-002.sh'), 'deploy health must require the wrapper')
assert.ok(deployHealth.includes('V20260711_001_remove_tenant_002.sql'), 'deploy health must require the SQL')
assert.ok(uploadGuide.includes('CONFIRM_REMOVE_TENANT_002=YES'), 'upload guide must document execution')

console.log('TENANT_002 cleanup safety checks passed')
```

- [ ] **Step 2: Record the real historical checksum before running the test**

Run:

```powershell
(Get-FileHash -Algorithm SHA256 'C:\Users\HUAWEI\Desktop\hive部署_全新配置\db-migrations\migrations\V20260530_001_second_tenant_seed.sql').Hash.ToLower()
```

Replace only the checksum literal in the new test if the command returns a different value. Do not edit the historical SQL.

- [ ] **Step 3: Run the test and verify RED**

Run:

```powershell
node --test tests/deploy-tenant-002-cleanup.test.js
```

Expected: FAIL because the wrapper and cleanup SQL do not exist.

---

### Task 2: Implement Atomic Tenant Database Deletion

**Files:**
- Create: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/db-migrations/manual/V20260711_001_remove_tenant_002.sql`
- Test: `D:/HiveManager/management-ui/tests/deploy-tenant-002-cleanup.test.js`

**Interfaces:**
- Consumes: MySQL session connected to the configured Hive business database as root.
- Produces: idempotent removal of all `TENANT_002` rows and explicit role-permission children in one transaction.

- [ ] **Step 1: Create the fixed-target SQL**

Implement the following procedure structure exactly, keeping the target constant and the tenant delete last:

```sql
-- Manual post-migration cleanup for the retired TENANT_002 test tenant.
-- Never add this file to migration_manifest.txt.
SET NAMES utf8mb4;
SET @hive_target_tenant := 'TENANT_002';

DROP PROCEDURE IF EXISTS `manual_remove_tenant_002`;
DELIMITER $$
CREATE PROCEDURE `manual_remove_tenant_002`()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE tenant_table VARCHAR(64);
  DECLARE previous_foreign_key_checks INT DEFAULT 1;
  DECLARE nontransactional_table_count INT DEFAULT 0;
  DECLARE tenant_tables CURSOR FOR
    SELECT column_item.TABLE_NAME
    FROM information_schema.COLUMNS column_item
    INNER JOIN information_schema.TABLES table_item
      ON table_item.TABLE_SCHEMA = column_item.TABLE_SCHEMA
     AND table_item.TABLE_NAME = column_item.TABLE_NAME
     AND table_item.TABLE_TYPE = 'BASE TABLE'
    WHERE column_item.TABLE_SCHEMA = DATABASE()
      AND column_item.COLUMN_NAME = 'tenant_code'
      AND column_item.TABLE_NAME <> 'tenant'
    ORDER BY column_item.TABLE_NAME;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    SET FOREIGN_KEY_CHECKS = previous_foreign_key_checks;
    RESIGNAL;
  END;

  IF NOT (BINARY @hive_target_tenant = BINARY 'TENANT_002') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Refusing cleanup for an unexpected tenant';
  END IF;

  -- Reject affected non-InnoDB tables before START TRANSACTION.
  SET previous_foreign_key_checks = @@FOREIGN_KEY_CHECKS;
  START TRANSACTION;
  SET FOREIGN_KEY_CHECKS = 0;

  DELETE role_permission
  FROM `sys_role_permission` role_permission
  INNER JOIN `sys_role` role_item ON role_item.`id` = role_permission.`role_id`
  WHERE BINARY role_item.`tenant_code` = BINARY 'TENANT_002';

  OPEN tenant_tables;
  tenant_loop: LOOP
    FETCH tenant_tables INTO tenant_table;
    IF done = 1 THEN
      LEAVE tenant_loop;
    END IF;
    IF tenant_table NOT REGEXP '^[A-Za-z0-9_]+$' THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unsafe tenant table identifier';
    END IF;
    SET @cleanup_sql = CONCAT(
      'DELETE FROM `', tenant_table,
      '` WHERE BINARY `tenant_code` = BINARY ''TENANT_002'''
    );
    PREPARE cleanup_stmt FROM @cleanup_sql;
    EXECUTE cleanup_stmt;
    DEALLOCATE PREPARE cleanup_stmt;
  END LOOP;
  CLOSE tenant_tables;

  DELETE FROM `tenant` WHERE BINARY `tenant_code` = BINARY 'TENANT_002';
  COMMIT;
  SET FOREIGN_KEY_CHECKS = previous_foreign_key_checks;
END$$
DELIMITER ;

CALL `manual_remove_tenant_002`();
DROP PROCEDURE IF EXISTS `manual_remove_tenant_002`;
```

- [ ] **Step 2: Run the focused test**

Run:

```powershell
node --test tests/deploy-tenant-002-cleanup.test.js
```

Expected: still FAIL because the wrapper and documentation are not implemented yet; SQL-specific assertions pass.

---

### Task 3: Implement Preview, Backup, Cache, And Upload Cleanup

**Files:**
- Create: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/scripts/manual-remove-tenant-002.sh`
- Test: `D:/HiveManager/management-ui/tests/deploy-tenant-002-cleanup.test.js`

**Interfaces:**
- Consumes: `.env`, Docker Compose services `mysql` and `redis`, Task 2 SQL, backup scripts, `/root/hive/uploads`.
- Produces: read-only preview by default and confirmed end-to-end cleanup with zero-residue verification.

- [ ] **Step 1: Implement fixed configuration and MySQL helpers**

The script must start with:

```bash
#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

TARGET_TENANT='TENANT_002'
CLEANUP_SQL='db-migrations/manual/V20260711_001_remove_tenant_002.sql'

fail() { echo "FAIL: $1" >&2; exit 1; }

test -f .env || fail 'missing .env'
set -a
source ./.env
set +a

DATABASE_NAME="${DATABASE_NAME:-hive}"
echo "${DATABASE_NAME}" | grep -Eq '^[A-Za-z0-9_]+$' || fail 'unsafe DATABASE_NAME'
test -n "${MYSQL_ROOT_PASSWORD:-}" || fail '.env missing MYSQL_ROOT_PASSWORD'
test -f "${CLEANUP_SQL}" || fail "missing ${CLEANUP_SQL}"

mysql_root_db() {
  docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" \
    --default-character-set=utf8mb4 "${DATABASE_NAME}" "$@"
}

mysql_scalar() {
  mysql_root_db --batch --skip-column-names --execute "$1" | tr -d '\r'
}
```

- [ ] **Step 2: Implement read-only preview and residue counting**

Add `load_tenant_tables`, `preview_tenant_data`, and `tenant_residue_count`. Discovery must read only `information_schema`, validate every identifier against `^[A-Za-z0-9_]+$`, print non-zero table counts, include the `tenant` row and explicit `sys_role_permission` count, and assign the numeric sum to `TENANT_RESIDUE_TOTAL`.

Use these exact SQL predicates:

```bash
"SELECT TABLE_NAME FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = '${DATABASE_NAME}'
   AND COLUMN_NAME = 'tenant_code'
 ORDER BY TABLE_NAME"

"SELECT COUNT(*) FROM \`${table_name}\`
 WHERE BINARY \`tenant_code\` = BINARY '${TARGET_TENANT}'"

"SELECT COUNT(*) FROM sys_role_permission role_permission
 INNER JOIN sys_role role_item ON role_item.id = role_permission.role_id
 WHERE BINARY role_item.tenant_code = BINARY '${TARGET_TENANT}'"
```

- [ ] **Step 3: Gate destructive mode and run verified backup**

The main flow must preview first, then exit successfully unless the exact confirmation is present:

```bash
wait_for_mysql
wait_for_redis
preview_tenant_data

if [ "${operator_confirmation}" != 'YES' ]; then
  echo 'Preview only. Re-run with CONFIRM_REMOVE_TENANT_002=YES to delete.'
  exit 0
fi

echo 'Creating and verifying a database backup before TENANT_002 cleanup...'
DEPLOY_DIR="$(pwd)" DATABASE_NAME="${DATABASE_NAME}" bash db-migrations/scripts/backup-online.sh
DATABASE_NAME="${DATABASE_NAME}" MAX_BACKUP_AGE_HOURS="${BACKUP_VERIFY_MAX_AGE_HOURS:-2}" \
  bash scripts/verify-latest-backup.sh

mysql_root_db < "${CLEANUP_SQL}"
preview_tenant_data
[ "${TENANT_RESIDUE_TOTAL}" = '0' ] || fail "database residue remains: ${TENANT_RESIDUE_TOTAL}"
```

- [ ] **Step 4: Add exact Redis cleanup**

Build a `redis_cli` array with `REDISCLI_AUTH` only when `REDIS_PASSWORD` is non-empty. Scan with the literal pattern `*TENANT_002*`, call `UNLINK` once per returned key, then scan again and fail if any key remains.

```bash
redis_cli=(docker compose exec -T)
if [ -n "${REDIS_PASSWORD:-}" ]; then
  redis_cli+=(-e "REDISCLI_AUTH=${REDIS_PASSWORD}")
fi
redis_cli+=(redis redis-cli --raw)

while IFS= read -r redis_key; do
  [ -n "${redis_key}" ] || continue
  redis_key_has_target_segment "${redis_key}" || continue
  "${redis_cli[@]}" UNLINK "${redis_key}" >/dev/null
done < <("${redis_cli[@]}" --scan --pattern '*TENANT_002*')

remaining_redis_key="$("${redis_cli[@]}" --scan --pattern '*TENANT_002*' | head -n 1)"
[ -z "${remaining_redis_key}" ] || fail 'TENANT_002 Redis residue remains'
```

- [ ] **Step 5: Add upload path-boundary cleanup**

Resolve the deployment root and fixed upload root. Delete only directories whose basename is exactly `TENANT_002` and whose resolved path starts with the resolved upload root plus `/`.

```bash
deploy_root="$(realpath -m "$(pwd)")"
upload_root="$(realpath -m "${deploy_root}/uploads")"
[ "${upload_root}" = "${deploy_root}/uploads" ] || fail 'unexpected upload root'

if [ -d "${upload_root}" ]; then
  while IFS= read -r -d '' candidate; do
    resolved_candidate="$(realpath -m "${candidate}")"
    [ "$(basename "${resolved_candidate}")" = "${TARGET_TENANT}" ] || fail 'unexpected upload candidate'
    case "${resolved_candidate}" in
      "${upload_root}"/*) ;;
      *) fail "upload candidate escaped root: ${resolved_candidate}" ;;
    esac
    rm -rf -- "${resolved_candidate}"
  done < <(find "${upload_root}" -depth -type d -name "${TARGET_TENANT}" -print0)
fi
```

Finish by verifying no exact tenant directory remains and print `TENANT_002 cleanup finished.`.

- [ ] **Step 6: Run the focused test**

Run:

```powershell
node --test tests/deploy-tenant-002-cleanup.test.js
```

Expected: still FAIL only for health-check or documentation assertions.

---

### Task 4: Wire Deployment Checks And Operator Instructions

**Files:**
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/scripts/check-deploy-health.sh`
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/UPLOAD_STEPS.md`
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/RELEASE_NOTES.md`
- Test: `D:/HiveManager/management-ui/tests/deploy-tenant-002-cleanup.test.js`

**Interfaces:**
- Consumes: Task 2 SQL and Task 3 wrapper.
- Produces: package-time presence checks and exact post-migration operator commands.

- [ ] **Step 1: Require both cleanup artifacts in deploy health**

Add these checks next to the existing manual cleanup requirements:

```bash
require_file "scripts/manual-remove-tenant-002.sh"
require_file "db-migrations/manual/V20260711_001_remove_tenant_002.sql"
grep -q "CONFIRM_REMOVE_TENANT_002" scripts/manual-remove-tenant-002.sh \
  || fail "TENANT_002 cleanup script missing explicit confirmation gate"
ok "Manual TENANT_002 cleanup workflow is present"
```

- [ ] **Step 2: Document preview and destructive commands**

Add a post-release section to `UPLOAD_STEPS.md`:

````markdown
## 清理已废弃的 TENANT_002 测试租户

必须先完成迁移和功能验收。先只读预览：

```bash
cd /root/hive
bash scripts/manual-remove-tenant-002.sh
```

确认预览只包含测试租户后执行：

```bash
CONFIRM_REMOVE_TENANT_002=YES bash scripts/manual-remove-tenant-002.sh
```

最终必须看到 `TENANT_002 cleanup finished.`。不要修改或删除历史迁移 `V20260530_001_second_tenant_seed.sql`。
````

Add the same ordering and one-time cleanup requirement to `RELEASE_NOTES.md` without duplicating shell implementation details.

- [ ] **Step 3: Run the focused test and verify GREEN**

Run:

```powershell
node --test tests/deploy-tenant-002-cleanup.test.js
```

Expected: PASS and output `TENANT_002 cleanup safety checks passed`.

- [ ] **Step 4: Commit the deploy contract**

```powershell
git add management-ui/tests/deploy-tenant-002-cleanup.test.js docs/superpowers/plans/2026-07-11-tenant-002-removal.md
git commit -m "test: guard tenant 002 cleanup workflow"
```

---

### Task 5: Run Full Release Verification

**Files:**
- Verify only; no new files expected.

**Interfaces:**
- Consumes: all prior tasks.
- Produces: evidence that the deploy package remains safe and internally consistent.

- [ ] **Step 1: Run all management/deploy Node tests**

```powershell
$tests = Get-ChildItem -LiteralPath 'tests' -Filter '*.test.js' -File | Sort-Object Name
node --test $tests.FullName
```

Expected: all tests pass with zero failures.

- [ ] **Step 2: Verify low-cost deployment mode**

```powershell
& 'C:\Users\HUAWEI\Desktop\hive部署_全新配置\scripts\verify-low-cost-mode.ps1'
```

Expected: `Low-cost single-machine verification passed.`

- [ ] **Step 3: Recheck historical migration immutability and manifest exclusion**

```powershell
(Get-FileHash -Algorithm SHA256 'C:\Users\HUAWEI\Desktop\hive部署_全新配置\db-migrations\migrations\V20260530_001_second_tenant_seed.sql').Hash.ToLower()
Select-String -Path 'C:\Users\HUAWEI\Desktop\hive部署_全新配置\db-migrations\migration_manifest.txt' -Pattern 'remove_tenant_002'
```

Expected: checksum equals the test constant and `Select-String` returns no match.

- [ ] **Step 4: Verify repository state and push**

```powershell
git diff --check
git status --short
git push origin dev
```

Expected: no unstaged changes after commit and remote `dev` advances to the cleanup contract commit.

- [ ] **Step 5: Record the server-only acceptance commands**

Do not claim tenant deletion locally. The server operator must run:

```bash
cd /root/hive
bash scripts/manual-remove-tenant-002.sh
CONFIRM_REMOVE_TENANT_002=YES bash scripts/manual-remove-tenant-002.sh
```

Expected: preview first, verified backup, destructive cleanup, zero residue, then `TENANT_002 cleanup finished.`.
