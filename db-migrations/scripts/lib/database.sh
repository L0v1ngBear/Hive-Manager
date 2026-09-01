#!/usr/bin/env bash

# Database access boundary for deployment and migration scripts. COMPOSE is the
# established default. EXTERNAL deliberately uses a host mysql client so no
# command starts, execs into, or mutates the bundled mysql container.
database_fail() {
  echo "FAIL: $*" >&2
  return 1
}

database_mode() {
  printf '%s' "${HIVE_DATABASE_MODE:-COMPOSE}" | tr '[:lower:]' '[:upper:]'
}

using_external_mysql() {
  [ "$(database_mode)" = "EXTERNAL" ]
}

require_database_value() {
  local key="$1"
  local value="${!key:-}"
  [ -n "${value}" ] && [[ "${value}" != *CHANGE_ME* ]] \
    || database_fail "external MySQL requires ${key} in .env"
}

load_database_env() {
  DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
  DATABASE_NAME="${DATABASE_NAME:-hive}"

  cd "${DEPLOY_DIR}"
  test -f ".env" || database_fail "Missing ${DEPLOY_DIR}/.env" || return 1
  set -a
  source ./.env
  set +a

  case "$(database_mode)" in
    COMPOSE)
      test -n "${MYSQL_ROOT_PASSWORD:-}" \
        || database_fail ".env missing MYSQL_ROOT_PASSWORD" || return 1
      ;;
    EXTERNAL)
      for key in HIVE_DATABASE_JDBC_URL HIVE_DATABASE_APP_USERNAME HIVE_DATABASE_APP_PASSWORD \
        HIVE_DATABASE_ADMIN_HOST HIVE_DATABASE_ADMIN_PORT HIVE_DATABASE_ADMIN_USERNAME HIVE_DATABASE_ADMIN_PASSWORD; do
        require_database_value "${key}" || return 1
      done
      DATABASE_NAME="${HIVE_DATABASE_NAME:-${DATABASE_NAME}}"
      command -v mysql >/dev/null 2>&1 \
        || database_fail "external MySQL requires the mysql client on the release host" || return 1
      ;;
    *)
      database_fail "unsupported HIVE_DATABASE_MODE: $(database_mode) (use COMPOSE or EXTERNAL)" || return 1
      ;;
  esac
}

mysql_root_no_db() {
  if using_external_mysql; then
    MYSQL_PWD="${HIVE_DATABASE_ADMIN_PASSWORD}" mysql --protocol=TCP \
      --host="${HIVE_DATABASE_ADMIN_HOST}" --port="${HIVE_DATABASE_ADMIN_PORT}" \
      --user="${HIVE_DATABASE_ADMIN_USERNAME}" --default-character-set=utf8mb4 "$@"
  else
    docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" \
      --default-character-set=utf8mb4 "$@"
  fi
}

mysql_root_db() {
  local db_name="${DATABASE_NAME}"
  if [ "$#" -gt 0 ] && [[ "$1" != -* ]]; then
    db_name="$1"
    shift
  fi
  mysql_root_no_db "$@" "${db_name}"
}

mysql_root_dump() {
  if using_external_mysql; then
    MYSQL_PWD="${HIVE_DATABASE_ADMIN_PASSWORD}" mysqldump --protocol=TCP \
      --host="${HIVE_DATABASE_ADMIN_HOST}" --port="${HIVE_DATABASE_ADMIN_PORT}" \
      --user="${HIVE_DATABASE_ADMIN_USERNAME}" --default-character-set=utf8mb4 "$@"
  else
    docker compose exec -T mysql mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" \
      --default-character-set=utf8mb4 "$@"
  fi
}

ensure_database_available() {
  if using_external_mysql; then
    mysql_root_no_db -N -B -e 'SELECT 1;' >/dev/null
  else
    docker compose up -d mysql >/dev/null
    docker compose exec -T mysql mysqladmin ping -h 127.0.0.1 -p"${MYSQL_ROOT_PASSWORD}" --silent
  fi
}
