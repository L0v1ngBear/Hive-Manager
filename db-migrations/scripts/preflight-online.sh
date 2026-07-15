#!/bin/bash
set -euo pipefail

# 线上迁移预检：确认 MySQL 可连、磁盘正常、root 账号可 DDL。

DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
DATABASE_NAME="${DATABASE_NAME:-hive}"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

mysql_root_no_db() {
  docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 "$@"
}

cd "${DEPLOY_DIR}"
test -f ".env" || fail "缺少 ${DEPLOY_DIR}/.env。"

set -a
source ./.env
set +a

test -n "${MYSQL_ROOT_PASSWORD:-}" || fail ".env 缺少 MYSQL_ROOT_PASSWORD。"

echo "1/5 检查 Docker Compose 和 MySQL 容器..."
docker compose up -d mysql >/dev/null
docker compose exec -T mysql mysqladmin ping -h 127.0.0.1 -p"${MYSQL_ROOT_PASSWORD}" --silent

echo "2/5 检查服务器磁盘空间..."
df -h

echo "3/5 检查 MySQL 版本和目标库..."
mysql_root_no_db -e "
SELECT VERSION() AS mysql_version;
CREATE DATABASE IF NOT EXISTS \`${DATABASE_NAME}\`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;
"

echo "4/5 检查 DDL 能力和 InnoDB 存储引擎..."
mysql_root_no_db "${DATABASE_NAME}" <<'EOSQL'
CREATE TABLE IF NOT EXISTS `__migration_ddl_probe` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '迁移预检ID',
  `probe_value` varchar(30) NOT NULL COMMENT '预检值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='迁移DDL预检表';
INSERT INTO `__migration_ddl_probe` (`probe_value`) VALUES ('ok');
DROP TABLE `__migration_ddl_probe`;
EOSQL

echo "5/5 检查迁移历史表状态..."
mysql_root_no_db -e "
SELECT COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = '${DATABASE_NAME}';
"

echo "预检通过，可以继续备份和迁移。"
