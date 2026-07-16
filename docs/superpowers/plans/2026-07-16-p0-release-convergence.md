# Hive P0 Release Convergence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the unified-backend source repository, database migrations, permission contracts, release metadata, and assembled deployment artifacts reproducible from one pushed Git commit.

**Architecture:** Keep the current `codex/fix-p0-release-convergence` branch as the only integration baseline. Transplant only the focused multi-installer commit, move all migration and release tests to repository-owned inputs, derive release artifacts from the committed source, and make integrity gates verify every artifact family instead of only the backend JAR.

**Tech Stack:** Java 21, Spring Boot 3.1, MyBatis-Plus, Maven, Vue 3, Vite, Node test runner, Bash, MySQL 8, Docker Compose.

## Global Constraints

- Do not create a Git worktree.
- Do not discard or overwrite pre-existing uncommitted user changes.
- Do not modify historical migration files already executed by a server.
- Do not restore `constructionPersonnel` or `constructionPhone` compatibility.
- Do not restore dual-backend routes, packages, JARs, or containers.
- Do not copy, print, commit, or package real `.env` values or TLS private keys.
- Every release metadata commit must be resolvable from the pushed remote branch.

---

### Task 1: Repository-Owned Migration Contract

**Files:**
- Modify: `management-ui/tests/deploy-migration-immutability.test.js`
- Modify: `management-ui/tests/order-note-backend-contract.test.js`
- Create: `management-ui/tests/repository-release-convergence.test.js`
- Create: `db-migrations/migrations/V20260715_001_order_notes_and_material_approval.sql`
- Create: `db-migrations/migrations/V20260715_002_installation_task_installer.sql`
- Modify: `db-migrations/migration_manifest.txt`
- Modify: `db-migrations/migration_checksums.sha256`

**Interfaces:**
- Consumes: repository root resolved relative to each test file.
- Produces: a 76-entry immutable migration tree owned entirely by Git.

- [ ] **Step 1: Write failing repository migration tests**

Add tests that resolve `repoRoot` from `import.meta.url`, reject absolute `C:/Users/...` deployment reads, require both `V20260715_001` and `V20260715_002`, and assert that manifest filenames exactly equal the sorted migration directory filenames.

- [ ] **Step 2: Verify RED**

Run:

```powershell
node --test tests/deploy-migration-immutability.test.js tests/order-note-backend-contract.test.js tests/repository-release-convergence.test.js
```

Expected: FAIL because `_001` and `_002` are absent from the repository migration tree and existing tests depend on the desktop deployment directory.

- [ ] **Step 3: Restore the two immutable migrations**

Copy `_001` byte-for-byte from the current assembled deployment directory. Restore `_002` from commit `a9390ead6223a50798af4276d0781e1f37ab7684`. Append both paths to the manifest and regenerate `migration_checksums.sha256` using lowercase SHA-256 values and repository-relative paths.

- [ ] **Step 4: Verify GREEN**

Run the three tests again and verify all pass. Then run:

```powershell
npm test
```

Expected: all management UI tests pass without reading migration content from an absolute desktop path.

- [ ] **Step 5: Commit**

```powershell
git add db-migrations management-ui/tests/deploy-migration-immutability.test.js management-ui/tests/order-note-backend-contract.test.js management-ui/tests/repository-release-convergence.test.js
git commit -m "fix: restore repository migration authority"
```

### Task 2: Multi-Installer Runtime Convergence

**Files:**
- Restore from commit `a9390ead...`: focused installation mapper, DTO, entity, VO, service, frontend helper, frontend view, and installation tests.
- Modify only when resolving current-branch conflicts: `management/src/main/java/my/hive/domain/installation/**`
- Modify only when resolving current-branch conflicts: `management-ui/src/views/function/installationTask/**`
- Modify: `docs/api/unified-api-catalog.md`
- Modify: `docs/management-ui/modules/installation-task.md`
- Modify: `docs/migrations/unified-backend-migrations.md`

**Interfaces:**
- Consumes: `installation_task_installer` table from Task 1.
- Produces: `InstallationTaskStatusUpdateRequest.installers` and `InstallationTaskVO.installers` with no single-person fields.

- [ ] **Step 1: Run focused tests before implementation**

Restore only the three backend installer tests and two frontend installer tests from `a9390ead...`, then run them before restoring production implementation.

Expected: compilation/test failure because installer classes and `installers[]` runtime support are absent.

- [ ] **Step 2: Transplant focused implementation**

