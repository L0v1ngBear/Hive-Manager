#!/bin/bash
set -euo pipefail

# 迁移后校验。
# 如果传入 BASELINE_DATABASE，会对比 baseline 和目标库的表/字段差异。

DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
DATABASE_NAME="${DATABASE_NAME:-hive}"
BASELINE_DATABASE="${BASELINE_DATABASE:-}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MIGRATION_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
MANIFEST_FILE="${MIGRATION_MANIFEST:-${MIGRATION_DIR}/migration_manifest.txt}"
BASELINE_FILE="${BASELINE_FILE:-${MIGRATION_DIR}/baseline/hive_schema_baseline.sql}"
BASELINE_MIGRATION_VERSION="baseline/hive_schema_baseline"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

hash_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
    return
  fi
  if command -v openssl >/dev/null 2>&1; then
    openssl dgst -sha256 "$1" | awk '{print $2}'
    return
  fi
  fail "Missing sha256sum/openssl; cannot verify baseline migration state"
}

mysql_root_no_db() {
  docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 "$@"
}

mysql_root_db() {
  local db_name="$1"
  shift
  docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 "$@" "${db_name}"
}

cd "${DEPLOY_DIR}"
test -f "${MANIFEST_FILE}" || fail "Missing migration manifest: ${MANIFEST_FILE}"
test -f ".env" || fail "缺少 ${DEPLOY_DIR}/.env。"

set -a
source ./.env
set +a

test -n "${MYSQL_ROOT_PASSWORD:-}" || fail ".env 缺少 MYSQL_ROOT_PASSWORD。"

echo "1/4 检查目标库表数量..."
mysql_root_no_db -e "
SELECT table_schema, COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema IN ('${DATABASE_NAME}', '${BASELINE_DATABASE}', 'xxl_job')
GROUP BY table_schema;
"

echo "2/4 检查迁移失败记录..."
if mysql_root_db "${DATABASE_NAME}" -N -B -e "SHOW TABLES LIKE 'schema_migration_history';" | grep -q schema_migration_history; then
  failed_migrations="$(mysql_root_db "${DATABASE_NAME}" -N -B -e "SELECT COUNT(*) FROM schema_migration_history WHERE status <> 'SUCCESS';")"
  if [ "${failed_migrations}" != "0" ]; then
    mysql_root_db "${DATABASE_NAME}" -e "
SELECT version, status, LEFT(COALESCE(error_message, ''), 200) AS error_message, updated_at
FROM schema_migration_history
WHERE status <> 'SUCCESS'
ORDER BY id DESC;
"
    fail "Found failed database migrations"
  fi
else
  fail "Missing schema_migration_history table"
fi

baseline_marker_count="$(mysql_root_db "${DATABASE_NAME}" -N -B -e "SELECT COUNT(*) FROM schema_migration_history WHERE version = '${BASELINE_MIGRATION_VERSION}';")"
if [ "${baseline_marker_count}" = "1" ]; then
  if [ -f "${BASELINE_FILE}.gz" ] && [ ! -f "${BASELINE_FILE}" ]; then
    REAL_BASELINE_FILE="${BASELINE_FILE}.gz"
  else
    REAL_BASELINE_FILE="${BASELINE_FILE}"
  fi
  test -f "${REAL_BASELINE_FILE}" || fail "Missing baseline used by registered baseline state: ${REAL_BASELINE_FILE}"

  expected_baseline_checksum="$(hash_file "${REAL_BASELINE_FILE}")"
  actual_baseline_state="$(mysql_root_db "${DATABASE_NAME}" -N -B -e "SELECT CONCAT(status, '|', checksum_sha256) FROM schema_migration_history WHERE version = '${BASELINE_MIGRATION_VERSION}' LIMIT 1;")"
  if [ "${actual_baseline_state}" != "SUCCESS|${expected_baseline_checksum}" ]; then
    fail "Registered baseline checksum/status does not match the packaged baseline"
  fi

  while IFS= read -r raw_line || [ -n "${raw_line}" ]; do
    entry="$(printf "%s" "${raw_line}" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    if [ -z "${entry}" ] || [[ "${entry}" == \#* ]]; then
      continue
    fi
    migration_file="${MIGRATION_DIR}/${entry}"
    test -f "${migration_file}" || fail "Missing migration represented by baseline state: ${entry}"
    migration_version="${entry%.sql}"
    expected_migration_checksum="$(hash_file "${migration_file}")"
    actual_migration_state="$(mysql_root_db "${DATABASE_NAME}" -N -B -e "SELECT CONCAT(status, '|', checksum_sha256) FROM schema_migration_history WHERE version = '${migration_version}' LIMIT 1;" </dev/null)"
    if [ "${actual_migration_state}" != "SUCCESS|${expected_migration_checksum}" ]; then
      fail "Registered baseline migration state does not match packaged migration: ${entry}"
    fi
  done < "${MANIFEST_FILE}"
  echo "Baseline new-install migration state is checksummed and complete."
