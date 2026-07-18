#!/usr/bin/env bash
set -euo pipefail

# Synchronize an immutable release staging directory into the runtime directory.
# Runtime-owned secrets, certificates, data, uploads, logs and backups are never copied or deleted.
RELEASE_SOURCE_DIR="${RELEASE_SOURCE_DIR:-}"
RELEASE_TARGET_DIR="${RELEASE_TARGET_DIR:-/root/hive}"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

[ -n "${RELEASE_SOURCE_DIR}" ] || fail "Set RELEASE_SOURCE_DIR to the extracted release staging directory"
command -v rsync >/dev/null 2>&1 || fail "Missing command: rsync"

RELEASE_SOURCE_DIR="$(cd "${RELEASE_SOURCE_DIR}" && pwd)"
mkdir -p "${RELEASE_TARGET_DIR}"
RELEASE_TARGET_DIR="$(cd "${RELEASE_TARGET_DIR}" && pwd)"
[ "${RELEASE_SOURCE_DIR}" != "${RELEASE_TARGET_DIR}" ] || fail "Release source and runtime target must be different directories"

for required in \
  docker-compose.yml \
  backend/hive-backend.jar \
  management-ui/dist/index.html \
  management-ui/dist-manifest.sha256 \
  nginx/conf.d/hive.conf \
  scripts/restart.sh \
  db-migrations/migration_manifest.txt; do
  [ -f "${RELEASE_SOURCE_DIR}/${required}" ] || fail "Missing release artifact: ${required}"
done

for forbidden in .env mysql/data redis/data nginx/certs uploads backups; do
  [ ! -e "${RELEASE_SOURCE_DIR}/${forbidden}" ] || fail "Release staging contains runtime-owned path: ${forbidden}"
done

# Files are copied explicitly; directories are mirrored so stale release-owned files disappear.
for file in docker-compose.yml .env.example README.md RELEASE_BUILD_INFO.txt management-ui/dist-manifest.sha256; do
  if [ -f "${RELEASE_SOURCE_DIR}/${file}" ]; then
    install -D -m 0644 "${RELEASE_SOURCE_DIR}/${file}" "${RELEASE_TARGET_DIR}/${file}"
  fi
done

for directory in backend management-ui/dist nginx/conf.d scripts db-migrations mysql/init; do
  if [ -d "${RELEASE_SOURCE_DIR}/${directory}" ]; then
    mkdir -p "${RELEASE_TARGET_DIR}/${directory}"
    rsync -a --delete "${RELEASE_SOURCE_DIR}/${directory}/" "${RELEASE_TARGET_DIR}/${directory}/"
  fi
done

# Nginx workers run as an unprivileged user and must be able to traverse and read the bind-mounted UI tree.
find "${RELEASE_TARGET_DIR}/management-ui/dist" -type d -exec chmod 0755 {} +
find "${RELEASE_TARGET_DIR}/management-ui/dist" -type f -exec chmod 0644 {} +

find "${RELEASE_TARGET_DIR}/scripts" "${RELEASE_TARGET_DIR}/db-migrations/scripts" \
  -type f -name '*.sh' -exec chmod +x {} +

echo "Release files synchronized into ${RELEASE_TARGET_DIR}."
echo "Preserved runtime paths: .env, mysql/data, redis/data, nginx/certs, uploads, backups."
