# Installation Multi-Person Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the installation task's retired single-person fields with a tenant-isolated, ordered list of up to 20 installers across database, backend API, frontend, tests, documentation, and deployment artifacts.

**Architecture:** `InstallationTaskService` remains the only domain service and owns validation, transactional replacement, and two-query page assembly. A new MyBatis-Plus entity/mapper persists installer rows; pure JavaScript helpers own frontend list manipulation and validation so Node tests can exercise the UI contract without mounting Vue.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, MySQL 8 migrations, Vue 3, Element Plus, Node test runner, Maven, Vite.

## Global Constraints

- Work only in the independent clone and branch `codex/installation-multi-person`; never use git worktree.
- Preserve the latest `origin/dev` history and never overwrite or revert unrelated work.
- All public endpoints remain under the configured `/api/**` context; add no `/web/**` or compatibility endpoint.
- Never edit an executed historical migration; only add `V20260715_002_installation_task_installer.sql`, preserving the independently delivered `_001` order migration.
- Remove `constructionPersonnel`, `constructionPhone`, `construction_personnel`, and `construction_phone` from active business contracts; historical SQL may still contain them.
- Installer names are trimmed, required when a row exists, and limited to 50 characters.
- Installer phones are trimmed, required when a row exists, and limited to 40 characters; no mobile-only format validation.
- A task has at most 20 installers and cannot contain the same trimmed `(name, phone)` pair twice.
- `completed_accepted` requires at least one installer; every other status allows an empty installer list.
- Every installer read, delete, and write includes `tenant_code`.
- Update and full installer replacement occur in one rollback-for-Exception transaction.
- Page reads batch-load installers once per non-empty page and never perform per-task queries.

---

### Task 1: Lock the backend contract with failing tests

**Files:**
- Create: `management/src/test/java/my/hive/domain/installation/InstallationTaskInstallerContractTest.java`
- Create: `management/src/main/java/my/hive/domain/installation/model/dto/InstallationTaskInstallerRequest.java`
- Create: `management/src/main/java/my/hive/domain/installation/model/vo/InstallationTaskInstallerVO.java`
- Create: `management/src/main/java/my/hive/domain/installation/model/entity/InstallationTaskInstaller.java`
- Create: `management/src/main/java/my/hive/domain/installation/mapper/InstallationTaskInstallerMapper.java`
- Modify: `management/src/main/java/my/hive/domain/installation/model/dto/InstallationTaskStatusUpdateRequest.java`
- Modify: `management/src/main/java/my/hive/domain/installation/model/vo/InstallationTaskVO.java`
- Modify: `management/src/main/java/my/hive/domain/installation/model/entity/InstallationTask.java`

**Interfaces:**
- Produces: `List<InstallationTaskInstallerRequest> InstallationTaskStatusUpdateRequest.getInstallers()`.
- Produces: `List<InstallationTaskInstallerVO> InstallationTaskVO.getInstallers()`.
- Produces: MyBatis entity fields `id`, `tenantCode`, `installationTaskId`, `installerName`, `installerPhone`, `sortOrder`, `createTime`, `updateTime`.

- [ ] **Step 1: Write the failing contract test**

Add reflection assertions that the request and VO expose `installers`, the installer request has `name/phone`, the installer VO has `id/name/phone/sortOrder`, the new entity maps `installation_task_installer`, and the old fields are absent from request, VO, and task entity.

```java
assertNotNull(InstallationTaskStatusUpdateRequest.class.getDeclaredField("installers"));
assertThrows(NoSuchFieldException.class,
        () -> InstallationTaskStatusUpdateRequest.class.getDeclaredField("constructionPersonnel"));
assertThrows(NoSuchFieldException.class,
        () -> InstallationTask.class.getDeclaredField("constructionPhone"));
```

- [ ] **Step 2: Run the contract test and verify RED**

Run: `mvn -pl management -Dtest=InstallationTaskInstallerContractTest test`

Expected: compilation or assertion failure because installer models and fields do not exist and old fields still exist.

- [ ] **Step 3: Implement the minimal models and mapper**

Use Lombok `@Data`, initialize request/VO lists to `new ArrayList<>()`, map the entity with `@TableName("installation_task_installer")`, and make the mapper extend `BaseMapper<InstallationTaskInstaller>`. Delete only the two retired fields from `InstallationTask`.

