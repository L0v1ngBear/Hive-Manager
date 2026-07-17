#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_command docker
require_command curl
require_file .env

wait_for_healthy_container hive-backend
wait_for_healthy_container nginx

docker compose exec -T backend sh -c "test -r /app/app.jar"

PUBLIC_DOMAIN="${HIVE_PUBLIC_DOMAIN:-hellohive.top}"
management_home_status="$(curl -ksS --connect-timeout 5 --max-time 20 \
  -o /dev/null -w '%{http_code}' https://127.0.0.1/ -H "Host: ${PUBLIC_DOMAIN}" || true)"
[ "${management_home_status}" = "200" ] || fail "management web home returned HTTP ${management_home_status:-empty}; expected 200"
echo "OK: management web home HTTP 200"

bash scripts/smoke-unified-backend.sh
echo "Unified backend smoke test passed."
