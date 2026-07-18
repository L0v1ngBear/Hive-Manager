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

if [ -n "${duplicate_rows}" ]; then
  fail "Duplicate tenant phone hashes must be resolved before unified employee login migration"
fi