```java
@Data
public class InstallationTaskInstallerRequest {
    private String name;
    private String phone;
}
```

- [ ] **Step 4: Re-run and verify GREEN**

Run: `mvn -pl management -Dtest=InstallationTaskInstallerContractTest test`

Expected: PASS.

---

### Task 2: Implement service validation, transactional replacement, tenant isolation, and batch reads

**Files:**
- Create: `management/src/test/java/my/hive/domain/installation/InstallationTaskInstallerServiceTest.java`
- Modify: `management/src/test/java/my/hive/domain/installation/UnifiedInstallationServiceTest.java`
- Modify: `management/src/main/java/my/hive/domain/installation/service/InstallationTaskService.java`

**Interfaces:**
- Consumes: the Task 1 request, VO, entity, and mapper models.
- Produces: `updateStatus(request)` with validated full replacement and `page(request)` with batch-loaded installers.

- [ ] **Step 1: Write failing validation and persistence tests**

With Mockito and `TenantPermissionContext.init("TENANT_A", 1L, Set.of())`, inject mocked task and installer mappers. Cover zero, one, and multiple installers; missing name; missing phone; name length 51; phone length 41; duplicate trimmed pair; 21 rows; completed-with-empty; non-completed-with-empty. Capture inserted rows and assert trimmed values, tenant code, task ID, sequential sort order, and VO echo.

```java
assertThrows(BusinessException.class, () -> service.updateStatus(request(
        "completed_accepted", List.of(installer("", "010-1")))));
verify(installerMapper, never()).delete(any());
```

- [ ] **Step 2: Run the validation tests and verify RED**

Run: `mvn -pl management -Dtest=InstallationTaskInstallerServiceTest test`

Expected: compilation or behavior failures because service does not validate or persist installer rows.

- [ ] **Step 3: Add minimal normalization and replacement implementation**

Add `MAX_INSTALLERS = 20`, validate before mutating the task, then update the task, delete installer rows with a wrapper containing both tenant and task ID, insert normalized rows, and return a VO with the inserted IDs/values. Treat `null` installers as empty.

```java
private List<InstallationTaskInstallerRequest> normalizeInstallers(List<InstallationTaskInstallerRequest> source,
                                                                    InstallationTaskStatusEnum status) {
    List<InstallationTaskInstallerRequest> installers = source == null ? List.of() : source;
    if (installers.size() > MAX_INSTALLERS) throw new BusinessException("安装人员最多 20 名");
    // trim, required, lengths, and duplicate pair checks
    if (InstallationTaskStatusEnum.COMPLETED_ACCEPTED.matches(status.getCode()) && result.isEmpty()) {
        throw new BusinessException("已完成已验收状态至少需要一名安装人员");
    }
    return result;
}
```

- [ ] **Step 4: Verify validation and save tests GREEN**

Run: `mvn -pl management -Dtest=InstallationTaskInstallerServiceTest test`

Expected: all validation/save tests PASS.

- [ ] **Step 5: Add failing tenant, rollback-boundary, and batch-page tests**

Assert the task lookup wrapper, installer delete wrapper, and installer page query wrapper contain `tenant_code`; a cross-tenant lookup returns not-found and never touches detail rows. Verify `updateStatus` still has `@Transactional(rollbackFor = Exception.class)`. For a page with two tasks, return three detail rows from one `selectList` call and assert ordered grouping; for an empty page verify zero detail queries.

```java
verify(installerMapper, times(1)).selectList(any());
assertEquals(List.of("A", "B"), page.getRecords().get(0).getInstallers().stream()
        .map(InstallationTaskInstallerVO::getName).toList());
```

- [ ] **Step 6: Run batch/tenant tests and verify RED**

Run: `mvn -pl management -Dtest=InstallationTaskInstallerServiceTest,UnifiedInstallationServiceTest test`

Expected: batch assertions fail until page assembly and tenant wrappers are implemented.

- [ ] **Step 7: Implement the one-query detail loader and shared VO conversion**

Collect task IDs from the page, call installer mapper once using tenant plus `in(installationTaskId, ids)` and ordered task/sort fields, group by task ID, and pass the group to `toVO(task, installers)`. Empty pages skip the detail mapper.