Use `git show a9390ead:<path>` or a no-commit cherry-pick limited to that commit. Preserve current order-note, invoice-warning, permission, Element Plus, and theme work. Resolve installation view conflicts by retaining the current page shell and replacing only old single-person fields with the dynamic installer list.

- [ ] **Step 3: Enforce retired-field scan**

Add a repository contract assertion that production Java, Vue, and API files contain neither `constructionPersonnel` nor `constructionPhone`. Migration SQL may contain the column names only in the `DROP COLUMN` statements.

- [ ] **Step 4: Verify GREEN**

Run:

```powershell
.\mvnw.cmd -Dtest=InstallationTaskInstallerContractTest,InstallationTaskInstallerServiceTest,InstallationTaskInstallerTransactionIntegrationTest test
npm test -- --test-name-pattern="installation task installers|installer migration"
```

Then run full backend and management UI tests.

- [ ] **Step 5: Commit**

```powershell
git add management/src management-ui/src management/src/test management-ui/tests docs db-migrations/scripts
git commit -m "fix: converge installation tasks on multi-installer schema"
```

### Task 3: Permission V3 Source and Dist Enforcement

**Files:**
- Modify: `management-ui/tests/permission-v3-catalog.test.js`
- Modify: `management-ui/tests/frontend-unified-backend-adaptation.test.js`
- Create: `management-ui/tests/release-dist-permission-contract.test.js`
- Preserve current fixes in: `management-ui/src/router/index.js`
- Preserve current fixes in: `management-ui/src/views/function/order/order.vue`
- Preserve current fixes in: `management-ui/src/views/function/customer/customer.vue`
- Preserve current fixes in: `management-ui/src/views/function/approval/approvalCenter.vue`
- Preserve current fixes in: `management-ui/src/views/function/inventory/**`

**Interfaces:**
- Consumes: assignable leaves from `PermissionCatalogV3.java`.
- Produces: source and production-dist permission literals that are all valid exact V3 leaves.

- [ ] **Step 1: Add failing retired-permission tests**

Change the permission scanner so any colon-delimited literal used by `hasPermission`, route `permissions`, permission directives, or permission constants is checked, including the `table` root. Explicitly reject `customer:page`, `table:export`, `approval:order:audit`, wildcards, and aliases retired by the V3 migration.

Add a dist scanner that takes a dist path from `MANAGEMENT_UI_DIST` and scans built JavaScript for the same retired literals.

- [ ] **Step 2: Verify RED against current deployment dist**

```powershell
$env:MANAGEMENT_UI_DIST='C:\Users\HUAWEI\Desktop\hive部署_全新配置\management-ui\dist'
node --test tests/release-dist-permission-contract.test.js
```

Expected: FAIL and report at least `customer:page` and `table:export`.

- [ ] **Step 3: Complete source synchronization**

Keep the current exact permission edits, resolve all remaining retired literals, and ensure route visibility, list requests, detail requests, exports, and audit actions use independent V3 leaves.

- [ ] **Step 4: Build and verify GREEN**

```powershell
npm test
npm run build
$env:MANAGEMENT_UI_DIST=(Resolve-Path dist)
node --test tests/release-dist-permission-contract.test.js
```

Expected: source tests and built-dist scan pass; no retired permission literal exists.

- [ ] **Step 5: Commit**

```powershell
git add management-ui/src management-ui/tests docs/management-ui/modules/approval.md docs/architecture/unified-frontend-contract.md
git commit -m "fix: align management UI with permission v3"
```

### Task 4: Complete Release Integrity and Secret Separation

**Files:**
- Modify: `deploy/scripts/verify-release-integrity.sh`
- Modify: `deploy/scripts/check-deploy-health.sh`
- Create: `deploy/scripts/verify-upload-package.sh`
- Modify: `management-ui/tests/deploy-package-cleanliness.test.js`
- Modify: `management-ui/tests/deploy-secret-hardening.test.js`
- Create: `management-ui/tests/release-integrity-coverage.test.js`
- Modify: `deploy/README.md`
- Modify: `docs/operations/P0-20260714-001-release-incident.md`

**Interfaces:**
- Consumes: fields in `RELEASE_BUILD_INFO.txt` and a repository or assembled release root.
- Produces: nonzero exit on JAR, UI, migration, commit, retired-contract, or upload-secret drift.

- [ ] **Step 1: Write failing gate-coverage tests**

Assert integrity verification reads and validates `BackendJarSha256`, `ManagementUiSha256`, `ManagementUiFileCount`, `MigrationManifestSha256`, `MigrationCount`, `SourceGitCommit`, and `ReleasePackageGitCommit`. Assert upload verification rejects `.env`, `*.key`, production certificate files, runtime data directories, reports, and snapshots.

