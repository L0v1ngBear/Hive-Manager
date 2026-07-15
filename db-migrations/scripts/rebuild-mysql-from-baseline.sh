#!/bin/bash
set -euo pipefail

# 高风险恢复脚本：保留旧数据目录，然后用 baseline 重建 MySQL。
# 只有线上库确认混乱/DDL 已坏，且已经接受用本地标准库重建时才执行。

DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
DATABASE_NAME="${DATABASE_NAME:-hive}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MIGRATION_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BASELINE_FILE="${BASELINE_FILE:-${MIGRATION_DIR}/baseline/hive_schema_baseline.sql}"
CONFIRM_REBUILD_MYSQL="${CONFIRM_REBUILD_MYSQL:-NO}"
STAMP="$(date +%Y%m%d_%H%M%S)"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

service_exists() {
  docker compose config --services | grep -qx "$1"
}

cd "${DEPLOY_DIR}"
test -f "scripts/verify-order-information-channel-artifacts.sh" \
  || fail "Missing scripts/verify-order-information-channel-artifacts.sh"
bash scripts/verify-order-information-channel-artifacts.sh
test -f ".env" || fail "缺少 ${DEPLOY_DIR}/.env。"
test -f "${BASELINE_FILE}" || test -f "${BASELINE_FILE}.gz" || fail "缺少 baseline：${BASELINE_FILE} 或 ${BASELINE_FILE}.gz。"

if [ "${CONFIRM_REBUILD_MYSQL}" != "YES" ]; then
  fail "这是重建 MySQL 数据目录的高风险操作。确认执行请设置：CONFIRM_REBUILD_MYSQL=YES"
fi

set -a
source ./.env
set +a

test -n "${MYSQL_ROOT_PASSWORD:-}" || fail ".env 缺少 MYSQL_ROOT_PASSWORD。"

echo "1/8 停止业务服务，避免重建期间继续写库..."
for service in nginx management-backend-1 management-backend-2 backend-1 backend-2 mysql-backup; do
  if service_exists "${service}"; then
    docker compose stop "${service}" || true
  fi
done

echo "2/8 尝试逻辑备份，失败也继续做物理留存..."
set +e
bash "${SCRIPT_DIR}/backup-online.sh"
logical_backup_status=$?
set -e
if [ "${logical_backup_status}" -ne 0 ]; then
  echo "逻辑备份失败，说明当前 MySQL 已不健康；后续仍会物理保留旧数据目录。"
fi

echo "3/8 停止并移除旧 MySQL 容器，释放异常挂载..."
docker compose stop mysql || true
docker compose rm -f mysql || true

echo "4/8 物理保留旧 mysql/data..."
mkdir -p backups/mysql-data
if [ -d mysql/data ]; then
  tar -czf "backups/mysql-data/mysql-data-before-rebuild-${STAMP}.tar.gz" mysql/data
  mv mysql/data "mysql/data.broken.${STAMP}"
else
  echo "未发现 mysql/data，继续创建新数据目录。"
fi

echo "5/8 创建新的 MySQL 数据目录..."
mkdir -p mysql/data
chown -R 999:999 mysql/data
chmod 750 mysql/data

echo "6/8 重建 MySQL 容器..."
docker compose up -d mysql
sleep 8
for i in $(seq 1 60); do
  if docker compose exec -T mysql mysqladmin ping -h 127.0.0.1 -p"${MYSQL_ROOT_PASSWORD}" --silent >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
docker compose exec -T mysql mysqladmin ping -h 127.0.0.1 -p"${MYSQL_ROOT_PASSWORD}" --silent

echo "7/8 初始化数据库账号..."
if [ -x scripts/init-database.sh ]; then
  bash scripts/init-database.sh
elif [ -x mysql/init/01-create-app-users.sh ]; then
  bash mysql/init/01-create-app-users.sh
else
  echo "未找到 init-database.sh，跳过账号初始化。"
fi

echo "8/9 导入 baseline 到 ${DATABASE_NAME}..."
TARGET_DATABASE="${DATABASE_NAME}" RESET_TARGET=YES CONFIRM_IMPORT_TO_HIVE=YES \
  bash "${SCRIPT_DIR}/import-baseline-to-shadow.sh"

echo "9/9 校验并启动业务服务..."
DATABASE_NAME="${DATABASE_NAME}" BASELINE_FILE="${BASELINE_FILE}" \
  bash "${SCRIPT_DIR}/verify-online-schema.sh"
docker compose up -d

echo "MySQL 已按 baseline 重建完成。旧数据目录已保留为 mysql/data.broken.${STAMP}，物理备份在 backups/mysql-data/。"
