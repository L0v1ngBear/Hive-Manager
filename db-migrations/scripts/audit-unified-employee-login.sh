#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
DATABASE_NAME="${DATABASE_NAME:-hive}"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

mysql_root_db() {
  docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" \
    --default-character-set=utf8mb4 "$@" "${DATABASE_NAME}"
}

cd "${DEPLOY_DIR}"
test -f ".env" || fail "Missing ${DEPLOY_DIR}/.env"
set -a
source ./.env
set +a
test -n "${MYSQL_ROOT_PASSWORD:-}" || fail ".env missing MYSQL_ROOT_PASSWORD"

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
