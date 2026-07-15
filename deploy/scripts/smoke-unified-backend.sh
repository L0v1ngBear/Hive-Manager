#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_command curl
base_url="${HIVE_PUBLIC_BASE_URL:-https://hellohive.top}"
work_dir="$(mktemp -d)"
trap 'rm -rf "${work_dir}"' EXIT

curl_options=(-ksS --connect-timeout 5 --max-time 20)
if [ "${base_url}" = "https://hellohive.top" ]; then
  curl_options+=(--resolve hellohive.top:443:127.0.0.1)
fi

expected_build=""
expected_instance=""

header_value() {
  local header_file="$1"
  local name="$2"
  awk -v wanted="${name}" '
    tolower($0) ~ "^" tolower(wanted) ":" {
      sub(/^[^:]+:[[:space:]]*/, "")
      sub(/\r$/, "")
      value=$0
    }
    END { print value }
  ' "${header_file}"
}

probe() {
  local label="$1"
  local method="$2"
  local path="$3"
  local allowed="$4"
  local payload="${5:-}"
  local slug="${label//[^a-zA-Z0-9]/-}"
  local headers="${work_dir}/${slug}.headers"
  local body="${work_dir}/${slug}.body"
  local status
  local build
  local instance
  local args=("${curl_options[@]}" -D "${headers}" -o "${body}" -w '%{http_code}' -X "${method}")

  if [ -n "${payload}" ]; then
    args+=(-H 'Content-Type: application/json' -d "${payload}")
  fi
  status="$(curl "${args[@]}" "${base_url}${path}" || true)"
  echo ",${allowed}," | grep -q ",${status}," || fail "${label} returned HTTP ${status:-empty}; expected ${allowed}"

  build="$(header_value "${headers}" X-Hive-Build)"
  instance="$(header_value "${headers}" X-Hive-Instance)"
  [ -n "${build}" ] || fail "${label} is missing X-Hive-Build"
  [ -n "${instance}" ] || fail "${label} is missing X-Hive-Instance"

  if [ -z "${expected_build}" ]; then
    expected_build="${build}"
    expected_instance="${instance}"
  else
    [ "${build}" = "${expected_build}" ] || fail "${label} came from a different build"
    [ "${instance}" = "${expected_instance}" ] || fail "${label} came from a different backend process"
  fi
  echo "OK: ${label} HTTP ${status} build=${build} instance=${instance}"
}

probe "application health" GET /api/health 200
probe "admin login rejection" POST /api/auth/admin/login 400 '{}'
probe "mini login rejection" POST /api/auth/mini/login 400 '{}'
probe "current user" GET /api/auth/me 401
probe "employee query" GET /api/emp/employee/page 401
probe "orders" GET /api/orders 401
probe "approval summary" GET /api/approval/summary 401
probe "inventory summary" GET /api/inventory/summary 401
probe "notifications" GET /api/notifications/page 401
probe "print tasks" GET /api/print-task/recent 401

echo "Unified route smoke test passed: all probes resolved to one build and one process."
