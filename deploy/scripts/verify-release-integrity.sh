#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_file RELEASE_BUILD_INFO.txt
require_file backend/hive-backend.jar
require_file management-ui/dist/index.html
require_file db-migrations/migration_manifest.txt
require_file db-migrations/migration_checksums.sha256

require_metadata() {
  local key="$1"
  local value
  value="$(metadata_value "${key}")"
  [ -n "${value}" ] || fail "RELEASE_BUILD_INFO.txt is missing ${key}"
  printf '%s' "${value}"
}

backend_expected="$(require_metadata BackendJarSha256)"
backend_actual="$(file_sha256 backend/hive-backend.jar)"
[ "${backend_actual}" = "${backend_expected}" ] || fail "backend JAR checksum mismatch"

[ "$(find backend -maxdepth 1 -type f -name '*.jar' | wc -l | tr -d ' ')" = "1" ] || fail "release must contain exactly one backend JAR"

ui_manifest="$(mktemp)"
cleanup() {
  rm -f "${ui_manifest}"
}
trap cleanup EXIT

while IFS= read -r -d '' file; do
  relative_path="${file#management-ui/dist/}"
  printf '%s  %s\n' "$(file_sha256 "${file}")" "${relative_path}"
done < <(find management-ui/dist -type f -print0 | LC_ALL=C sort -z) > "${ui_manifest}"

ui_expected="$(require_metadata ManagementUiSha256)"
ui_actual="$(file_sha256 "${ui_manifest}")"
[ "${ui_actual}" = "${ui_expected}" ] || fail "management UI tree checksum mismatch"

ui_count_expected="$(require_metadata ManagementUiFileCount)"
ui_count_actual="$(wc -l < "${ui_manifest}" | tr -d ' ')"
[ "${ui_count_actual}" = "${ui_count_expected}" ] || fail "management UI file count mismatch"

ui_index_expected="$(require_metadata ManagementUiIndexSha256)"
ui_index_actual="$(file_sha256 management-ui/dist/index.html)"
[ "${ui_index_actual}" = "${ui_index_expected}" ] || fail "management UI index checksum mismatch"

migration_manifest_expected="$(require_metadata MigrationManifestSha256)"
migration_manifest_actual="$(file_sha256 db-migrations/migration_manifest.txt)"
[ "${migration_manifest_actual}" = "${migration_manifest_expected}" ] || fail "migration manifest checksum mismatch"

migration_count_expected="$(require_metadata MigrationCount)"
migration_count_actual="$(grep -c '^migrations/' db-migrations/migration_manifest.txt | tr -d ' ')"
[ "${migration_count_actual}" = "${migration_count_expected}" ] || fail "migration manifest count mismatch"
[ "$(find db-migrations/migrations -maxdepth 1 -type f -name '*.sql' | wc -l | tr -d ' ')" = "${migration_count_expected}" ] || fail "migration file count mismatch"

if command -v sha256sum >/dev/null 2>&1; then
  (cd db-migrations && sha256sum -c migration_checksums.sha256 >/dev/null)
else
  fail "missing command: sha256sum"
fi

for retired_permission in customer:page table:export approval:order:audit document:breadcrumbs; do
  if grep -R -F -q -- "${retired_permission}" management-ui/dist; then
    fail "management UI contains retired permission: ${retired_permission}"
  fi
done

git_root="${SOURCE_REPOSITORY_ROOT:-}"
if [ -z "${git_root}" ] && command -v git >/dev/null 2>&1 && git -C "${DEPLOY_ROOT}" rev-parse --show-toplevel >/dev/null 2>&1; then
  git_root="$(git -C "${DEPLOY_ROOT}" rev-parse --show-toplevel)"
fi
if [ -n "${git_root}" ]; then
  [ -d "${git_root}" ] || fail "SOURCE_REPOSITORY_ROOT does not exist: ${git_root}"
  for commit_field in SourceGitCommit ReleasePackageGitCommit; do
    commit="$(require_metadata "${commit_field}")"
    git -C "${git_root}" cat-file -e "${commit}^{commit}" 2>/dev/null || fail "${commit_field} is not a resolvable commit: ${commit}"
  done
else
  echo "WARN: Git metadata resolution deferred; set SOURCE_REPOSITORY_ROOT on the build host."
fi

bash scripts/inspect-backend-artifact.sh
echo "Release integrity verified (backend=${backend_actual}, ui=${ui_actual}, migrations=${migration_count_actual})."
