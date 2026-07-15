#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_command docker
configure_profiles
docker compose down
echo "Hive services stopped. Persistent data was retained."
