#!/bin/bash
set -euo pipefail

DEFAULT_DEPLOY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_ROOT="${HIVE_RELEASE_ROOT:-${DEFAULT_DEPLOY_ROOT}}"
case "${DEPLOY_ROOT}" in
  /*) ;;
  *) fail_message="HIVE_RELEASE_ROOT must be an absolute path"; echo "FAIL: ${fail_message}" >&2; exit 1 ;;
esac
[ -d "${DEPLOY_ROOT}" ] || { echo "FAIL: release root does not exist: ${DEPLOY_ROOT}" >&2; exit 1; }
DEPLOY_ROOT="$(cd "${DEPLOY_ROOT}" && pwd)"
cd "${DEPLOY_ROOT}"

fail() {
  echo "FAIL: $*" >&2
  exit 1
}
require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

require_file() {
  test -f "$1" || fail "missing file: $1"
}

env_value() {
  local key="$1"
  grep -E "^${key}=" .env 2>/dev/null | tail -n 1 | cut -d= -f2- | tr -d '\r' | sed -e 's/^"//' -e 's/"$//' || true
}

env_true() {
  local value
  value="$(env_value "$1")"
  [ "${value}" = "true" ] || [ "${value}" = "TRUE" ] || [ "${value}" = "1" ]
}

configure_profiles() {
  local profiles=()
  [ "$(env_value OPERATION_LOG_QUEUE_TYPE)" = "rabbitmq" ] && profiles+=(rabbitmq)
  env_true XXL_JOB_ENABLED && profiles+=(scheduler)
  if [ "${#profiles[@]}" -gt 0 ]; then
    COMPOSE_PROFILES="$(IFS=,; echo "${profiles[*]}")"
    export COMPOSE_PROFILES
  else
    unset COMPOSE_PROFILES || true
  fi
}

prepare_runtime_directories() {
  mkdir -p logs/backend uploads mysql/data redis/data rabbitmq/data certbot/www smoke-reports release-snapshots management-ui/dist
  chown -R 10001:10001 logs/backend uploads 2>/dev/null || true
  chmod -R u+rwX,g+rwX logs/backend uploads 2>/dev/null || true
}

file_sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    openssl dgst -sha256 "$1" | awk '{print $2}'
  fi
}

metadata_value() {
  local key="$1"
  grep -E "^${key}=" RELEASE_BUILD_INFO.txt 2>/dev/null | tail -n 1 | cut -d= -f2- | tr -d '\r' || true
}

wait_for_healthy_container() {
  local container="$1"
  local attempts="${2:-40}"
  local state=""
  for _ in $(seq 1 "${attempts}"); do
    state="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container}" 2>/dev/null || true)"
    [ "${state}" = "healthy" ] || [ "${state}" = "running" ] || { sleep 3; continue; }
    return 0
  done
  fail "container did not become healthy: ${container} (${state:-missing})"
}
