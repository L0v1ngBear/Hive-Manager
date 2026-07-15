#!/bin/bash
set -euo pipefail

# Import a schema-only baseline into a shadow database by default.
# Online data must be changed only by versioned migrations; never import a data baseline into hive casually.
DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
DATABASE_NAME="${DATABASE_NAME:-hive}"
TARGET_DATABASE="${TARGET_DATABASE:-hive_shadow}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MIGRATION_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BASELINE_FILE="${BASELINE_FILE:-${MIGRATION_DIR}/baseline/hive_schema_baseline.sql}"
MANIFEST_FILE="${MIGRATION_MANIFEST:-${MIGRATION_DIR}/migration_manifest.txt}"
BASELINE_MIGRATION_VERSION="baseline/hive_schema_baseline"
BASELINE_LAST_MIGRATION="${BASELINE_LAST_MIGRATION:-migrations/V20260713_001_order_information_channel_and_cancel_reason.sql}"
RESET_TARGET="${RESET_TARGET:-NO}"
CONFIRM_IMPORT_TO_HIVE="${CONFIRM_IMPORT_TO_HIVE:-NO}"
ALLOW_DATA_BASELINE="${ALLOW_DATA_BASELINE:-NO}"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

sql_escape() {
  printf "%s" "$1" | sed "s/'/''/g"
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
  fail "Missing sha256sum/openssl; cannot register baseline migration state"
}

mysql_root_no_db() {
  docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 "$@"
}

mysql_root_db() {
  local db_name="$1"
  shift
  docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 "$@" "${db_name}"
}

assert_schema_only_baseline() {
  local file_path="$1"
  if [ "${ALLOW_DATA_BASELINE}" = "YES" ]; then
    echo "ALLOW_DATA_BASELINE=YES detected. Only use this for isolated shadow database verification."
    return
  fi
  if grep -Eiq '^[[:space:]]*(DROP[[:space:]]+TABLE|TRUNCATE[[:space:]]+TABLE|DELETE[[:space:]]+FROM|INSERT[[:space:]]+INTO|REPLACE[[:space:]]+INTO|UPDATE[[:space:]])' "${file_path}"; then
    fail "Baseline is not schema-only. Online rebuild/import only allows table structure; use versioned migrations or explicit seed migrations for data."
  fi
}

cd "${DEPLOY_DIR}"
test -f ".env" || fail "Missing ${DEPLOY_DIR}/.env"
test -f "${BASELINE_FILE}" || test -f "${BASELINE_FILE}.gz" || fail "Missing baseline SQL: ${BASELINE_FILE} or ${BASELINE_FILE}.gz"
test -f "${MANIFEST_FILE}" || fail "Missing migration manifest: ${MANIFEST_FILE}"

set -a
source ./.env
set +a

test -n "${MYSQL_ROOT_PASSWORD:-}" || fail ".env missing MYSQL_ROOT_PASSWORD"

if [ "${TARGET_DATABASE}" = "${DATABASE_NAME}" ] && [ "${CONFIRM_IMPORT_TO_HIVE}" != "YES" ]; then
  fail "Target database is online ${DATABASE_NAME}. If you really need to initialize it, set CONFIRM_IMPORT_TO_HIVE=YES RESET_TARGET=YES after backup."
fi

if [ -f "${BASELINE_FILE}.gz" ] && [ ! -f "${BASELINE_FILE}" ]; then
  REAL_BASELINE_FILE="${BASELINE_FILE}.gz"
else
  REAL_BASELINE_FILE="${BASELINE_FILE}"
fi

case "${REAL_BASELINE_FILE}" in
  *.gz)
    if [ "${ALLOW_DATA_BASELINE}" != "YES" ]; then
      fail "Compressed baseline cannot be verified as schema-only. Decompress it or set ALLOW_DATA_BASELINE=YES for shadow-only verification."
    fi
    ;;
  *)
    assert_schema_only_baseline "${REAL_BASELINE_FILE}"
    ;;
esac

echo "1/7 Run preflight..."
DATABASE_NAME="${TARGET_DATABASE}" DEPLOY_DIR="${DEPLOY_DIR}" bash "${SCRIPT_DIR}/preflight-online.sh"

echo "2/7 Prepare target database ${TARGET_DATABASE}..."
if [ "${RESET_TARGET}" = "YES" ]; then
  mysql_root_no_db -e "DROP DATABASE IF EXISTS \`${TARGET_DATABASE}\`;"
fi

