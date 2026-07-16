# Hive Release, Database, and Order Audit Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make fresh and existing database releases deterministic, prevent failed releases from starting an invalid backend, remove stale server artifacts safely, and expose tenant-isolated order operation records.

**Architecture:** Separate runtime-owned data from replaceable release artifacts. Add explicit database-state gates and a new current baseline instead of mutating historical migrations. Read order operations through the unified order API and render them beside status transitions.

**Tech Stack:** Bash, Docker Compose, MySQL 8, Spring Boot 3, MyBatis Plus, Vue 3, Element Plus, WeChat Mini Program, Node test runner, Maven/JUnit.

## Global Constraints

- Do not modify historical `db-migrations/migrations/V*.sql` files.
- Do not delete or force-recreate MySQL/Redis during a routine application release.
- Do not start the backend after migration or schema verification failure.
- Preserve server `.env`, certificates, data, uploads, logs, backups, reports, and snapshots.
- Keep `operation_log` restricted to the order module.

---

### Task 1: Database State Gate and Fresh Baseline

**Files:**
- Create: `db-migrations/baseline/hive_schema_baseline_v2.sql`
- Create: `db-migrations/scripts/check-database-state.sh`
- Create: `db-migrations/scripts/initialize-fresh-database.sh`
- Modify: `db-migrations/scripts/import-baseline-to-shadow.sh`
- Modify: `db-migrations/scripts/verify-online-schema.sh`
- Modify: `scripts/migrate-db.sh`
- Test: `management-ui/tests/fresh-database-initialization.test.js`

- [ ] Write contract tests that require an explicit baseline-v2 marker, core-table gate, confirmation variable, schema verification, and no historical SQL edits.
- [ ] Run `node --test tests/fresh-database-initialization.test.js` and confirm the new contracts fail.
- [ ] Generate the schema-only baseline from a verified MySQL shadow schema and add the explicit initialization/state scripts.
- [ ] Run the contract test and import the baseline into a disposable local MySQL database.

### Task 2: Safe Release and Restore Workflow

**Files:**
- Create: `deploy/scripts/sync-release-files.sh`
- Create: `db-migrations/scripts/restore-verified-backup.sh`
- Modify: `deploy/scripts/restart.sh`
- Modify: `deploy/scripts/create-release-snapshot.sh`
- Modify: `deploy/scripts/verify-latest-backup.sh`
- Modify: `db-migrations/scripts/rebuild-mysql-from-baseline.sh`
- Test: `management-ui/tests/release-runtime-safety.test.js`

- [ ] Write failing tests that forbid force-recreating data services, forbid backend restart on migration failure, require unified `backend` shutdown for rebuild, and require required-table backup checks.
- [ ] Run the focused test and confirm failures match the audited defects.
- [ ] Implement allowlisted synchronization, guarded restore, safe restart, and unified-service rebuild behavior.
- [ ] Run the focused test and existing deployment tests.

### Task 3: Single-Tenant Runtime Configuration

**Files:**
- Modify: `deploy/scripts/check-deploy-health.sh`
- Modify: `deploy/.env.example`
- Modify: `deploy/README.md`
- Test: `management-ui/tests/deploy-secret-hardening.test.js`

- [ ] Add failing assertions for `TENANT_001` only, tenant count `1`, and upload/runtime directory separation.
- [ ] Add health-check failures for stale `TENANT_002` configuration and document preservation rules.
- [ ] Run the focused deployment tests.

### Task 4: Order Operation Query Contract

**Files:**
- Create: `management/src/main/java/my/hive/domain/order/model/vo/OrderOperationLogVO.java`
- Create: `management/src/main/java/my/hive/domain/order/mapper/OrderOperationLogMapper.java`
- Create: `management/src/main/java/my/hive/domain/order/service/OrderOperationLogService.java`
- Modify: `management/src/main/java/my/hive/api/order/OrderController.java`
- Test: `management/src/test/java/my/hive/domain/order/service/OrderOperationLogServiceTest.java`

- [ ] Write failing service tests for tenant isolation, order business-number filtering, safe fields, and newest-first ordering.
- [ ] Add `GET /orders/{orderId}/operation-log` guarded by `order:detail` and log status-time correction as an order mutation.
- [ ] Run the focused Maven tests.

### Task 5: Management and Mini-Program Operation Timeline

**Files:**
- Modify: `management-ui/src/views/function/order/api/order.js`
- Modify: `management-ui/src/views/function/order/order.vue`
- Modify: the canonical mini-program `pages/orderDetail/orderDetail.js`
- Modify: the canonical mini-program `pages/orderDetail/orderDetail.wxml`
- Modify: the canonical mini-program `pages/orderDetail/orderDetail.wxss`
- Test: `management-ui/tests/order-operation-log-ui.test.js`

- [ ] Write failing UI contract tests for independent operation-record loading, rendering, empty/error states, and no sensitive payload output.
- [ ] Implement the management view and canonical mini-program detail section.
- [ ] Run UI tests and synchronize the verified mini-program source into release assembly only after its own tests pass.

### Task 6: Documentation, Full Verification, and Package Assembly

**Files:**
- Modify: `docs/deployment/unified-backend-deployment.md`
- Modify: `docs/migrations/unified-backend-migrations.md`
- Modify: `docs/architecture/2026-07-13-hive-system-logic-chain-map.md`
- Modify: `RELEASE_BUILD_INFO.txt`

- [ ] Update counts and workflows from generated evidence, removing stale 74-migration and old-hash statements.
- [ ] Run `mvn test`, `npm test`, and `npm run build`.
- [ ] Verify all migration checksums, baseline shadow import, JAR/UI hashes, mini-program size, and clean Git status.
- [ ] Assemble a clean upload staging directory separately from the server-owned runtime mirror.
