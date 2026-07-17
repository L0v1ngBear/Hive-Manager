#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
DATABASE_NAME="${DATABASE_NAME:-hive}"
MIGRATION_VERSION="migrations/V20260717_001_order_multi_shipment"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

mysql_root_db() {
  docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" \
    --default-character-set=utf8mb4 "$@" "${DATABASE_NAME}"
}

assert_empty_sales_order() {
  local order_row_count="${1:-}"
  case "${order_row_count}" in
    0) return 0 ;;
    ''|*[!0-9]*)
      echo "FAIL: Unable to verify that sales_order is empty; migration is blocked." >&2
      return 1
      ;;
    *)
      echo "FAIL: sales_order contains ${order_row_count} business rows. Run the formal cleanup process before this clean-launch migration." >&2
      return 1
      ;;
  esac
}

check_order_multi_shipment_clean_launch() {
  cd "${DEPLOY_DIR}"
  test -f ".env" || fail "Missing ${DEPLOY_DIR}/.env"
  set -a
  source ./.env
  set +a
  test -n "${MYSQL_ROOT_PASSWORD:-}" || fail ".env missing MYSQL_ROOT_PASSWORD"

  local migration_state
  if ! migration_state="$(mysql_root_db -N -B -e "
SELECT status
FROM schema_migration_history
WHERE version = '${MIGRATION_VERSION}'
LIMIT 1;
" </dev/null)"; then
    fail "Unable to read ${MIGRATION_VERSION} state; migration is blocked."
  fi

  if [ "${migration_state}" = "SUCCESS" ]; then
    echo "Clean-launch gate: ${MIGRATION_VERSION} is already registered SUCCESS; no destructive migration will run."
    return 0
  fi
  if [ -n "${migration_state}" ]; then
    fail "${MIGRATION_VERSION} has non-success state ${migration_state}; migration is blocked."
  fi

  local sales_order_exists
  if ! sales_order_exists="$(mysql_root_db -N -B -e "
SELECT COUNT(*)
FROM information_schema.tables
WHERE table_schema = '${DATABASE_NAME}' AND table_name = 'sales_order';
" </dev/null)"; then
    fail "Unable to verify sales_order existence; migration is blocked."
  fi
  [ "${sales_order_exists}" = "1" ] || fail "sales_order is missing; migration is blocked. Use the formal fresh-database initialization flow."

  local order_row_count
  if ! order_row_count="$(mysql_root_db -N -B -e "SELECT COUNT(*) FROM sales_order;" </dev/null)"; then
    fail "Unable to count sales_order business rows; migration is blocked."
  fi
  assert_empty_sales_order "${order_row_count}" || exit 1
  echo "Clean-launch gate: sales_order is empty; ${MIGRATION_VERSION} may run."
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  check_order_multi_shipment_clean_launch
fi