mysql_root_no_db -e "
CREATE DATABASE IF NOT EXISTS \`${TARGET_DATABASE}\`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;
"

table_count="$(mysql_root_no_db -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '${TARGET_DATABASE}';")"
if [ "${table_count}" != "0" ] && [ "${RESET_TARGET}" != "YES" ]; then
  fail "Target database ${TARGET_DATABASE} already has ${table_count} tables. Set RESET_TARGET=YES or use a different TARGET_DATABASE."
fi

echo "3/7 Import baseline ${REAL_BASELINE_FILE} -> ${TARGET_DATABASE}..."
case "${REAL_BASELINE_FILE}" in
  *.gz)
    gzip -dc "${REAL_BASELINE_FILE}" | mysql_root_db "${TARGET_DATABASE}"
    ;;
  *)
    mysql_root_db "${TARGET_DATABASE}" < "${REAL_BASELINE_FILE}"
    ;;
esac

echo "4/7 Register checksummed baseline migration state through ${BASELINE_LAST_MIGRATION}..."
mysql_root_db "${TARGET_DATABASE}" <<'EOSQL'
CREATE TABLE IF NOT EXISTS `schema_migration_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `version` varchar(120) NOT NULL COMMENT 'Migration version',
  `file_name` varchar(255) NOT NULL COMMENT 'Migration file',
  `checksum_sha256` varchar(64) NOT NULL COMMENT 'Migration SHA256',
  `status` varchar(20) NOT NULL COMMENT 'RUNNING/SUCCESS/FAILED',
  `error_message` text DEFAULT NULL COMMENT 'Failure reason',
  `executed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'First execution time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schema_migration_version` (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Database migration history';
EOSQL

baseline_checksum="$(hash_file "${REAL_BASELINE_FILE}")"
baseline_checksum_sql="$(sql_escape "${baseline_checksum}")"
mysql_root_db "${TARGET_DATABASE}" -e "
INSERT INTO schema_migration_history
  (version, file_name, checksum_sha256, status, error_message)
VALUES
  ('${BASELINE_MIGRATION_VERSION}', 'baseline/hive_schema_baseline.sql', '${baseline_checksum_sql}', 'SUCCESS', NULL)
ON DUPLICATE KEY UPDATE
  file_name = VALUES(file_name),
  checksum_sha256 = VALUES(checksum_sha256),
  status = 'SUCCESS',
  error_message = NULL,
  updated_at = NOW();
"

baseline_cutoff_reached="NO"
while IFS= read -r raw_line || [ -n "${raw_line}" ]; do
  entry="$(printf "%s" "${raw_line}" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
  if [ -z "${entry}" ] || [[ "${entry}" == \#* ]]; then
    continue
  fi
  if [ "${baseline_cutoff_reached}" = "YES" ]; then
    continue
  fi

  migration_file="${MIGRATION_DIR}/${entry}"
  test -f "${migration_file}" || fail "Manifest migration does not exist: ${migration_file}"
  migration_version="${entry%.sql}"
  migration_checksum="$(hash_file "${migration_file}")"
  migration_version_sql="$(sql_escape "${migration_version}")"
  entry_sql="$(sql_escape "${entry}")"
  migration_checksum_sql="$(sql_escape "${migration_checksum}")"

  mysql_root_db "${TARGET_DATABASE}" -e "
INSERT INTO schema_migration_history
  (version, file_name, checksum_sha256, status, error_message)
VALUES
  ('${migration_version_sql}', '${entry_sql}', '${migration_checksum_sql}', 'SUCCESS', NULL)
ON DUPLICATE KEY UPDATE
  file_name = VALUES(file_name),
  checksum_sha256 = VALUES(checksum_sha256),
  status = 'SUCCESS',
  error_message = NULL,
  updated_at = NOW();
" </dev/null

  if [ "${entry}" = "${BASELINE_LAST_MIGRATION}" ]; then
    baseline_cutoff_reached="YES"
  fi
done < "${MANIFEST_FILE}"

if [ "${baseline_cutoff_reached}" != "YES" ]; then
  fail "Baseline cutoff is missing from migration manifest: ${BASELINE_LAST_MIGRATION}"
fi

echo "5/7 Execute migrations newer than the baseline cutoff..."
DEPLOY_DIR="${DEPLOY_DIR}" DATABASE_NAME="${TARGET_DATABASE}" \
  MIGRATION_MANIFEST="${MANIFEST_FILE}" RUN_PREFLIGHT=NO \
  bash "${SCRIPT_DIR}/run-versioned-migrations.sh"

echo "6/7 Print import result..."
mysql_root_no_db -e "
SELECT COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = '${TARGET_DATABASE}';
"

mysql_root_db "${TARGET_DATABASE}" -e "
SELECT COUNT(*) AS successful_migration_state_count
FROM schema_migration_history
WHERE status = 'SUCCESS';
"

echo "7/7 Baseline import finished. Represented migrations were registered and newer migrations were executed."
