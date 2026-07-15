#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

readonly confirmation="${CONFIRM_FRESH_BUSINESS_RESET:-NO}"
require_file .env
require_file db-migrations/manual/V20260712_001_reset_tenant_001_business_data.sql
require_file db-migrations/manual/V20260711_001_remove_tenant_002.sql

if [ "${confirmation}" != "YES" ]; then
  echo "Preview only: the fresh-release reset will preserve the TENANT_001 administrator/catalog, remove business rows, remove TENANT_002, flush the dedicated Redis database, and clear uploads."
  echo "Run only after reviewing a verified backup:"
  echo "  CONFIRM_FRESH_BUSINESS_RESET=YES bash scripts/reset-fresh-business-data.sh"
  exit 0
fi

require_command docker

mysql_root_password="$(env_value MYSQL_ROOT_PASSWORD)"
[ -n "${mysql_root_password}" ] || fail ".env missing MYSQL_ROOT_PASSWORD"

echo "1/6 Stop application writes"
docker compose stop backend 2>/dev/null || true

echo "2/6 Create and verify a database backup"
bash db-migrations/scripts/backup-online.sh
bash scripts/verify-latest-backup.sh

echo "3/6 Reset TENANT_001 business data while preserving its administrator and catalogs"
docker compose exec -T mysql mysql -uroot -p"${mysql_root_password}" hive \
  < db-migrations/manual/V20260712_001_reset_tenant_001_business_data.sql

echo "4/6 Remove the retired secondary tenant"
docker compose exec -T mysql mysql -uroot -p"${mysql_root_password}" hive \
  < db-migrations/manual/V20260711_001_remove_tenant_002.sql

echo "5/6 Clear the dedicated Redis database"
docker compose exec -T redis redis-cli FLUSHDB >/dev/null

echo "6/6 Clear uploaded business artifacts"
mkdir -p uploads
deploy_real="$(cd "${DEPLOY_ROOT}" && pwd -P)"
uploads_real="$(cd uploads && pwd -P)"
[ "${uploads_real}" = "${deploy_real}/uploads" ] || fail "uploads path escaped the deployment root"
find "${uploads_real}" -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +

echo "Fresh business-data reset completed. Run the single migration entry before starting the backend."
