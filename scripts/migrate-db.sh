#!/bin/bash
set -euo pipefail

# Single online database migration entrypoint.
# Flow: preflight -> backup -> versioned migrations -> schema verification.
cd "$(dirname "$0")/.."

test -f "scripts/verify-order-information-channel-artifacts.sh" || {
  echo "FAIL: Missing scripts/verify-order-information-channel-artifacts.sh" >&2
  exit 1
}
bash scripts/verify-order-information-channel-artifacts.sh

if [ -f "scripts/normalize-env.sh" ]; then
  bash scripts/normalize-env.sh .env
fi

DATABASE_NAME="${DATABASE_NAME:-hive}"
SKIP_BACKUP="${SKIP_BACKUP:-NO}"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

require_executable() {
  local file_path="$1"
  test -x "${file_path}" || fail "Missing executable file: ${file_path}. Run: chmod +x scripts/*.sh db-migrations/scripts/*.sh mysql/init/*.sh"
}

test -f ".env" || fail "Missing .env"
test -f "db-migrations/migration_manifest.txt" || fail "Missing db-migrations/migration_manifest.txt"
require_executable "db-migrations/scripts/preflight-online.sh"
require_executable "db-migrations/scripts/run-versioned-migrations.sh"
require_executable "db-migrations/scripts/verify-online-schema.sh"

echo "Database migration target: ${DATABASE_NAME}"
echo "Migration manifest: db-migrations/migration_manifest.txt"

echo "1/4 Run database preflight..."
DATABASE_NAME="${DATABASE_NAME}" bash db-migrations/scripts/preflight-online.sh

echo "2/4 Backup before migration..."
if [ "${SKIP_BACKUP}" = "YES" ]; then
  echo "SKIP_BACKUP=YES detected. Only use this immediately after a confirmed full rebuild with no new business data."
else
  require_executable "db-migrations/scripts/backup-online.sh"
  DATABASE_NAME="${DATABASE_NAME}" bash db-migrations/scripts/backup-online.sh
  test -f "scripts/verify-latest-backup.sh" || fail "Missing scripts/verify-latest-backup.sh"
  DATABASE_NAME="${DATABASE_NAME}" MAX_BACKUP_AGE_HOURS="${BACKUP_VERIFY_MAX_AGE_HOURS:-2}" bash scripts/verify-latest-backup.sh
fi

if [ "${AUTO_RESTORE_MIGRATION_DRIFT:-YES}" = "YES" ] && [ -x "scripts/diagnose-migration-drift.sh" ]; then
  echo "2.5/4 Check and restore known migration drift..."
  ALLOW_NO_HISTORY=YES CONFIRM_RESTORE_MIGRATION=YES DATABASE_NAME="${DATABASE_NAME}" VERSION="migrations/V20260705_004_installation_task_schema" bash scripts/diagnose-migration-drift.sh
fi

echo "3/4 Run versioned migrations..."
DATABASE_NAME="${DATABASE_NAME}" RUN_PREFLIGHT=NO bash db-migrations/scripts/run-versioned-migrations.sh

echo "4/4 Verify migrated schema..."
DATABASE_NAME="${DATABASE_NAME}" bash db-migrations/scripts/verify-online-schema.sh

echo "Database migration finished."
