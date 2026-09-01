#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_file .env
bash scripts/normalize-env.sh .env

mode="$(database_mode)"
[ "${mode}" = "EXTERNAL" ] || fail "verify-external-mysql.sh requires HIVE_DATABASE_MODE=EXTERNAL"

# This read-only entrypoint performs no MySQL writes. It proves that the release host can
# reach the independent database and that it already has a complete Hive schema
# before a restart is allowed to use it.
bash scripts/check-deploy-health.sh
DATABASE_NAME="$(env_value HIVE_DATABASE_NAME)" HIVE_DATABASE_MODE=EXTERNAL \
  bash db-migrations/scripts/check-database-state.sh

echo "External MySQL preflight passed. The next guarded cutover command is: bash scripts/restart.sh"