- [ ] **Step 8: Run focused backend tests and verify GREEN**

Run: `mvn -pl management -Dtest=InstallationTaskInstallerContractTest,InstallationTaskInstallerServiceTest,UnifiedInstallationServiceTest test`

Expected: PASS with no Mockito interaction errors.

---

### Task 3: Add the forward-only migration and immutable-history gate

**Files:**
- Create: `db-migrations/migrations/V20260715_002_installation_task_installer.sql`
- Modify: `db-migrations/migration_manifest.txt`
- Modify: `db-migrations/migration_checksums.sha256`
- Create: `management-ui/tests/installation-task-installer-migration.test.js`
- Modify: `management-ui/tests/unified-backend-migration.test.js`

**Interfaces:**
- Produces: installer table, query index, sort-order uniqueness, and removal of two retired task columns.

- [ ] **Step 1: Write the failing migration contract test**

Assert the new filename is present, contains all required columns and indexes, drops both old columns, is the final manifest entry, and that pinned SHA-256 values for `V20260705_004`, `V20260707_001`, and `V20260710_001` remain unchanged.

- [ ] **Step 2: Run and verify RED**

Run: `npm --prefix management-ui test -- tests/installation-task-installer-migration.test.js`

Expected: FAIL because the new migration is absent.

- [ ] **Step 3: Add the idempotent MySQL migration**

Create the table with `utf8mb4`, `idx_installation_task_installer_task (tenant_code, installation_task_id)`, and `uk_installation_task_installer_sort (tenant_code, installation_task_id, sort_order)`. Use `information_schema.columns` plus prepared statements to conditionally drop the two old columns so partially converged environments are safe. The `_002` version avoids the parallel `_001` deployment migration.

- [ ] **Step 4: Append manifest and calculate only the new checksum**

Run: `Get-FileHash -Algorithm SHA256 db-migrations/migrations/V20260715_002_installation_task_installer.sql`

Append `migrations/V20260715_002_installation_task_installer.sql` and its lowercase hash; do not recalculate or rewrite historical lines.

- [ ] **Step 5: Run migration tests and verify GREEN**

Run: `npm --prefix management-ui test -- tests/installation-task-installer-migration.test.js tests/unified-backend-migration.test.js`

Expected: PASS and all protected historical hashes unchanged.

---

### Task 4: Replace the frontend single-person editor with a tested dynamic installer list

**Files:**
- Create: `management-ui/src/views/function/installationTask/installationTaskInstallers.js`
- Create: `management-ui/tests/installation-task-installers.test.js`
- Modify: `management-ui/src/views/function/installationTask/installationTask.vue`
- Modify: relevant static UI contract test if its exact single-field expectations change.

**Interfaces:**
- Produces: `createInstaller`, `addInstaller`, `removeInstaller`, `cloneInstallers`, `validateInstallers`, `installerPreview`, and `buildInstallerPayload` pure functions.
- Consumes: backend `row.installers` and emits only `installers: [{name, phone}]` in the save request.

- [ ] **Step 1: Write failing helper tests**

Cover add, remove, 20-row limit, deep-cloned refill, trimmed payload, name/phone required, both length limits, duplicate pair, completed-with-empty, unfinished-empty, and preview of first three plus remaining count.

- [ ] **Step 2: Run and verify RED**

Run: `npm --prefix management-ui test -- tests/installation-task-installers.test.js`

Expected: FAIL because the helper module does not exist.

- [ ] **Step 3: Implement minimal pure helpers**

Return validation objects shaped `{ valid: boolean, message: string }`; preserve row order; never include IDs or sort order in the request payload.

- [ ] **Step 4: Run helper tests and verify GREEN**

Run: `npm --prefix management-ui test -- tests/installation-task-installers.test.js`

Expected: PASS.

- [ ] **Step 5: Add failing component contract assertions**

Read the Vue source and assert it binds `editorForm.installers`, exposes add/remove controls, renders preview/all-view behavior, sends `installers`, and no longer references `constructionPersonnel` or `constructionPhone`.

- [ ] **Step 6: Run component contract and verify RED**

Run: `npm --prefix management-ui test -- tests/installation-task-installers.test.js`

