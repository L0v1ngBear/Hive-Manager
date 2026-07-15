#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_command docker
docker compose logs --tail="${TAIL_LINES:-200}" -f backend nginx mysql redis