elif [ "${baseline_marker_count}" = "0" ]; then
  echo "Historical-upgrade migration state detected."
else
  fail "Duplicate baseline migration markers found"
fi

order_information_channel_migration_count="$(mysql_root_db "${DATABASE_NAME}" -N -B -e "SELECT COUNT(*) FROM schema_migration_history WHERE version = 'migrations/V20260713_001_order_information_channel_and_cancel_reason' AND status = 'SUCCESS';")"
if [ "${order_information_channel_migration_count}" != "1" ]; then
  fail "Order information-channel migration is not SUCCESS"
fi

# V20260713_003_permission_catalog_v3.sql
permission_v3_migration_count="$(mysql_root_db "${DATABASE_NAME}" -N -B -e "SELECT COUNT(*) FROM schema_migration_history WHERE version = 'migrations/V20260713_003_permission_catalog_v3' AND status = 'SUCCESS';")"
if [ "${permission_v3_migration_count}" != "1" ]; then
  fail "Permission catalog V3 migration is not SUCCESS"
fi

echo "3/4 检查关键字段..."
xxl_job_schema_count="$(mysql_root_no_db -N -B -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = 'xxl_job';")"
if [ "${xxl_job_schema_count}" != "1" ]; then
  fail "Missing required database: xxl_job"
fi

xxl_job_table_count="$(mysql_root_no_db -N -B -e "
SELECT COUNT(*)
FROM information_schema.tables
WHERE table_schema = 'xxl_job'
  AND table_name IN (
    'xxl_job_group',
    'xxl_job_registry',
    'xxl_job_info',
    'xxl_job_logglue',
    'xxl_job_log',
    'xxl_job_log_report',
    'xxl_job_lock',
    'xxl_job_user'
  );
")"
if [ "${xxl_job_table_count}" != "8" ]; then
  fail "Missing required XXL-JOB tables in xxl_job database"
fi

missing_columns="$(mysql_root_db "${DATABASE_NAME}" -N -B <<'EOSQL'
WITH expected_columns AS (
  SELECT 'notification_record' AS table_name, 'task_status' AS column_name UNION ALL
  SELECT 'notification_record', 'close_result' UNION ALL
  SELECT 'notification_record', 'close_note' UNION ALL
  SELECT 'notification_record', 'close_user_id' UNION ALL
  SELECT 'notification_record', 'close_time' UNION ALL
  SELECT 'enterprise_announcement', 'announcement_code' UNION ALL
  SELECT 'enterprise_announcement', 'title' UNION ALL
  SELECT 'enterprise_announcement', 'content' UNION ALL
  SELECT 'enterprise_announcement_read', 'announcement_id' UNION ALL
  SELECT 'enterprise_announcement_read', 'read_flag' UNION ALL
  SELECT 'installation_task', 'order_id' UNION ALL
  SELECT 'installation_task', 'installation_status' UNION ALL
  SELECT 'installation_task', 'express_company' UNION ALL
  SELECT 'installation_task', 'express_no' UNION ALL
  SELECT 'installation_task', 'construction_personnel' UNION ALL
  SELECT 'installation_task', 'special_exception_note' UNION ALL
  SELECT 'installation_task', 'attachment_url' UNION ALL
  SELECT 'installation_task', 'accepted_time' UNION ALL
  SELECT 'sales_order', 'information_channel' UNION ALL
  SELECT 'sales_order', 'cancel_reason' UNION ALL
  SELECT 'production_order', 'information_channel' UNION ALL
  SELECT 'installation_task', 'information_channel' UNION ALL
  SELECT 'sys_permission', 'module_code' UNION ALL
  SELECT 'sys_permission', 'assignable' UNION ALL
  SELECT 'sys_permission', 'status' UNION ALL
  SELECT 'user', 'permission_version' UNION ALL
  SELECT 'user', 'auth_version' UNION ALL
  SELECT 'system_event', 'event_key' UNION ALL
  SELECT 'system_event', 'source_app' UNION ALL
  SELECT 'system_event', 'event_type' UNION ALL
  SELECT 'system_event', 'level' UNION ALL
  SELECT 'system_event', 'detail_json' UNION ALL
  SELECT 'tenant', 'package_code' UNION ALL
  SELECT 'tenant', 'package_name' UNION ALL
  SELECT 'tenant', 'subscription_status' UNION ALL
  SELECT 'tenant', 'subscription_start_time' UNION ALL
  SELECT 'tenant', 'subscription_end_time' UNION ALL
  SELECT 'tenant', 'max_users' UNION ALL
  SELECT 'tenant', 'max_storage_mb' UNION ALL
  SELECT 'tenant', 'feature_flags' UNION ALL
  SELECT 'tenant_usage_meter', 'tenant_code' UNION ALL
  SELECT 'tenant_usage_meter', 'meter_type' UNION ALL
  SELECT 'tenant_usage_meter', 'period_key' UNION ALL
  SELECT 'tenant_usage_meter', 'used_count' UNION ALL
  SELECT 'tenant_usage_meter', 'limit_count' UNION ALL
  SELECT 'document', 'original_name' UNION ALL
  SELECT 'document', 'storage_provider' UNION ALL
  SELECT 'document', 'storage_bucket' UNION ALL
  SELECT 'document', 'storage_object_key' UNION ALL
  SELECT 'document', 'file_hash' UNION ALL
  SELECT 'document', 'etag' UNION ALL
  SELECT 'document', 'upload_status' UNION ALL
  SELECT 'tenant_field_config', 'tenant_code' UNION ALL
  SELECT 'tenant_field_config', 'module_code' UNION ALL
  SELECT 'tenant_field_config', 'field_key' UNION ALL
  SELECT 'tenant_field_config', 'field_label' UNION ALL
  SELECT 'tenant_field_config', 'visible_flag' UNION ALL
  SELECT 'tenant_field_config', 'required_flag' UNION ALL
  SELECT 'tenant_field_config', 'sort_no' UNION ALL
  SELECT 'tenant_field_config', 'field_type' UNION ALL
  SELECT 'tenant_field_config', 'options_json' UNION ALL
  SELECT 'cloth', 'custom_fields_json'
)
SELECT e.table_name, e.column_name
FROM expected_columns e
LEFT JOIN information_schema.columns c
  ON c.table_schema = DATABASE()
 AND c.table_name = e.table_name
 AND c.column_name = e.column_name
