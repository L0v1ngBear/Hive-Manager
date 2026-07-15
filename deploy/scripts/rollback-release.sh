#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_command docker
require_command tar
snapshot="${1:-$(find release-snapshots -maxdepth 1 -type f -name 'hive-release-*.tar.gz' -printf '%T@ %p\n' | sort -nr | head -n 1 | cut -d' ' -f2-)}"
[ -n "${snapshot}" ] && [ -f "${snapshot}" ] || fail "no release snapshot found"

docker compose stop backend || true
tar -xzf "${snapshot}" -C .
configure_profiles
docker compose up -d --build --force-recreate --remove-orphans backend nginx
wait_for_healthy_container hive-backend
wait_for_healthy_container hive-nginx
bash scripts/smoke-test.sh
echo "Rollback completed from ${snapshot}. Database rollback is intentionally separate and requires a verified backup."
