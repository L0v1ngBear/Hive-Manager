#!/bin/bash
set -euo pipefail

# 版本化迁移执行器。
# 规则：同版本同校验值自动跳过；同版本已成功但文件内容变更则失败，必须新增版本。

DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
DATABASE_NAME="${DATABASE_NAME:-hive}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MIGRATION_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
MANIFEST_FILE="${MIGRATION_MANIFEST:-${MIGRATION_DIR}/migration_manifest.txt}"
RUN_PREFLIGHT="${RUN_PREFLIGHT:-YES}"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

sql_escape() {
  printf "%s" "$1" | sed "s/'/''/g"
}

file_sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
    return
  fi
  if command -v openssl >/dev/null 2>&1; then
    openssl dgst -sha256 "$1" | awk '{print $2}'
    return
  fi
  fail "服务器缺少 sha256sum/openssl，无法计算迁移文件校验值。"
}

mysql_root_no_db() {
  docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 "$@"
}

mysql_root_db() {
  docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 "$@" "${DATABASE_NAME}"
}

repair_xxl_job_official_schema_if_needed() {
  mysql_root_no_db <<'EOSQL'
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @xxl_group_exists = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE table_schema = 'xxl_job'
    AND table_name = 'xxl_job_group'
);
SET @sql = IF(
  @xxl_group_exists > 0,
  'ALTER TABLE `xxl_job`.`xxl_job_group` MODIFY `title` varchar(64) NOT NULL COMMENT ''Executor title''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @xxl_info_exists = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE table_schema = 'xxl_job'
    AND table_name = 'xxl_job_info'
);
SET @sql = IF(
  @xxl_info_exists > 0,
  'ALTER TABLE `xxl_job`.`xxl_job_info` MODIFY `executor_param` text DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
EOSQL
}

cd "${DEPLOY_DIR}"
test -f ".env" || fail "缺少 ${DEPLOY_DIR}/.env。"
test -f "${MANIFEST_FILE}" || fail "缺少迁移清单：${MANIFEST_FILE}"

set -a
source ./.env
set +a

test -n "${MYSQL_ROOT_PASSWORD:-}" || fail ".env 缺少 MYSQL_ROOT_PASSWORD。"

echo "0/5 兼容修复 XXL-JOB 官方表结构..."
repair_xxl_job_official_schema_if_needed

if [ "${RUN_PREFLIGHT}" = "YES" ]; then
  echo "0/5 执行迁移预检..."
  DATABASE_NAME="${DATABASE_NAME}" DEPLOY_DIR="${DEPLOY_DIR}" bash "${SCRIPT_DIR}/preflight-online.sh"
fi

echo "1/5 确保迁移历史表存在..."
mysql_root_no_db <<EOSQL
CREATE DATABASE IF NOT EXISTS \`${DATABASE_NAME}\`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS \`${DATABASE_NAME}\`.\`schema_migration_history\` (
  \`id\` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  \`version\` varchar(120) NOT NULL COMMENT '迁移版本，一般取SQL文件名',
  \`file_name\` varchar(255) NOT NULL COMMENT '迁移文件名',
  \`checksum_sha256\` varchar(64) NOT NULL COMMENT '迁移文件SHA256',
  \`status\` varchar(20) NOT NULL COMMENT '执行状态(RUNNING/SUCCESS/FAILED)',
  \`error_message\` text DEFAULT NULL COMMENT '失败原因',
  \`executed_at\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次执行时间',
  \`updated_at\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (\`id\`),
  UNIQUE KEY \`uk_schema_migration_version\` (\`version\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据库迁移历史表';
EOSQL

echo "2/5 按清单执行迁移..."
while IFS= read -r raw_line || [ -n "${raw_line}" ]; do
  line="$(printf "%s" "${raw_line}" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
  if [ -z "${line}" ] || [[ "${line}" == \#* ]]; then
    continue
  fi

  sql_file="${MIGRATION_DIR}/${line}"
  test -f "${sql_file}" || fail "清单中的 SQL 不存在：${sql_file}"

  version="${line%.sql}"
  checksum="$(file_sha256 "${sql_file}")"
  version_sql="$(sql_escape "${version}")"
  file_sql="$(sql_escape "${line}")"
  checksum_sql="$(sql_escape "${checksum}")"

  # Prevent mysql -e from consuming the migration manifest stdin used by the while loop.
  existing="$(mysql_root_db -N -B -e "SELECT CONCAT(status, '|', checksum_sha256) FROM schema_migration_history WHERE version = '${version_sql}' LIMIT 1;" </dev/null || true)"
  if [ -n "${existing}" ]; then
    existing_status="${existing%%|*}"
    existing_checksum="${existing#*|}"
    if [ "${existing_status}" = "SUCCESS" ] && [ "${existing_checksum}" = "${checksum}" ]; then
      echo "SKIP ${line}，已执行成功。"
      continue
    fi
    if [ "${existing_status}" = "SUCCESS" ] && [ "${existing_checksum}" != "${checksum}" ]; then
      fail "${line} 已成功执行过，但文件内容发生变化。请新增迁移 SQL，不要修改历史 SQL。"
    fi
  fi

  echo "RUN  ${line}"
  mysql_root_db -e "
INSERT INTO schema_migration_history (version, file_name, checksum_sha256, status, error_message)
VALUES ('${version_sql}', '${file_sql}', '${checksum_sql}', 'RUNNING', NULL)
ON DUPLICATE KEY UPDATE
  file_name = VALUES(file_name),
  checksum_sha256 = VALUES(checksum_sha256),
  status = 'RUNNING',
  error_message = NULL,
  updated_at = NOW();
" </dev/null

  error_file="$(mktemp)"
  if { printf 'SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;\n'; cat "${sql_file}"; } | mysql_root_db 2>"${error_file}"; then
    mysql_root_db -e "
UPDATE schema_migration_history
SET status = 'SUCCESS',
    error_message = NULL,
    updated_at = NOW()
WHERE version = '${version_sql}';
" </dev/null
    rm -f "${error_file}"
    echo "OK   ${line}"
  else
    error_message="$(head -c 1500 "${error_file}" | tr '\n' ' ')"
    error_sql="$(sql_escape "${error_message}")"
    mysql_root_db -e "
UPDATE schema_migration_history
SET status = 'FAILED',
    error_message = '${error_sql}',
    updated_at = NOW()
WHERE version = '${version_sql}';
" </dev/null
    cat "${error_file}" >&2
    rm -f "${error_file}"
    fail "${line} 执行失败。"
  fi
done < "${MANIFEST_FILE}"

echo "3/5 输出最近迁移历史..."
mysql_root_db -e "
SELECT version, status, executed_at, updated_at
FROM schema_migration_history
ORDER BY id DESC
LIMIT 20;
"

echo "4/5 检查是否存在失败迁移..."
failed_count="$(mysql_root_db -N -B -e "SELECT COUNT(*) FROM schema_migration_history WHERE status = 'FAILED';")"
if [ "${failed_count}" != "0" ]; then
  fail "存在失败迁移记录，请先处理。"
fi

echo "5/5 迁移完成。"