WHERE c.column_name IS NULL;
EOSQL
)"
if [ -n "${missing_columns}" ]; then
  echo "${missing_columns}"
  fail "Missing required schema columns"
fi

invalid_order_columns="$(mysql_root_db "${DATABASE_NAME}" -N -B <<'EOSQL'
WITH expected_columns AS (
  SELECT 'sales_order' AS table_name, 'information_channel' AS column_name, 100 AS expected_length UNION ALL
  SELECT 'production_order', 'information_channel', 100 UNION ALL
  SELECT 'installation_task', 'information_channel', 100 UNION ALL
  SELECT 'sales_order', 'cancel_reason', 500
)
SELECT e.table_name, e.column_name, c.data_type, c.character_maximum_length,
       c.is_nullable, c.column_default
FROM expected_columns e
LEFT JOIN information_schema.columns c
  ON c.table_schema = DATABASE()
 AND c.table_name = e.table_name
 AND c.column_name = e.column_name
WHERE c.column_name IS NULL
   OR c.data_type <> 'varchar'
   OR (e.column_name = 'information_channel' AND c.character_maximum_length <> 100)
   OR (e.column_name = 'cancel_reason' AND c.character_maximum_length <> 500)
   OR c.is_nullable <> 'YES'
   OR c.column_default IS NOT NULL;
EOSQL
)"
if [ -n "${invalid_order_columns}" ]; then
  echo "${invalid_order_columns}"
  fail "Invalid order column definitions"
fi

legacy_delivery_date_columns="$(mysql_root_db "${DATABASE_NAME}" -N -B <<'EOSQL'
WITH retired_columns (table_name, column_name) AS (
  SELECT 'sales_order', 'delivery_date' UNION ALL
  SELECT 'production_order', 'delivery_date' UNION ALL
  SELECT 'installation_task', 'delivery_date'
)
SELECT r.table_name, r.column_name
FROM retired_columns r
JOIN information_schema.columns c
  ON c.table_schema = DATABASE()
 AND c.table_name = r.table_name
 AND c.column_name = r.column_name;
EOSQL
)"
if [ -n "${legacy_delivery_date_columns}" ]; then
  echo "${legacy_delivery_date_columns}"
  fail "Legacy delivery_date columns remain"
fi

permission_v3_table_count="$(mysql_root_db "${DATABASE_NAME}" -N -B -e "
SELECT COUNT(*)
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('sys_permission_catalog', 'order_responsibility');
")"
if [ "${permission_v3_table_count}" != "2" ]; then
  fail "Missing permission V3 tables: sys_permission_catalog/order_responsibility"
fi

permission_v3_catalog_version="$(mysql_root_db "${DATABASE_NAME}" -N -B -e "SELECT catalog_version FROM sys_permission_catalog WHERE id = 1 LIMIT 1;")"
if [ "${permission_v3_catalog_version}" != "3" ]; then
  fail "Permission catalog version is not 3"
fi

