# Hive One-Command Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a root-level release wrapper and produce a clean deployment directory that publishes Hive with one command.

**Architecture:** A small Bash wrapper resolves the uploaded package directory, validates source and target separation, and delegates to the existing allowlisted synchronization and restart entrypoints. Existing synchronization, migration, backup, permission normalization, and smoke-test behavior remains authoritative.

**Tech Stack:** Bash, Docker Compose, Node.js test runner, PowerShell packaging verification.

## Global Constraints

- Default runtime target is exactly `/root/hive`.
- `HIVE_RUNTIME_DIR` is the only target override.
- The wrapper must not bypass synchronization, migration safety, backup, static-file permission normalization, or smoke verification.
- The clean upload package must exclude `.env`, TLS certificates, database files, uploads, backups, logs, and mini-program source files.

---

### Task 1: One-command release wrapper and clean package

**Files:**
- Create: `deploy/publish.sh`
- Modify: `management-ui/tests/release-runtime-safety.test.js`
- Modify: `deploy/README.md`
- Package: `C:/Users/HUAWEI/Desktop/hive发布_2.0.0_<commit>`

**Interfaces:**
- Consumes: `scripts/sync-release-files.sh`, `scripts/restart.sh`, optional `HIVE_RUNTIME_DIR`.
- Produces: root command `bash publish.sh`, with non-zero exit status on any failed release stage.

- [ ] **Step 1: Write the failing wrapper contract test**

Add assertions that `deploy/publish.sh` uses `BASH_SOURCE[0]`, defaults `HIVE_RUNTIME_DIR` to `/root/hive`, rejects identical source and target paths, calls `scripts/sync-release-files.sh` with explicit environment variables, and invokes `scripts/restart.sh` from the runtime directory.

- [ ] **Step 2: Run the focused test and verify failure**

Run: `node --test tests/release-runtime-safety.test.js`

Expected: FAIL because `deploy/publish.sh` does not exist.

- [ ] **Step 3: Implement the minimal wrapper**

```bash
#!/usr/bin/env bash
set -euo pipefail

RELEASE_SOURCE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_TARGET_DIR="${HIVE_RUNTIME_DIR:-/root/hive}"
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
```

- [ ] **Step 4: Document the single release command**

Document only:

```bash
cd /path/to/uploaded-release
bash publish.sh
```

State that the runtime target defaults to `/root/hive` and all existing safety gates remain active.

- [ ] **Step 5: Run verification**

Run: `npm test` from `management-ui`.

Expected: 299 tests pass with zero failures.

- [ ] **Step 6: Commit and assemble the clean package**

Commit the wrapper, tests, and documentation. Copy release-owned artifacts into a new clean directory named with the resulting short commit. Verify artifact hashes, wrapper presence, source commit metadata, and absence of all forbidden runtime paths.
