#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_file .env
bash scripts/normalize-env.sh .env
require_command docker
configure_profiles
prepare_runtime_directories

bash scripts/prune-management-ui.sh
bash scripts/check-deploy-health.sh

build_args=()
if [ "${PULL_IMAGES:-0}" = "1" ]; then
  docker compose pull nginx
  build_args+=(--pull)
fi
if [ "${NO_CACHE:-0}" = "1" ]; then
  build_args+=(--no-cache)
fi
docker compose build "${build_args[@]}" backend
docker compose stop backend || true

if ! bash scripts/migrate-db.sh; then
  echo "FAIL: database migration failed; backend remains stopped to prevent incompatible code from serving traffic." >&2
  echo "Inspect the migration error before starting the existing container manually." >&2
  exit 1
fi

docker compose up -d --force-recreate --remove-orphans backend nginx
wait_for_healthy_container hive-backend
wait_for_healthy_container nginx
remove_retired_backend_containers
bash scripts/verify-release-integrity.sh
bash scripts/smoke-test.sh

echo "Unified backend restart completed."
