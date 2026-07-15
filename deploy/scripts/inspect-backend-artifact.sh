#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_command jar
require_file backend/hive-backend.jar

entries="$(jar tf backend/hive-backend.jar)"
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
