#!/usr/bin/env bash
set -euo pipefail

# Explicit, one-time initialization for a new single-tenant Hive database.
DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
DATABASE_NAME="${DATABASE_NAME:-hive}"
CONFIRM_FRESH_DATABASE_INITIALIZATION="${CONFIRM_FRESH_DATABASE_INITIALIZATION:-NO}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MIGRATION_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BASELINE_FILE="${MIGRATION_DIR}/baseline/hive_schema_baseline_v2.sql"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

sql_escape() {
  printf '%s' "$1" | sed "s/'/''/g"
}

md5_password() {
  if command -v md5sum >/dev/null 2>&1; then
    printf '%s' "$1" | md5sum | awk '{print $1}'
  else
    printf '%s' "$1" | openssl dgst -md5 | awk '{print $NF}'
  fi
}

mysql_root_db() {
  docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 "$@" "${DATABASE_NAME}"
}

[ "${CONFIRM_FRESH_DATABASE_INITIALIZATION}" = "YES" ] || fail "Set CONFIRM_FRESH_DATABASE_INITIALIZATION=YES for a confirmed empty database"
cd "${DEPLOY_DIR}"
[ -f .env ] || fail "Missing ${DEPLOY_DIR}/.env"
[ -f "${BASELINE_FILE}" ] || fail "Missing ${BASELINE_FILE}"

set -a
source ./.env
set +a

[ "${HIVE_ALLOWED_TENANT_CODES:-}" = "TENANT_001" ] || fail "HIVE_ALLOWED_TENANT_CODES must be TENANT_001"
[ "${HIVE_DEFAULT_TENANT_CODE:-}" = "TENANT_001" ] || fail "HIVE_DEFAULT_TENANT_CODE must be TENANT_001"
[ "${HIVE_TENANT_MAX_COUNT:-1}" = "1" ] || fail "HIVE_TENANT_MAX_COUNT must be 1"
[ -n "${TENANT_OWNER_DEFAULT_PASSWORD:-}" ] || fail ".env missing TENANT_OWNER_DEFAULT_PASSWORD"

tenant_name="${HIVE_SINGLE_TENANT_NAME:-当前组织}"
owner_login="${FRESH_TENANT_OWNER_LOGIN:-owner}"
owner_name="${FRESH_TENANT_OWNER_NAME:-企业管理员}"
[ -n "${owner_login}" ] || fail "FRESH_TENANT_OWNER_LOGIN cannot be empty"
[ -n "${owner_name}" ] || fail "FRESH_TENANT_OWNER_NAME cannot be empty"

DATABASE_NAME="${DATABASE_NAME}" ALLOW_FRESH_DATABASE=YES bash "${SCRIPT_DIR}/check-database-state.sh"
table_count="$(docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DATABASE_NAME}';")"
[ "${table_count}" = "0" ] || fail "Fresh initialization only accepts an empty database; found ${table_count} tables"

echo "1/4 Stop backend writes..."
docker compose stop backend 2>/dev/null || true

echo "2/4 Import immutable current baseline and active permission catalog..."
TARGET_DATABASE="${DATABASE_NAME}" RESET_TARGET=YES CONFIRM_IMPORT_TO_HIVE=YES \
  BASELINE_FILE="${BASELINE_FILE}" bash "${SCRIPT_DIR}/import-baseline-to-shadow.sh"

echo "3/4 Create TENANT_001 owner and administrator role..."
tenant_name_sql="$(sql_escape "${tenant_name}")"
owner_login_sql="$(sql_escape "${owner_login}")"
owner_name_sql="$(sql_escape "${owner_name}")"
owner_password_hash="$(md5_password "${TENANT_OWNER_DEFAULT_PASSWORD}")"
mysql_root_db <<EOSQL
INSERT INTO tenant
  (tenant_code, tenant_name, tenant_type, status, package_code, package_name,
   subscription_status, max_users, max_storage_mb, deleted)
VALUES
  ('TENANT_001', '${tenant_name_sql}', 1, 1, 'STANDARD', '标准版', 'ACTIVE', 200, 10240, 0);

INSERT INTO user
  (tenant_code, name, login_name, password, must_change_password, status,
   attendance_required, role_level, permission_version, auth_version)
VALUES
  ('TENANT_001', '${owner_name_sql}', '${owner_login_sql}', '${owner_password_hash}', 1, 1, 0, 100, 1, 1);

INSERT INTO sys_role (tenant_code, role_code, role_name, is_system, is_deleted)
VALUES ('TENANT_001', 'ADMIN', '系统管理员', 1, 0);

SET @owner_user_id = (SELECT id FROM user WHERE tenant_code='TENANT_001' AND login_name='${owner_login_sql}' LIMIT 1);
SET @admin_role_id = (SELECT id FROM sys_role WHERE tenant_code='TENANT_001' AND role_code='ADMIN' LIMIT 1);

INSERT INTO sys_user_role (user_id, tenant_code, role_id, is_deleted)
VALUES (@owner_user_id, 'TENANT_001', @admin_role_id, 0);

INSERT INTO sys_role_permission (role_id, permission_id, is_deleted)
SELECT @admin_role_id, p.id, 0
FROM sys_permission p
WHERE p.assignable=1 AND p.status=1 AND p.is_deleted=0;
EOSQL

echo "4/4 Verify schema and bootstrap data..."
DATABASE_NAME="${DATABASE_NAME}" BASELINE_FILE="${BASELINE_FILE}" bash "${SCRIPT_DIR}/verify-online-schema.sh"
bootstrap_state="$(mysql_root_db -N -B -e "
SELECT CONCAT(
  (SELECT COUNT(*) FROM tenant WHERE tenant_code='TENANT_001' AND deleted=0), '|',
  (SELECT COUNT(*) FROM user WHERE tenant_code='TENANT_001' AND login_name='${owner_login_sql}'), '|',
  (SELECT COUNT(*) FROM sys_permission WHERE assignable=1 AND status=1 AND is_deleted=0), '|',
  (SELECT COUNT(*) FROM sys_role_permission WHERE role_id=(SELECT id FROM sys_role WHERE tenant_code='TENANT_001' AND role_code='ADMIN' LIMIT 1) AND is_deleted=0)
);")"
IFS='|' read -r tenant_count owner_count permission_count grant_count <<< "${bootstrap_state}"
[ "${tenant_count}" = "1" ] || fail "TENANT_001 bootstrap verification failed"
[ "${owner_count}" = "1" ] || fail "Owner bootstrap verification failed"
[ "${permission_count}" -gt 0 ] || fail "Permission catalog bootstrap verification failed"
[ "${grant_count}" = "${permission_count}" ] || fail "Administrator permission grants are incomplete"

echo "Fresh Hive database initialized. Start backend only after reviewing the verification output."
echo "The owner must change the temporary password at first login."
