#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
DATABASE_NAME="${DATABASE_NAME:-hive}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

source "${SCRIPT_DIR}/lib/database.sh"
load_database_env || fail "Unable to load database connection settings"

echo "Duplicate tenant phone hashes:"
duplicate_rows="$(mysql_root_db -N -B <<'EOSQL'
SELECT tenant_code, phone_hash, COUNT(*) AS duplicate_count
FROM user
WHERE tenant_code IS NOT NULL AND tenant_code <> ''
  AND phone_hash IS NOT NULL AND phone_hash <> ''
GROUP BY tenant_code, phone_hash
HAVING COUNT(*) > 1;
EOSQL
)"
if [ -n "${duplicate_rows}" ]; then
  printf '%s\n' "${duplicate_rows}"
else
  echo "None"
fi

echo "Tenant-less users requiring manual reconciliation:"
mysql_root_db <<'EOSQL'
SELECT id, phone_mask, status
FROM user
WHERE tenant_code IS NULL OR tenant_code = '';
EOSQL

echo "Missing employee extension rows:"
mysql_root_db <<'EOSQL'
SELECT u.id, u.tenant_code, u.phone_mask, u.status
FROM user u
LEFT JOIN emp_employee_ext ext
  ON ext.user_id = u.id
 AND ext.tenant_code = u.tenant_code
 AND IFNULL(ext.is_deleted, 0) = 0
WHERE u.tenant_code IS NOT NULL
  AND u.tenant_code <> ''
  AND ext.user_id IS NULL;
EOSQL

echo "Missing role assignments:"
mysql_root_db <<'EOSQL'
SELECT u.id, u.tenant_code, u.phone_mask, u.status
FROM user u
WHERE u.tenant_code IS NOT NULL
  AND u.tenant_code <> ''
  AND NOT EXISTS (
    SELECT 1
    FROM sys_user_role ur
    WHERE ur.user_id = u.id
      AND ur.tenant_code = u.tenant_code
      AND IFNULL(ur.is_deleted, 0) = 0
  );
EOSQL

echo "Invalid employee statuses:"
mysql_root_db <<'EOSQL'
SELECT u.id, u.tenant_code, u.phone_mask, u.status
FROM user u
WHERE u.tenant_code IS NOT NULL
  AND u.tenant_code <> ''
  AND (u.status IS NULL OR u.status NOT IN (0, 1, 2));
EOSQL

echo "Blank phone hashes for login-capable tenant employees:"
mysql_root_db <<'EOSQL'
SELECT u.id, u.tenant_code, u.phone_mask, u.status
FROM user u
WHERE u.tenant_code IS NOT NULL
  AND u.tenant_code <> ''
  AND u.status IN (1, 2)
  AND (u.phone_hash IS NULL OR u.phone_hash = '');
EOSQL

if [ -n "${duplicate_rows}" ]; then
  fail "Duplicate tenant phone hashes must be resolved before unified employee login migration"
fi
