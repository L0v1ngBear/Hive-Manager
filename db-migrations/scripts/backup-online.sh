#!/bin/bash
set -euo pipefail

# Online database backup before schema migration.
# The backup is gzip-compressed and verified by scripts/verify-latest-backup.sh.
DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
DATABASE_NAME="${DATABASE_NAME:-hive}"
BACKUP_ROOT="${BACKUP_ROOT:-${DEPLOY_DIR}/backups/db}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/lib/database.sh"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

load_database_env || exit 1
STAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_DIR="${BACKUP_ROOT}/${STAMP}"
BACKUP_FILE="${BACKUP_DIR}/${DATABASE_NAME}_${STAMP}.sql.gz"
mkdir -p "${BACKUP_DIR}"

echo "1/3 Check MySQL..."
ensure_database_available

echo "2/3 Backup ${DATABASE_NAME} to ${BACKUP_FILE}..."
mysql_root_dump \
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
