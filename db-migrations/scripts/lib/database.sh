#!/usr/bin/env bash

load_database_env() {
  DEPLOY_DIR="${DEPLOY_DIR:-/root/hive}"
  DATABASE_NAME="${DATABASE_NAME:-hive}"

  cd "${DEPLOY_DIR}"
  test -f ".env" || {
    echo "FAIL: Missing ${DEPLOY_DIR}/.env" >&2
    return 1
  }
  set -a
  source ./.env
  set +a
  test -n "${MYSQL_ROOT_PASSWORD:-}" || {
    echo "FAIL: .env missing MYSQL_ROOT_PASSWORD" >&2
    return 1
  }
}

mysql_root_db() {
  docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" \
    --default-character-set=utf8mb4 "$@" "${DATABASE_NAME}"
}
