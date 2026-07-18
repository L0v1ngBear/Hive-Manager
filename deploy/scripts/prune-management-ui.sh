#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

manifest_path="management-ui/dist-manifest.sha256"
require_file "${manifest_path}"
require_file RELEASE_BUILD_INFO.txt

expected_manifest_hash="$(metadata_value ManagementUiManifestSha256)"
[ -n "${expected_manifest_hash}" ] || fail "RELEASE_BUILD_INFO.txt is missing ManagementUiManifestSha256"

canonical_manifest="$(mktemp)"
cleanup() {
  rm -f "${canonical_manifest}"
}
trap cleanup EXIT
tr -d '\r' < "${manifest_path}" > "${canonical_manifest}"

actual_manifest_hash="$(file_sha256 "${canonical_manifest}")"
[ "${actual_manifest_hash}" = "${expected_manifest_hash}" ] \
  || fail "management UI release manifest checksum mismatch"

declare -A expected_files=()
while IFS= read -r line; do
  [ -n "${line}" ] || continue
  checksum="${line%%  *}"
  relative_path="${line#*  }"
  [ "${checksum}" != "${relative_path}" ] || fail "invalid management UI manifest entry"
  case "${relative_path}" in
    /*|../*|*/../*|*/..|.|..|'') fail "unsafe management UI manifest path: ${relative_path}" ;;
  esac
  expected_files["${relative_path}"]=1
done < "${canonical_manifest}"

[ "${#expected_files[@]}" -gt 0 ] || fail "management UI release manifest is empty"

removed=0
while IFS= read -r -d '' file; do
  relative_path="${file#management-ui/dist/}"
  if [[ -z ${expected_files[$relative_path]+x} ]]; then
    rm -f -- "${file}"
    removed=$((removed + 1))
  fi
done < <(find management-ui/dist -type f -print0)

find management-ui/dist -depth -type d -empty -delete
echo "Management UI release tree normalized; removed ${removed} stale file(s)."
