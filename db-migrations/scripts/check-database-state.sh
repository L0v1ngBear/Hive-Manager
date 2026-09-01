#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
DATABASE_NAME="${DATABASE_NAME:-hive}"
ALLOW_FRESH_DATABASE="${ALLOW_FRESH_DATABASE:-NO}"
source "$(cd "$(dirname "$0")" && pwd)/lib/database.sh"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

load_database_env || exit 1
ensure_database_available
table_count="$(mysql_root_no_db -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DATABASE_NAME}';")"

if [ "${table_count}" = "0" ]; then
  state="FRESH_EMPTY"
else
  required_count="$(mysql_root_no_db -N -B -e "
SELECT COUNT(*)
FROM information_schema.tables
WHERE table_schema='${DATABASE_NAME}'
  AND table_name IN ('tenant','user','sys_permission','sys_role','schema_migration_history');
")"
  if [ "${required_count}" = "5" ]; then
    history_count="$(mysql_root_db "${DATABASE_NAME}" -N -B -e "SELECT COUNT(*) FROM schema_migration_history WHERE status = 'SUCCESS';")"
    failed_count="$(mysql_root_db "${DATABASE_NAME}" -N -B -e "SELECT COUNT(*) FROM schema_migration_history WHERE status <> 'SUCCESS';")"
    if [ "${history_count}" -gt 0 ]; then
      if [ "${failed_count}" = "0" ]; then
        state="READY"
      else
        state="RECOVERABLE_FAILED"
      fi
    else
      state="INCOMPLETE"
    fi
  else
    state="INCOMPLETE"
  fi
fi

echo "Database state: ${state}"
case "${state}" in
  READY)
    exit 0
    ;;
  RECOVERABLE_FAILED)
    echo "Managed Hive schema has ${failed_count} failed migration(s); the versioned migration runner will retry them."
    exit 0
    ;;
  FRESH_EMPTY)
    if [ "${ALLOW_FRESH_DATABASE}" = "YES" ]; then
      exit 0
    fi
    fail "FRESH_EMPTY database requires db-migrations/scripts/initialize-fresh-database.sh; routine migration is blocked"
    ;;
  INCOMPLETE)
    fail "INCOMPLETE database is neither empty nor a managed Hive schema; automatic migration is blocked"
    ;;
esac
