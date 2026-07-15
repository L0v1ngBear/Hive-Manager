#!/bin/bash
set -u

# 非破坏性诊断脚本：排查 MySQL 进程活着但 DDL 失败的问题。

DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
DATABASE_NAME="${DATABASE_NAME:-hive}"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

section() {
  echo
  echo "== $1 =="
}

cd "${DEPLOY_DIR}" || fail "无法进入 ${DEPLOY_DIR}"
test -f ".env" || fail "缺少 ${DEPLOY_DIR}/.env。"

set -a
source ./.env
set +a

test -n "${MYSQL_ROOT_PASSWORD:-}" || fail ".env 缺少 MYSQL_ROOT_PASSWORD。"

section "Docker Compose 状态"
docker compose ps

section "MySQL 容器状态"
docker inspect mysql --format 'Status={{.State.Status}} Running={{.State.Running}} RestartCount={{.RestartCount}} ExitCode={{.State.ExitCode}} OOMKilled={{.State.OOMKilled}} Error={{.State.Error}}' 2>/dev/null || true

section "MySQL 版本和关键变量"
docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 -e "
SELECT VERSION() AS mysql_version;
SHOW VARIABLES LIKE 'datadir';
SHOW VARIABLES LIKE 'innodb_force_recovery';
SHOW VARIABLES LIKE 'innodb_file_per_table';
SELECT @@read_only AS read_only, @@super_read_only AS super_read_only;
SHOW ENGINES;
" 2>&1

section "容器内数据目录"
docker compose exec -T mysql sh -lc '
echo "user=$(id)"
echo "pwd=$(pwd)"
ls -ld /var/lib/mysql || true
ls -la /var/lib/mysql | head -80 || true
df -h /var/lib/mysql || true
df -i /var/lib/mysql || true
touch /var/lib/mysql/__write_probe 2>&1 && rm -f /var/lib/mysql/__write_probe && echo "write_probe=OK" || echo "write_probe=FAILED"
' 2>&1

section "宿主机数据目录"
ls -ld ./mysql ./mysql/data 2>&1 || true
stat -c '%U %G %a %n' ./mysql ./mysql/data 2>&1 || true
df -h ./mysql/data 2>&1 || true
df -i ./mysql/data 2>&1 || true

section "DDL 探针"
docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 "${DATABASE_NAME}" <<'EOSQL'
CREATE TABLE IF NOT EXISTS `__diagnose_ddl_probe` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `probe_value` varchar(30) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
INSERT INTO `__diagnose_ddl_probe` (`probe_value`) VALUES ('ok');
DROP TABLE `__diagnose_ddl_probe`;
EOSQL
ddl_status=$?
if [ "${ddl_status}" -ne 0 ]; then
  echo "DDL 探针失败：这不是迁移 SQL 问题，是 MySQL 数据目录/存储引擎不可写或损坏。"
else
  echo "DDL 探针通过。"
fi

section "MySQL 最近日志"
docker compose logs --tail=200 mysql 2>&1 || true

exit "${ddl_status}"

