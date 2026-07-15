#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_command docker
require_command curl
require_file .env

wait_for_healthy_container hive-backend
wait_for_healthy_container nginx

docker compose exec -T backend sh -c "test -r /app/app.jar"
bash scripts/smoke-unified-backend.sh
echo "Unified backend smoke test passed."
