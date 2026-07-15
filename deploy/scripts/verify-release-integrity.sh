#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_file RELEASE_BUILD_INFO.txt
require_file backend/hive-backend.jar
require_file management-ui/dist/index.html

expected="$(grep '^BackendJarSha256=' RELEASE_BUILD_INFO.txt | cut -d= -f2-)"
[ -n "${expected}" ] || fail "RELEASE_BUILD_INFO.txt is missing BackendJarSha256"
actual="$(file_sha256 backend/hive-backend.jar)"
[ "${actual}" = "${expected}" ] || fail "backend JAR checksum mismatch"

[ "$(find backend -maxdepth 1 -type f -name '*.jar' | wc -l | tr -d ' ')" = "1" ] || fail "release must contain exactly one backend JAR"
bash scripts/inspect-backend-artifact.sh
echo "Release integrity verified (${actual})."
