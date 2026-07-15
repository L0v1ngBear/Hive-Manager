#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_command docker
require_command curl
require_file .env

wait_for_healthy_container hive-backend
wait_for_healthy_container hive-nginx

base_url="${HIVE_PUBLIC_BASE_URL:-https://hellohive.top}"
curl_options=(-kfsS --connect-timeout 5 --max-time 15)
if [ "${base_url}" = "https://hellohive.top" ]; then
  curl_options+=(--resolve hellohive.top:443:127.0.0.1)
fi

curl "${curl_options[@]}" "${base_url}/health" >/dev/null

probe_api() {
  local path="$1"
  local payload="$2"
  local status
  status="$(curl "${curl_options[@]}" -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' -d "${payload}" "${base_url}${path}" || true)"
  case "${status}" in
    200|400|401|403) ;;
    *) fail "API probe ${path} returned HTTP ${status:-empty}" ;;
  esac
}

probe_api /api/auth/admin/login '{}'
probe_api /api/auth/mini/login '{}'
docker compose exec -T backend sh -c "test -r /app/app.jar"
echo "Unified backend smoke test passed."
