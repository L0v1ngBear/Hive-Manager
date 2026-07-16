#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_file .env
bash scripts/normalize-env.sh .env
require_command docker
configure_profiles
prepare_runtime_directories

bash scripts/check-deploy-health.sh
bash scripts/create-release-snapshot.sh

docker compose pull nginx
if [ "${NO_CACHE:-0}" = "1" ]; then
  docker compose build --no-cache backend
else
  docker compose build backend
fi
docker compose stop backend || true

restore_on_failure() {
  local code=$?
  if [ "${code}" -ne 0 ]; then
    echo "Restart failed; restoring the previous unified container."
    docker compose up -d --force-recreate backend nginx || true
  fi
  exit "${code}"
}
trap restore_on_failure EXIT

bash scripts/migrate-db.sh
docker compose up -d --force-recreate --remove-orphans mysql redis backend nginx
wait_for_healthy_container hive-backend
wait_for_healthy_container nginx
bash scripts/verify-release-integrity.sh
bash scripts/smoke-test.sh

trap - EXIT
echo "Unified backend restart completed."
