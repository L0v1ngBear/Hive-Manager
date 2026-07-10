# Launch Readiness Role and Approval Permission Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the confirmed launch blockers by rebuilding tenant built-in roles, enforcing approval data scopes, preventing unauthorized mini-program requests, and regenerating traceable deployment artifacts.

**Architecture:** A new immutable migration resets only system roles and creates dedicated installation permissions. Management runtime code owns the role catalog for tenants created after migration, while both backends keep the same employee baseline and versioned permission-cache keys. Approval summary capabilities drive the mini-program tabs; backend scope and row checks remain the final security boundary.

**Tech Stack:** Java 21, Spring Boot 3.1.8, MyBatis-Plus, MySQL 8, Redis, Vue 3/Vite, WeChat Mini Program, JUnit 5, Mockito, Node.js static/VM tests, PowerShell and Linux deployment scripts.

## Global Constraints

- Do not modify any previously executed migration file.
- Preserve custom roles, active user-role bindings for retained roles, and valid personal `GRANT`/`DENY` overrides.
- `DENY` continues to win over role and wildcard permissions.
- Do not restore sales-order/production-order permission families or any retired AI feature.
- No frontend control replaces backend permission or row-level authorization.
- Deployment order is migration, management backend, mini backend, online endpoint checks, then mini-program release.

---

### Task 1: Add the Authoritative Built-In Role Migration

**Files:**
- Create: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/db-migrations/migrations/V20260710_003_builtin_role_permission_matrix.sql`
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/db-migrations/migration_manifest.txt`
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/scripts/check-deploy-health.sh`
- Modify: `D:/HiveManager/management-ui/tests/deploy-migration-immutability.test.js`
- Create: `D:/HiveManager/management-ui/tests/builtin-role-matrix.test.js`

**Interfaces:**
- Produces 20 active tenant system roles and five installation permissions.
- Retires `TENANT_OWNER`, `AI_MANAGER`, and `dashboard:ai*` without deleting custom roles.
- Preserves `sys_user_permission` except overrides that point to retired AI permissions.

- [ ] Write a failing Node test asserting the new migration, manifest entry, 20 role codes, employee baseline, deprecated-role migration, installation permissions, and non-destructive SQL.
- [ ] Run `node tests/builtin-role-matrix.test.js`; expect failure because the migration is missing.
- [ ] Create the idempotent SQL using temporary role and permission-allow tables, `INSERT ... WHERE NOT EXISTS`, soft deletes, and `ON DUPLICATE KEY UPDATE is_deleted = 0`.
- [ ] Add the migration to the end of `migration_manifest.txt` and require it from deploy health checks.
- [ ] Run both deployment Node tests and verify they pass.

### Task 2: Keep New Tenants and Installation Permissions Consistent

**Files:**
- Create: `D:/HiveManager/management/src/main/java/my/management/module/sys/service/BuiltInRoleCatalog.java`
- Create: `D:/HiveManager/management/src/main/java/my/management/module/sys/service/BuiltInRoleProvisionService.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/module/tenant/service/TenantManageService.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/module/auth/service/AuthService.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/module/sys/model/enums/PermissionCodeEnum.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/InstallationTaskController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/common/utils/PermissionCacheUtil.java`
- Create: `D:/HiveManager/management/src/test/java/my/management/module/sys/service/BuiltInRoleCatalogTest.java`
- Modify: `D:/HiveManager/management/src/test/java/my/management/architecture/CommercialHardeningStaticTest.java`

**Interfaces:**
- `BuiltInRoleCatalog.definitions()` returns immutable role code/name/permission definitions.
- `BuiltInRoleCatalog.employeeBaselinePermissions()` is the management join baseline.
- `BuiltInRoleProvisionService.ensureTenantRoles(String tenantCode)` creates missing roles and initial permissions without resetting existing customized roles.
- Installation endpoints require `installation:list`, `installation:update`, `installation:attachment:upload`, or `installation:attachment:download`.

- [ ] Write failing catalog and architecture tests for role count, unique codes, employee inheritance, no AI/legacy order permissions, installation annotations, and versioned cache keys.
- [ ] Run the focused tests and confirm expected failures.
- [ ] Implement the catalog with shared base/staff/manager permission composition.
- [ ] Implement missing-role provisioning and invoke it during tenant owner initialization; use the catalog baseline in management organization join.
- [ ] Replace installation endpoint `order:list` annotations with dedicated permissions.
- [ ] Change management permission cache key segment from `perm` to `perm-v2` for both management and mini eviction keys.
- [ ] Run focused and full management backend tests.

### Task 3: Enforce Mini Approval Scope and Capability Boundaries

**Files:**
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/approval/model/vo/ApprovalSummaryVO.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/approval/service/ApprovalCenterService.java`
- Create: `D:/HiveBackend/server/src/main/java/my/hive_back/module/approval/service/ApprovalAccessService.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/api/approval/ApprovalController.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/leave/service/LeaveService.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/finance/service/FinanceApprovalService.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/resignation/service/ResignationApprovalService.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/user/service/UserService.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/common/interceptor/TenantInterceptor.java`
- Modify: `D:/HiveBackend/server/src/test/java/my/hive_back/module/approval/service/ApprovalCenterServiceTest.java`
- Create: `D:/HiveBackend/server/src/test/java/my/hive_back/module/approval/service/ApprovalAccessServiceTest.java`

