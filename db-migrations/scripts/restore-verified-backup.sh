#!/usr/bin/env bash
set -euo pipefail

# Restore is verified in an isolated shadow database first. The online database is replaced only
# after explicit confirmation, a fresh pre-restore backup, and a successful shadow verification.
DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
DATABASE_NAME="${DATABASE_NAME:-hive}"
SHADOW_DATABASE="${SHADOW_DATABASE:-hive_restore_verify}"
BACKUP_FILE="${BACKUP_FILE:-}"
CONFIRM_DATABASE_RESTORE="${CONFIRM_DATABASE_RESTORE:-NO}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

mysql_root() {
  docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 "$@"
}

cd "${DEPLOY_DIR}"
[ -f .env ] || fail "Missing ${DEPLOY_DIR}/.env"
[ -n "${BACKUP_FILE}" ] || fail "Set BACKUP_FILE to the .sql.gz backup to restore"
[ -f "${BACKUP_FILE}" ] || fail "Backup file not found: ${BACKUP_FILE}"
[ "${SHADOW_DATABASE}" != "${DATABASE_NAME}" ] || fail "Shadow database must differ from the online database"

set -a
source ./.env
set +a
[ -n "${MYSQL_ROOT_PASSWORD:-}" ] || fail ".env missing MYSQL_ROOT_PASSWORD"

BACKUP_FILE="${BACKUP_FILE}" DATABASE_NAME="${DATABASE_NAME}" MAX_BACKUP_AGE_HOURS=0 \
  bash scripts/verify-latest-backup.sh

if gzip -cd "${BACKUP_FILE}" | grep -Eq '^[[:space:]]*(CREATE DATABASE|USE )[[:space:]]'; then
  fail "Backup contains CREATE DATABASE/USE statements and cannot be safely redirected to a shadow database"
fi

echo "1/5 Import backup into shadow database ${SHADOW_DATABASE}..."
mysql_root -e "DROP DATABASE IF EXISTS \`${SHADOW_DATABASE}\`; CREATE DATABASE \`${SHADOW_DATABASE}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
gzip -cd "${BACKUP_FILE}" | mysql_root "${SHADOW_DATABASE}"

echo "2/5 Verify restored shadow schema and migration state..."
DATABASE_NAME="${SHADOW_DATABASE}" bash "${SCRIPT_DIR}/verify-online-schema.sh"

if [ "${CONFIRM_DATABASE_RESTORE}" != "YES" ]; then
  mysql_root -e "DROP DATABASE IF EXISTS \`${SHADOW_DATABASE}\`;"
  echo "Shadow restore verification passed. Online database was not changed."
  echo "Set CONFIRM_DATABASE_RESTORE=YES to perform the guarded online replacement."
  exit 0
fi

echo "3/5 Stop backend writes and back up the current online database..."
docker compose stop backend
DATABASE_NAME="${DATABASE_NAME}" bash "${SCRIPT_DIR}/backup-online.sh"

echo "4/5 Replace online database from the verified shadow database..."
mysql_root -e "DROP DATABASE IF EXISTS \`${DATABASE_NAME}\`; CREATE DATABASE \`${DATABASE_NAME}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
docker compose exec -T mysql mysqldump \
  -uroot -p"${MYSQL_ROOT_PASSWORD}" --single-transaction --routines --triggers --events \
  --default-character-set=utf8mb4 "${SHADOW_DATABASE}" | mysql_root "${DATABASE_NAME}"

echo "5/5 Verify online database before starting backend..."
DATABASE_NAME="${DATABASE_NAME}" bash "${SCRIPT_DIR}/verify-online-schema.sh"
mysql_root -e "DROP DATABASE IF EXISTS \`${SHADOW_DATABASE}\`;"
docker compose up -d backend nginx
echo "Verified database restore completed."
