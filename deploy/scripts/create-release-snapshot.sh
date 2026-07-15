#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_command tar
timestamp="$(date +%Y%m%d_%H%M%S)"
snapshot="release-snapshots/hive-release-${timestamp}.tar.gz"
mkdir -p release-snapshots

items=(docker-compose.yml .env backend/hive-backend.jar management-ui/dist nginx/conf.d RELEASE_BUILD_INFO.txt)
existing=()
for item in "${items[@]}"; do
  [ -e "${item}" ] && existing+=("${item}")
done
tar -czf "${snapshot}" "${existing[@]}"

keep="$(env_value RELEASE_SNAPSHOT_KEEP)"
keep="${keep:-3}"
find release-snapshots -maxdepth 1 -type f -name 'hive-release-*.tar.gz' -printf '%T@ %p\n' \
  | sort -nr | awk -v keep="${keep}" 'NR > keep { sub(/^[^ ]+ /, ""); print }' \
  | while IFS= read -r old; do rm -f -- "${old}"; done

echo "Release snapshot created: ${snapshot}"