Expected: source contract assertions FAIL against the old component.

- [ ] **Step 7: Update `installationTask.vue`**

Replace summary, table cell, form fields, refill, validation, and payload code. Add an Element Plus popover/dialog for all installers, row-level delete buttons, and an add button disabled at 20. Keep all existing logistics, attachment, permission, and request-state behavior intact.

- [ ] **Step 8: Run installation UI tests and build**

Run: `npm --prefix management-ui test -- tests/installation-task-installers.test.js tests/installation-task-shipped-logistics.test.js tests/installation-task-special-note.test.js tests/element-plus-installation-quality.test.js`

Run: `npm --prefix management-ui run build`

Expected: tests PASS and Vite build exits 0.

---

### Task 5: Synchronize API, module, migration, architecture, and deployment documentation

**Files:**
- Modify: `docs/management-ui/modules/installation-task.md`
- Modify: `docs/api/unified-api-catalog.md`
- Modify: `docs/migrations/unified-backend-migrations.md`
- Modify: `docs/architecture/unified-backend.md`
- Modify: `docs/deployment/unified-backend-deployment.md`
- Modify: deployment copies under `C:/Users/HUAWEI/Desktop/hive部署_全新配置`

**Interfaces:**
- Produces: accurate request/response examples, validation rules, two-query behavior, migration version, and deployment instructions.

- [ ] **Step 1: Add a failing retired-contract static test**

Extend the frontend contract test to scan active Java, Vue, and target documentation files and reject the two retired camelCase names; explicitly exclude historical migrations and design/plan history.

- [ ] **Step 2: Run and verify RED**

Run: `npm --prefix management-ui test -- tests/installation-task-installers.test.js`

Expected: FAIL while active source/docs still mention retired fields.

- [ ] **Step 3: Update documentation**

Document `/api/installation-tasks/page`, `/api/installation-tasks/status`, `installers[]`, validation, tenant isolation, transaction rollback, batch loading, `V20260715_001`, and historical migration immutability.

- [ ] **Step 4: Synchronize deployment artifacts**

Copy the new migration, append the deployment manifest/checksum without changing historical entries, copy updated operational docs, replace the single management JAR after build, and update `RELEASE_BUILD_INFO.txt` using the repository's existing format.

- [ ] **Step 5: Verify retired-contract and deployment tests GREEN**

Run: `npm --prefix management-ui test -- tests/installation-task-installers.test.js tests/deploy-migration-immutability.test.js tests/unified-backend-migration.test.js`

Expected: PASS.

---

### Task 6: Full verification, final commit, and push

**Files:**
- Modify only files required by failures attributable to this feature.

**Interfaces:**
- Produces: pushed branch and final commit ID for the main merge window.

- [ ] **Step 1: Run complete backend tests**

Run: `mvn -pl management test`

Expected: BUILD SUCCESS.

- [ ] **Step 2: Build the unique backend JAR**

Run: `mvn -pl management clean package -DskipTests`

Expected: BUILD SUCCESS and exactly one deployable `management-*.jar` excluding original/sources/javadoc jars.

- [ ] **Step 3: Run the full frontend suite and build**

Run: `npm --prefix management-ui test`

Run: `npm --prefix management-ui run build`

Expected: all tests PASS and build exits 0.

- [ ] **Step 4: Run migration and deployment integrity checks**

Run the repository migration tests and available deployment health/checksum scripts. Confirm the deployment manifest/checksum matches the repository and the new migration is present.

- [ ] **Step 5: Review the final diff**

Run: `git diff --check origin/dev...HEAD` and `git status --short`.

Expected: no whitespace errors, no unrelated changes, and no active old contract names.

- [ ] **Step 6: Commit implementation**

Run: `git add <reviewed files>` then `git commit -m "feat: support multiple installation task installers"`.

- [ ] **Step 7: Push the branch**

Run: `git push -u origin codex/installation-multi-person`.

Expected: push succeeds without force.

- [ ] **Step 8: Report handoff**

Report changed files, exact commands/results, final commit ID, migration version, deployment synchronization, and merge steps:

```powershell
git fetch origin
git switch dev
git pull --ff-only origin dev
git merge --no-ff origin/codex/installation-multi-person
git push origin dev
```
