#!/usr/bin/env bash
set -euo pipefail

RELEASE_SOURCE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_TARGET_DIR="${HIVE_RUNTIME_DIR:-/root/hive}"

case "${RELEASE_TARGET_DIR}" in
  /*) ;;
  *) echo "FAIL: HIVE_RUNTIME_DIR must be an absolute path" >&2; exit 1 ;;
esac

[ -f "${RELEASE_SOURCE_DIR}/scripts/sync-release-files.sh" ] || {
  echo "FAIL: missing scripts/sync-release-files.sh" >&2
  exit 1
}
[ -f "${RELEASE_SOURCE_DIR}/scripts/restart.sh" ] || {
  echo "FAIL: missing scripts/restart.sh" >&2
  exit 1
}

mkdir -p "${RELEASE_TARGET_DIR}"
RELEASE_TARGET_DIR="$(cd "${RELEASE_TARGET_DIR}" && pwd)"
[ "${RELEASE_SOURCE_DIR}" != "${RELEASE_TARGET_DIR}" ] || {
  echo "FAIL: release package and runtime directory must be different" >&2
  exit 1
}

echo "Release source: ${RELEASE_SOURCE_DIR}"
echo "Runtime target: ${RELEASE_TARGET_DIR}"

RELEASE_SOURCE_DIR="${RELEASE_SOURCE_DIR}" RELEASE_TARGET_DIR="${RELEASE_TARGET_DIR}" \
  bash "${RELEASE_SOURCE_DIR}/scripts/sync-release-files.sh"

cd "${RELEASE_TARGET_DIR}"
bash scripts/restart.sh

echo "Hive release completed."
