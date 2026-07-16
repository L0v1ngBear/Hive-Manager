#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_file backend/hive-backend.jar

list_jar_entries() {
  local artifact="$1"
  if command -v jar >/dev/null 2>&1; then
    jar tf "${artifact}"
    return
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -Z1 "${artifact}"
    return
  fi
  if command -v python3 >/dev/null 2>&1; then
    python3 - "${artifact}" <<'PY'
import sys
import zipfile

with zipfile.ZipFile(sys.argv[1]) as archive:
    for name in archive.namelist():
        print(name)
PY
    return
  fi
  fail "backend artifact inspection requires jar, unzip or python3"
}

entries="$(list_jar_entries backend/hive-backend.jar)"
for required in \
  BOOT-INF/classes/my/hive/HiveApplication.class \
  BOOT-INF/classes/my/hive/api/auth/AdminAuthController.class \
  BOOT-INF/classes/my/hive/api/auth/MiniAuthController.class \
  BOOT-INF/classes/my/hive/api/order/OrderController.class; do
  echo "${entries}" | grep -qx "${required}" || fail "JAR missing ${required}"
done

if echo "${entries}" | grep -Eq 'BOOT-INF/classes/my/(management|hive_back)/'; then
  fail "JAR contains a retired backend package"
fi

[ "$(echo "${entries}" | grep -c 'BOOT-INF/classes/my/hive/HiveApplication.class')" = "1" ] || fail "JAR must contain one application entry"
echo "Unified backend artifact inspection passed."