**Interfaces:**
- Summary adds `canViewOrder`, `canViewQuality`, `canReviewFinance`, `canReviewLeave`, and `canReviewResignation`.
- `ApprovalAccessService.requireListScope(type, scope)` permits `mine` for submitters and restricts `pending`/`all` to reviewers.
- `ApprovalAccessService.requireDetailAccess(...)` permits only the applicant, assigned auditor, or a broad reviewer.
- Mini permission cache key segment becomes `perm-v2`.

- [ ] Write failing capability, scope, and row-access tests.
- [ ] Run focused tests and verify missing methods/fields cause the failures.
- [ ] Implement strict scope normalization and permission checks; return business code 403 for denied access.
- [ ] Remove static list annotations only where dynamic scope authorization replaces them; retain submit/audit annotations.
- [ ] Enforce detail row checks after tenant-scoped entity lookup.
- [ ] Reduce mini organization-join fallback to the exact employee baseline.
- [ ] Version the mini permission cache key and run focused/full backend tests.

### Task 4: Make the Mini Approval Page Permission-Driven

**Files:**
- Modify: `D:/productHiveFrontend/client/pages/approval/approval.js`
- Modify: `D:/productHiveFrontend/client/pages/approval/approval.wxml`
- Modify: `D:/productHiveFrontend/client/pages/approval/approval.wxss`
- Modify: `D:/productHiveFrontend/client/tests/approval-create-entry.test.js`

**Interfaces:**
- `buildTabs(summary)` returns tab items with `accessible` and `reviewable` flags.
- `resolveInitialApprovalState(summary, preferredTab)` selects an accessible tab and allowed scope.
- `refreshAll()` awaits summary before requesting a list.
- Create-only approval types expose only `mine`; inaccessible tabs are disabled and never issue requests.

- [ ] Extend the VM test to fail on current concurrent fetching, accessible-tab selection, disabled-tab guard, scope restriction, and summary failure copy.
- [ ] Run the focused test and verify it fails for the expected missing behavior.
- [ ] Implement summary-first loading and state resolution.
- [ ] Render disabled tabs and filtered scope controls; distinguish load failure from permission denial.
- [ ] Run the focused test, every mini test, JSON parsing, JavaScript syntax, and package-size check.

### Task 5: Build and Synchronize the Release Package

**Files:**
- Rebuild: `D:/HiveCommon/hive-backend-common/target/hive-backend-common-0.1.0.jar`
- Rebuild: `D:/HiveManager/management/target/management-0.0.1-SNAPSHOT.jar`
- Rebuild: `D:/HiveManager/management-ui/dist/**`
- Rebuild: `D:/HiveBackend/server/target/Hive_Back-0.0.1-SNAPSHOT.jar`
- Replace: deployment package backend JARs and management UI dist.
- Update: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/RELEASE_BUILD_INFO.txt`
- Update: deployment package mini-program release artifact using the existing packaging exclusion rules.

**Interfaces:**
- Release build info records exact source commits and SHA-256 values.
- Docker build contexts contain exactly one current JAR each.
- Mini package excludes tests, docs, audits, Git metadata, private IDE config, and remains below 2 MiB.

- [ ] Install the tested common module into the local Maven repository.
- [ ] Run clean package builds for both backends and a production Vite build.
- [ ] Copy artifacts with verified hashes and regenerate release metadata.
- [ ] Build the mini-program source ZIP and verify included paths and size.
- [ ] Run PowerShell low-cost checks, release-integrity equivalents, secret checks, migration manifest comparison, source tests, and artifact hash checks.
- [ ] Record server-only gates: certificate renewal, `docker compose config`, migrations with backup, container hash comparison, smoke test, runtime stability, and authenticated approval checks.

### Task 6: Final Launch Audit

**Files:**
- Verify only; no new production behavior.

- [ ] Confirm every source repository is clean, committed, and pushed to its intended remote branch.
- [ ] Confirm deployment artifacts match release metadata and current commits.
- [ ] Confirm online unauthenticated gates return accepted statuses and no 502/503/504.
- [ ] Confirm certificate has more than 30 days remaining after renewal.
- [ ] Confirm authenticated ordinary employee, department manager, and `ADMIN` scenarios against the role matrix.
- [ ] Run `scripts/smoke-test.sh`, `scripts/check-runtime-stability.sh`, backup verification, and commercial-readiness check on the server.
- [ ] Mark launch ready only when all local and server-only evidence is green; otherwise report explicit blockers without weakening the gate.
