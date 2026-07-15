#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")/.."

VERSION="${VERSION:-migrations/V20260705_004_installation_task_schema}"
DATABASE_NAME="${DATABASE_NAME:-hive}"
MIGRATION_FILE="db-migrations/${VERSION}.sql"
BASENAME="$(basename "${MIGRATION_FILE}")"

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
  fail "missing sha256sum/openssl"
}

test -f ".env" || fail "missing .env"
set -a
source ./.env
set +a

test -n "${MYSQL_ROOT_PASSWORD:-}" || fail ".env missing MYSQL_ROOT_PASSWORD"
test -f "${MIGRATION_FILE}" || fail "missing current migration file: ${MIGRATION_FILE}"

mysql_root_db() {
  docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 "$@" "${DATABASE_NAME}"
}

echo "Migration drift diagnosis"
echo "version=${VERSION}"
echo "database=${DATABASE_NAME}"
echo "current_file=${MIGRATION_FILE}"

expected="$(mysql_root_db -N -B -e "SELECT checksum_sha256 FROM schema_migration_history WHERE version = '${VERSION}' AND status = 'SUCCESS' LIMIT 1;" 2>/dev/null || true)"
if [ -z "${expected}" ]; then
  if [ "${ALLOW_NO_HISTORY:-}" = "YES" ]; then
    echo "SKIP: no SUCCESS migration history found for ${VERSION}."
    exit 0
  fi
  fail "no SUCCESS migration history found for ${VERSION}"
fi

current="$(hash_file "${MIGRATION_FILE}")"
echo "expected_checksum=${expected}"
echo "current_checksum=${current}"

if [ "${expected}" = "${current}" ]; then
  echo "OK: current file matches migration history. If restart still fails, server may be running in another directory."
  exit 0
fi

echo
echo "Current file does not match migration history."
echo "Searching local server backups for ${BASENAME} ..."

matches_file="$(mktemp)"
trap 'rm -f "${matches_file}"' EXIT

find /root/hive /root -path '*/mysql/data/*' -prune -o -type f -name "${BASENAME}" -print 2>/dev/null \
  | while IFS= read -r candidate; do
      candidate_hash="$(hash_file "${candidate}" || true)"
      if [ -n "${candidate_hash}" ]; then
        printf '%s  %s\n' "${candidate_hash}" "${candidate}"
      fi
    done | sort -u | tee "${matches_file}"

restore_candidate="$(awk -v expected="${expected}" '$1 == expected { $1=""; sub(/^  /, ""); print; exit }' "${matches_file}")"

if [ -z "${restore_candidate}" ]; then
  echo
  echo "FAIL: no matching historical file found on this server."
  echo "Next step: upload the exact historical file whose sha256 is ${expected}, or restore it from an older release snapshot."
  exit 1
fi

echo
echo "Found matching historical file:"
echo "${restore_candidate}"

if [ "${CONFIRM_RESTORE_MIGRATION:-}" != "YES" ]; then
  echo
  echo "Dry-run only. To restore it, run:"
  echo "CONFIRM_RESTORE_MIGRATION=YES VERSION=${VERSION} bash scripts/diagnose-migration-drift.sh"
  exit 2
fi

backup_file="${MIGRATION_FILE}.drift.$(date +%Y%m%d_%H%M%S).bak"
cp -p "${MIGRATION_FILE}" "${backup_file}"
cp -p "${restore_candidate}" "${MIGRATION_FILE}"

restored="$(hash_file "${MIGRATION_FILE}")"
echo "backup_file=${backup_file}"
echo "restored_checksum=${restored}"

if [ "${restored}" != "${expected}" ]; then
  fail "restore failed, restored checksum still differs"
fi

echo "OK: migration file restored to the exact historical content."
