#!/bin/bash
set -euo pipefail

# Online database backup before schema migration.
# The backup is gzip-compressed and verified by scripts/verify-latest-backup.sh.
DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
DATABASE_NAME="${DATABASE_NAME:-hive}"
BACKUP_ROOT="${BACKUP_ROOT:-${DEPLOY_DIR}/backups/db}"
STAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_DIR="${BACKUP_ROOT}/${STAMP}"
BACKUP_FILE="${BACKUP_DIR}/${DATABASE_NAME}_${STAMP}.sql.gz"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

cd "${DEPLOY_DIR}"
test -f ".env" || fail "Missing ${DEPLOY_DIR}/.env"

set -a
source ./.env
set +a

test -n "${MYSQL_ROOT_PASSWORD:-}" || fail ".env missing MYSQL_ROOT_PASSWORD"
mkdir -p "${BACKUP_DIR}"

echo "1/3 Check MySQL..."
docker compose up -d mysql >/dev/null
docker compose exec -T mysql mysqladmin ping -h 127.0.0.1 -p"${MYSQL_ROOT_PASSWORD}" --silent

echo "2/3 Backup ${DATABASE_NAME} to ${BACKUP_FILE}..."
docker compose exec -T mysql mysqldump \
  -uroot \
  -p"${MYSQL_ROOT_PASSWORD}" \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  --default-character-set=utf8mb4 \
  "${DATABASE_NAME}" | gzip -c > "${BACKUP_FILE}"

test -s "${BACKUP_FILE}" || fail "Backup file is empty: ${BACKUP_FILE}"

echo "3/3 Backup file:"
ls -lh "${BACKUP_FILE}"
echo "Backup finished: ${BACKUP_FILE}"