assignable_wildcard_count="$(mysql_root_db "${DATABASE_NAME}" -N -B -e "
SELECT COUNT(*)
FROM sys_permission
WHERE is_deleted = 0
  AND status = 1
  AND assignable = 1
  AND (perm_code IN ('*', '*:*') OR perm_code LIKE '%:*');
")"
if [ "${assignable_wildcard_count}" != "0" ]; then
  fail "assignable wildcard permissions remain"
fi

invalid_role_permission_count="$(mysql_root_db "${DATABASE_NAME}" -N -B -e "
SELECT COUNT(*)
FROM sys_role_permission rp
LEFT JOIN sys_role r ON r.id = rp.role_id
LEFT JOIN sys_permission p ON p.id = rp.permission_id
WHERE rp.is_deleted = 0
  AND (
    r.id IS NULL OR r.is_deleted <> 0 OR
    p.id IS NULL OR p.is_deleted <> 0 OR p.status <> 1 OR p.assignable <> 1
  );
")"
if [ "${invalid_role_permission_count}" != "0" ]; then
  fail "invalid role permission relations remain"
fi

invalid_user_permission_count="$(mysql_root_db "${DATABASE_NAME}" -N -B -e "
SELECT COUNT(*)
FROM sys_user_permission up
LEFT JOIN sys_permission p ON p.id = up.permission_id
WHERE up.is_deleted = 0
  AND (
    p.id IS NULL OR p.is_deleted <> 0 OR p.status <> 1 OR p.assignable <> 1
  );
")"
if [ "${invalid_user_permission_count}" != "0" ]; then
  fail "invalid user permission relations remain"
fi

invalid_user_permission_effect_count="$(mysql_root_db "${DATABASE_NAME}" -N -B -e "
SELECT COUNT(*)
FROM sys_user_permission
WHERE is_deleted = 0
  AND (effect IS NULL OR effect NOT IN ('GRANT', 'DENY'));
")"
if [ "${invalid_user_permission_effect_count}" != "0" ]; then
  fail "invalid user permission effects remain"
fi

orphan_user_permission_count="$(mysql_root_db "${DATABASE_NAME}" -N -B -e "
SELECT COUNT(*)
FROM sys_user_permission up
LEFT JOIN \`user\` u
  ON u.id = up.user_id
 AND BINARY u.tenant_code = BINARY up.tenant_code
WHERE up.is_deleted = 0
  AND u.id IS NULL;
")"
if [ "${orphan_user_permission_count}" != "0" ]; then
  fail "orphan user permission relations remain"
fi

missing_order_indexes="$(mysql_root_db "${DATABASE_NAME}" -N -B <<'EOSQL'
WITH expected_indexes (table_name, index_kind, column_names) AS (
  SELECT 'sales_order', 'PRIMARY', 'order_id' UNION ALL
  SELECT 'production_order', 'UNIQUE', 'order_id' UNION ALL
  SELECT 'installation_task', 'UNIQUE', 'tenant_code,order_id'
), actual_indexes AS (
  SELECT table_name,
         CASE
           WHEN index_name = 'PRIMARY' THEN 'PRIMARY'
           WHEN non_unique = 0 THEN 'UNIQUE'
           ELSE 'INDEX'
         END AS index_kind,
         GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') AS column_names
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name IN ('sales_order', 'production_order', 'installation_task')
  GROUP BY table_name, index_name, non_unique
)
SELECT e.table_name, e.index_kind, e.column_names
FROM expected_indexes e
LEFT JOIN actual_indexes a
  ON a.table_name = e.table_name
 AND a.index_kind = e.index_kind
 AND a.column_names = e.column_names
WHERE a.table_name IS NULL;
EOSQL
)"
if [ -n "${missing_order_indexes}" ]; then
  echo "${missing_order_indexes}"
  fail "Missing required order index contracts"
fi

if [ -n "${BASELINE_DATABASE}" ]; then
  echo "4/4 对比 ${BASELINE_DATABASE} -> ${DATABASE_NAME} 缺失字段..."
  missing_baseline_columns="$(mysql_root_no_db -N -B -e "
SELECT src.table_name, src.column_name, src.column_type
FROM information_schema.columns src
LEFT JOIN information_schema.columns dst
  ON dst.table_schema = '${DATABASE_NAME}'
 AND dst.table_name = src.table_name
 AND dst.column_name = src.column_name
WHERE src.table_schema = '${BASELINE_DATABASE}'
  AND dst.column_name IS NULL
ORDER BY src.table_name, src.ordinal_position;
")"
  if [ -n "${missing_baseline_columns}" ]; then
    echo "${missing_baseline_columns}"
    fail "Target schema is missing columns from baseline schema"
  fi
else
  echo "4/4 未设置 BASELINE_DATABASE，跳过全量结构对比。"
fi

echo "校验完成，结构层面通过。"
