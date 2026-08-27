#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

# A direct publish may reuse the runtime directory rather than rsyncing a clean
# release tree. Keep only migration scripts declared by the signed manifest so
# retired scripts cannot make the release-integrity count fail.
manifest_path="db-migrations/migration_manifest.txt"
require_file "${manifest_path}"

canonical_manifest="$(mktemp)"
cleanup() {
  rm -f "${canonical_manifest}"
}
trap cleanup EXIT
tr -d '\r' < "${manifest_path}" > "${canonical_manifest}"

declare -A expected_files=()
while IFS= read -r relative_path; do
  [ -n "${relative_path}" ] || continue
  case "${relative_path}" in
    migrations/*.sql)
      [[ "${relative_path}" =~ ^migrations/[A-Za-z0-9][A-Za-z0-9._-]*\.sql$ ]] \
        || fail "unsafe migration manifest path: ${relative_path}"
      expected_files["${relative_path}"]=1
      ;;
  esac
done < "${canonical_manifest}"

[ "${#expected_files[@]}" -gt 0 ] || fail "migration manifest contains no versioned migration scripts"

removed=0
while IFS= read -r -d '' file; do
  relative_path="migrations/${file#db-migrations/migrations/}"
  if [[ -z ${expected_files["${relative_path}"]+x} ]]; then
    rm -f -- "${file}"
    removed=$((removed + 1))
  fi
done < <(find db-migrations/migrations -maxdepth 1 -type f -name '*.sql' -print0)

for relative_path in "${!expected_files[@]}"; do
  [ -f "db-migrations/${relative_path}" ] \
    || fail "migration declared by manifest is missing: ${relative_path}"
done

echo "Database migration release tree normalized; removed ${removed} stale migration file(s)."