- [ ] **Step 2: Verify RED**

Run the three release-gate tests. Expected: FAIL because the current integrity script only validates the backend JAR and cleanliness tests allow `.env` and `.key`.

- [ ] **Step 3: Implement full integrity checks**

Add deterministic UI tree hashing matching the metadata algorithm, migration manifest hashing/count checks, `git cat-file -e <commit>^{commit}` checks when `.git` is available, and source/dist retired-contract scans. Keep runtime TLS validation separate from upload-package cleanliness.

- [ ] **Step 4: Verify tamper detection**

Assemble a temporary release fixture outside the real deployment directory. Verify the gate passes unchanged, then separately mutate one UI file, one migration, and metadata commit and confirm each mutation fails. Verify a fixture containing `.env` or `nginx/certs/*.key` fails upload-package validation.

- [ ] **Step 5: Commit**

```powershell
git add deploy/scripts deploy/README.md management-ui/tests docs/operations/P0-20260714-001-release-incident.md
git commit -m "fix: enforce complete release integrity"
```

### Task 5: Mini-Program Source Traceability

**Files:**
- Repository: `D:/productHiveFrontend`
- Preserve current source changes under `client/`.
- Restore or replace deleted regression tests under `client/tests/`.
- Update release metadata only after the mini branch is committed and pushed.

**Interfaces:**
- Consumes: current unified `/api` mini-program source.
- Produces: a pushed commit that exactly identifies the uploaded/releasable mini source.

- [ ] **Step 1: Create a non-worktree repair branch**

Create `codex/fix-p0-mini-traceability` in the existing mini repository while preserving the dirty worktree.

- [ ] **Step 2: Restore regression coverage**

Restore the 18 deleted tests, adapt only those invalidated by the unified backend contract, and keep the new unified contract test. Do not reduce order scan, request security, permission, package-size, or responsive-layout coverage.

- [ ] **Step 3: Verify mini source**

Run all `client/tests/*.test.js`, `node --check` for every client JavaScript file, parse all JSON files, and verify package size/exclude configuration. If WeChat CLI is available, run compile/preview without uploading a new version unless explicitly requested.

- [ ] **Step 4: Commit and push**

Commit the complete mini source and tests, push the branch, and record the pushed commit in release metadata. Do not reuse missing commit `9046ceb...`.

### Task 6: Reproducible Build and Deployment Assembly

**Files:**
- Modify: `RELEASE_BUILD_INFO.txt`
- Synchronize generated artifacts into: `C:/Users/HUAWEI/Desktop/hive部署_全新配置`
- Do not modify: real `.env`, `nginx/certs`, runtime data, uploads, logs, or snapshots.

**Interfaces:**
- Consumes: pushed management and mini source commits.
- Produces: one backend JAR, one management UI dist, 76 migrations, and metadata whose hashes match all artifacts.

- [ ] **Step 1: Run full source verification**

```powershell
cd D:\HiveManager\management
.\mvnw.cmd clean test package
cd D:\HiveManager\management-ui
npm test
npm run build
```

Expected: all tests and both production builds pass.

- [ ] **Step 2: Commit and push the integrated source**

Commit any final documentation/metadata source changes and push `codex/fix-p0-release-convergence`. Verify every metadata commit with both `git cat-file -e` and the remote branch head.

- [ ] **Step 3: Assemble without touching runtime secrets**

Clean-sync only the JAR, UI dist, repository migration tree, version-controlled deployment scripts/configuration, and release metadata. Preserve the existing `.env` and certificate directory in place; exclude them from any upload staging directory.

- [ ] **Step 4: Generate final metadata**

Record exact pushed commits, UTC build time, backend SHA-256/bytes, UI tree SHA-256/file count/index hash, migration manifest SHA-256/count, test counts, unified route smoke result, mini source commit/version, and explicit deferred cancel-reason status.

- [ ] **Step 5: Run final gates**

Run source tests, built-dist permission scan, migration checksum verification, upload-package cleanliness, complete release integrity, backend artifact inspection, and static deployment health. Report release-host-only Docker/TLS/smoke checks as pending until executed on the server; do not mark them passed locally.

## Final Self-Review

- Every P0 root cause has a task and a failing-before/passing-after verification.
- No task modifies an executed historical migration.
- The multi-installer transplant is limited to the focused commit and current-branch conflict resolution.
- Runtime secrets are preserved on the server but excluded from upload artifacts.
- Final artifacts are generated only after the source commits are pushed and resolvable.
