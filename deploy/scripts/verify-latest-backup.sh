#!/usr/bin/env bash
set -euo pipefail

# Verify the latest database backup without restoring it.
# Read-only by design: no data deletion, no service restart, no database migration.
cd "$(dirname "$0")/.."

BACKUP_ROOT="${BACKUP_ROOT:-backups/db}"
DATABASE_NAME="${DATABASE_NAME:-hive}"
MAX_BACKUP_AGE_HOURS="${MAX_BACKUP_AGE_HOURS:-48}"
MIN_BACKUP_SIZE_KB="${MIN_BACKUP_SIZE_KB:-16}"
BACKUP_FILE="${BACKUP_FILE:-}"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

warn() {
  echo "WARN: $1"
}

ok() {
  echo "OK: $1"
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Command not found: $1"
}

is_number() {
  echo "$1" | grep -Eq '^[0-9]+$'
}

require_command gzip
require_command find
require_command stat

is_number "${MAX_BACKUP_AGE_HOURS}" || fail "MAX_BACKUP_AGE_HOURS must be a non-negative integer"
is_number "${MIN_BACKUP_SIZE_KB}" || fail "MIN_BACKUP_SIZE_KB must be a non-negative integer"

if [ -z "${BACKUP_FILE}" ]; then
  [ -d "${BACKUP_ROOT}" ] || fail "backup directory not found: ${BACKUP_ROOT}"
  BACKUP_FILE="$(
    find "${BACKUP_ROOT}" -type f -name "${DATABASE_NAME}_*.sql.gz" -printf '%T@ %p\n' 2>/dev/null \
      | sort -nr \
      | awk 'NR == 1 { sub(/^[^ ]+ /, ""); print }'
  )"
fi

[ -n "${BACKUP_FILE}" ] || fail "no backup file found under ${BACKUP_ROOT} for database ${DATABASE_NAME}"
[ -f "${BACKUP_FILE}" ] || fail "backup file not found: ${BACKUP_FILE}"

size_bytes="$(stat -c '%s' "${BACKUP_FILE}")"
size_kb=$(( (size_bytes + 1023) / 1024 ))
if [ "${size_kb}" -lt "${MIN_BACKUP_SIZE_KB}" ]; then
  fail "backup file too small: ${BACKUP_FILE}, size=${size_kb}KB, min=${MIN_BACKUP_SIZE_KB}KB"
fi
ok "backup file exists: ${BACKUP_FILE}, size=${size_kb}KB"

gzip -t "${BACKUP_FILE}"
ok "backup gzip integrity is valid"

now_epoch="$(date +%s)"
file_epoch="$(stat -c '%Y' "${BACKUP_FILE}")"
age_seconds=$(( now_epoch - file_epoch ))
if [ "${age_seconds}" -lt 0 ]; then
  warn "backup file timestamp is in the future: ${BACKUP_FILE}"
elif [ "${MAX_BACKUP_AGE_HOURS}" -gt 0 ]; then
  max_age_seconds=$(( MAX_BACKUP_AGE_HOURS * 3600 ))
  if [ "${age_seconds}" -gt "${max_age_seconds}" ]; then
    fail "latest backup is too old: age_seconds=${age_seconds}, max_seconds=${max_age_seconds}"
  fi
fi
ok "backup age is acceptable"

set +o pipefail
header="$(gzip -cd "${BACKUP_FILE}" 2>/dev/null | head -c 65536 || true)"
set -o pipefail

if ! printf '%s' "${header}" | grep -Eq 'MySQL dump|MariaDB dump|CREATE TABLE|INSERT INTO|Dump completed'; then
  fail "backup content does not look like a MySQL dump"
fi
ok "backup content looks like a MySQL dump"

if printf '%s' "${header}" | grep -Eq 'CREATE DATABASE `?'"${DATABASE_NAME}"'`?|USE `?'"${DATABASE_NAME}"'`?'; then
  ok "backup header references database ${DATABASE_NAME}"
else
  warn "backup header does not reference database ${DATABASE_NAME}; this can be normal for single-database mysqldump"
fi

echo "Latest backup verification passed."
