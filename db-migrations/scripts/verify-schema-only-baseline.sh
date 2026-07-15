#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MIGRATION_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BASELINE_FILE="${BASELINE_FILE:-${MIGRATION_DIR}/baseline/hive_schema_baseline.sql}"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

test -f "${BASELINE_FILE}" || fail "Missing schema-only baseline: ${BASELINE_FILE}"

if grep -Eiq '^[[:space:]]*(DROP[[:space:]]+TABLE|TRUNCATE[[:space:]]+TABLE|DELETE[[:space:]]+FROM|INSERT[[:space:]]+INTO|REPLACE[[:space:]]+INTO|UPDATE[[:space:]])' "${BASELINE_FILE}"; then
  fail "Schema baseline contains data-destructive or data-writing SQL. Use versioned migrations or seed scripts instead."
fi

if ! grep -Eiq '^[[:space:]]*CREATE[[:space:]]+TABLE' "${BASELINE_FILE}"; then
  fail "Schema baseline does not contain CREATE TABLE statements."
fi

echo "OK: schema-only baseline is safe"
