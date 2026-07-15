#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")/.."

MIGRATION_VERSION="V20260713_001"
RELEASE_INFO="${RELEASE_INFO:-RELEASE_BUILD_INFO.txt}"
MINI_BACKEND_ARTIFACT="${MINI_BACKEND_ARTIFACT:-backend/Hive_Back-0.0.1-SNAPSHOT.jar}"
MANAGEMENT_BACKEND_ARTIFACT="${MANAGEMENT_BACKEND_ARTIFACT:-management-backend/management-0.0.1-SNAPSHOT.jar}"
MANAGEMENT_UI_ARTIFACT="${MANAGEMENT_UI_ARTIFACT:-management-ui/dist}"

fail_blocked() {
  echo "BLOCKED: integrate and rebuild the server and management-web order information-channel artifacts before enabling ${MIGRATION_VERSION}" >&2
  exit 1
}

for command_name in unzip find grep mktemp; do
  command -v "${command_name}" >/dev/null 2>&1 || {
    echo "FAIL: required artifact inspection command is missing: ${command_name}" >&2
    fail_blocked
  }
done

test -f "${RELEASE_INFO}" || {
  echo "FAIL: missing ${RELEASE_INFO}" >&2
  fail_blocked
}

scan_dir="$(mktemp -d)"
trap 'rm -rf "${scan_dir}"' EXIT
issues=()

scan_artifact() {
  local artifact_name="$1"
  local artifact_path="$2"
  local scan_file="${scan_dir}/$3.txt"

  if [ -z "${artifact_path}" ] || { [ ! -f "${artifact_path}" ] && [ ! -d "${artifact_path}" ]; }; then
    issues+=("${artifact_name}: missing artifact ${artifact_path:-<not declared>}")
    return
  fi

  if [ -d "${artifact_path}" ]; then
    if command -v strings >/dev/null 2>&1; then
      find "${artifact_path}" -type f -exec strings {} + > "${scan_file}" 2>/dev/null || {
        issues+=("${artifact_name}: cannot scan directory ${artifact_path}")
        return
      }
    else
      find "${artifact_path}" -type f -exec grep -aEoh 'deliveryDate|delivery_date|informationChannel|information_channel' {} + > "${scan_file}" 2>/dev/null || true
    fi
  else
    unzip -tqq "${artifact_path}" >/dev/null 2>&1 || {
      issues+=("${artifact_name}: cannot inspect archive ${artifact_path}")
      return
    }
    if command -v strings >/dev/null 2>&1; then
      unzip -p "${artifact_path}" 2>/dev/null | strings > "${scan_file}"
    else
      unzip -p "${artifact_path}" 2>/dev/null | grep -aEo 'deliveryDate|delivery_date|informationChannel|information_channel' > "${scan_file}" || true
    fi
  fi

  if grep -Eq 'deliveryDate|delivery_date' "${scan_file}"; then
    issues+=("${artifact_name}: still references deliveryDate/delivery_date")
    if [ -d "${artifact_path}" ]; then
      while IFS= read -r legacy_file; do
        [ -n "${legacy_file}" ] && echo "DETAIL: ${artifact_name} legacy reference: ${legacy_file}" >&2
      done < <(grep -RIlE 'deliveryDate|delivery_date' "${artifact_path}" 2>/dev/null || true)
    fi
  fi
  if ! grep -Eq 'informationChannel|information_channel' "${scan_file}"; then
    issues+=("${artifact_name}: missing informationChannel/information_channel")
  fi
}

scan_artifact "mini backend" "${MINI_BACKEND_ARTIFACT}" "mini-backend"
scan_artifact "management backend" "${MANAGEMENT_BACKEND_ARTIFACT}" "management-backend"
scan_artifact "management UI" "${MANAGEMENT_UI_ARTIFACT}" "management-ui"

if ! grep -qxF 'OrderInformationChannelContract=READY' "${RELEASE_INFO}"; then
  issues+=("release metadata: OrderInformationChannelContract=READY is absent")
fi

if ! grep -qxF 'CancelReasonContract=DEFERRED' "${RELEASE_INFO}"; then
  issues+=("release metadata: CancelReasonContract=DEFERRED is absent")
fi

if ! grep -qxF 'MiniProgramFrontendContract=READY' "${RELEASE_INFO}"; then
  issues+=("release metadata: MiniProgramFrontendContract=READY is absent")
fi

if ! grep -qxF 'MiniProgramWechatDevtoolsVerification=UPLOADED' "${RELEASE_INFO}"; then
  issues+=("release metadata: MiniProgramWechatDevtoolsVerification=UPLOADED is absent")
fi

if [ "${#issues[@]}" -gt 0 ]; then
  for issue in "${issues[@]}"; do
    echo "FAIL: ${issue}" >&2
  done
  fail_blocked
fi

echo "Order information-channel artifact contract passed."
