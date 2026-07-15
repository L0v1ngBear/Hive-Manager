#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_file .env
require_command docker
configure_profiles

if [ "$(env_value OPERATION_LOG_QUEUE_TYPE)" != "rabbitmq" ]; then
  [ -z "$(docker compose ps -q rabbitmq 2>/dev/null)" ] || fail "RabbitMQ is running while the in-memory queue is selected"
fi
if ! env_true XXL_JOB_ENABLED; then
  [ -z "$(docker compose ps -q xxl-job-admin 2>/dev/null)" ] || fail "XXL-JOB admin is running while scheduling is disabled"
fi

[ "$(docker compose ps -q backend | wc -l | tr -d ' ')" = "1" ] || fail "exactly one backend service must be running"
wait_for_healthy_container hive-backend
echo "Low-cost single-backend mode verified."
