#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_file .env
bash scripts/normalize-env.sh .env
require_command docker
configure_profiles
prepare_runtime_directories

bash scripts/prune-management-ui.sh
echo "1/6 Validate release source"
bash scripts/check-deploy-health.sh

echo "2/6 Start data services"
if using_external_mysql; then
  echo "EXTERNAL MySQL mode: the bundled mysql service will not be started."
  docker compose up -d --remove-orphans redis
else
  docker compose up -d --remove-orphans mysql redis
  wait_for_healthy_container mysql
fi
wait_for_healthy_container redis

if [ "$(env_value OPERATION_LOG_QUEUE_TYPE)" = "rabbitmq" ]; then
  docker compose up -d --remove-orphans rabbitmq
  wait_for_healthy_container hive-rabbitmq
fi

echo "3/6 Run the single migration entry"
docker compose stop backend 2>/dev/null || true
bash scripts/migrate-db.sh

echo "4/6 Apply optional scheduler"
if using_external_mysql; then
  echo "EXTERNAL MySQL mode: local xxl-job-admin is not started; configure a separate scheduler endpoint if enabled."
  docker compose stop xxl-job-admin 2>/dev/null || true
elif env_true XXL_JOB_ENABLED; then
  docker compose up -d --remove-orphans xxl-job-admin
else
  docker compose stop xxl-job-admin 2>/dev/null || true
fi

echo "5/6 Start the unified application and gateway"
docker compose up -d --build --force-recreate --remove-orphans backend nginx
wait_for_healthy_container hive-backend
wait_for_healthy_container nginx
remove_retired_backend_containers

echo "6/6 Run smoke checks"
bash scripts/smoke-test.sh
