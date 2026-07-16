#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

for forbidden_path in \
  .env \
  mysql/data \
  redis/data \
  rabbitmq/data \
  uploads \
  logs \
  smoke-reports \
  release-snapshots \
  backups \
  certbot/www \
  mini-program; do
  [ ! -e "${forbidden_path}" ] || fail "upload package contains runtime or local-only path: ${forbidden_path}"
done

for pattern in '*.key' '*.pem' '*.crt' '*.cer' '*.p12' '*.pfx' '*.zip' '*.7z' '*.rar' '*.bak' '*.tmp' '*.log' '*.map'; do
  match="$(find . -type f -name "${pattern}" -print -quit)"
  [ -z "${match}" ] || fail "upload package contains forbidden file ${match} (pattern ${pattern})"
done

for transient_directory in .git node_modules target .vite .cache tests test smoke-reports release-snapshots; do
  match="$(find . -mindepth 1 -type d -name "${transient_directory}" -print -quit)"
  [ -z "${match}" ] || fail "upload package contains transient directory: ${match}"
done

echo "Upload package cleanliness verified. Runtime .env, certificates, and persistent data remain server-owned."
