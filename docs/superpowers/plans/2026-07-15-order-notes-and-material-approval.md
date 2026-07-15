# Order Notes and Material Approval Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the single order remark field with auditable multi-row notes on web and mini-program clients, and ensure material approval starts only when a pending-payment order advances to material preparation.

**Architecture:** Add a tenant-scoped `sales_order_note` aggregate with optimistic locking and integrate it transactionally into the canonical order create/update/detail contract. Keep business notes separate from transition logs, add exact Permission Catalog V3 leaves, and implement the same contract in both Java backends and both clients. Remove status-derived approval candidate creation; the advance command becomes the only material-approval trigger.

**Tech Stack:** Java 21, Spring Boot 3.1, MyBatis-Plus, MySQL 8, Vue 3/Vite, WeChat Mini Program JavaScript/WXML/WXSS, Node test runner, Maven/JUnit 5.

## Global Constraints

- Do not create a git worktree; use the current `codex/unify-hive-backend` branch and preserve unrelated dirty changes.
- Do not modify historical `db-migrations/migrations/V*.sql`; add `V20260715_001_order_notes_and_material_approval.sql` and append it to `migration_manifest.txt`.
- Do not migrate or preserve `sales_order.remark`; production launch will clear old business data.
- Saved notes can be created and edited but never deleted. Unsaved new rows may be discarded before the order is saved.
- Each note is nonblank, at most 1000 characters, and each order has at most 50 notes.
- Web, mini-program, management backend, and mini backend must use the exact permission codes `order:note:view`, `order:note:create`, `order:note:update`, and `order:audit:material`.
- Material approval is created only by `pending_pay -> pending_material`; creating or editing an order already in `pending_pay` must not create approval candidates.
- Status-log remarks and approval comments remain operation records and must not be stored in `sales_order_note`.
- Rebuild both backend JARs, management UI dist, mini-program release source, deployment metadata, and deployment package before completion.

---

### Task 1: Database Schema and Permission Catalog

**Files:**
- Create: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/db-migrations/migrations/V20260715_001_order_notes_and_material_approval.sql`
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/db-migrations/migration_manifest.txt`
- Modify: `management/src/main/java/my/hive/shared/permission/PermissionCatalogV3.java`
- Modify: `management/src/main/java/my/hive/domain/permission/service/BuiltInRoleCatalog.java`
- Modify: `management/src/main/java/my/hive/domain/dashboard/service/DashboardService.java`
- Modify: `management-ui/tests/permission-v3-catalog.test.js`
- Modify: `management-ui/tests/deploy-migration-immutability.test.js`

**Interfaces:**
- Produces table `sales_order_note` and exact permission leaves consumed by both backends and clients.
- Produces `PermissionCatalogV3.CODE_ORDER_NOTE_VIEW`, `CODE_ORDER_NOTE_CREATE`, `CODE_ORDER_NOTE_UPDATE`, and `CODE_ORDER_AUDIT_MATERIAL`.

- [ ] **Step 1: Add failing catalog and migration assertions**

Add assertions equivalent to:

```js
for (const code of [
  'order:note:view',
  'order:note:create',
  'order:note:update',
  'order:audit:material'
]) {
  assert.ok(catalogSource.includes(code), `missing Permission V3 leaf: ${code}`)
  assert.ok(migrationSource.includes(code), `missing permission seed: ${code}`)
}
assert.match(migrationSource, /CREATE TABLE `sales_order_note`/)
assert.match(migrationSource, /DROP COLUMN `remark`/)
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
cd D:\HiveManager\management-ui
node --test tests/permission-v3-catalog.test.js tests/deploy-migration-immutability.test.js
```

Expected: FAIL because the migration and four permission leaves do not exist.

- [ ] **Step 3: Add the migration**

Create an idempotent migration with this effective schema:

```sql
CREATE TABLE `sales_order_note` (
  `id` bigint NOT NULL,
  `tenant_code` varchar(64) NOT NULL,
  `order_id` varchar(64) NOT NULL,
  `content` varchar(1000) NOT NULL,
  `creator_user_id` bigint NOT NULL,
  `creator_name` varchar(100) NOT NULL,
  `updater_user_id` bigint NOT NULL,
  `updater_name` varchar(100) NOT NULL,
  `version` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sales_order_note_order` (`tenant_code`, `order_id`, `update_time`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

The same migration must seed the four active `sys_permission` leaves, grant note permissions to built-in roles that already receive the corresponding order view/update capabilities, grant `order:audit:material` to approval roles, remove stale role relations for `approval:order:audit`, and drop `sales_order.remark` without copying data.

- [ ] **Step 4: Update Java catalogs**

Add constants and actions:

```java
public static final String CODE_ORDER_NOTE_VIEW = "order:note:view";
public static final String CODE_ORDER_NOTE_CREATE = "order:note:create";
public static final String CODE_ORDER_NOTE_UPDATE = "order:note:update";
public static final String CODE_ORDER_AUDIT_MATERIAL = "order:audit:material";
```

Update built-in role and dashboard approval permission sets to use `CODE_ORDER_AUDIT_MATERIAL` for material approval without removing shipment approval from shipment flows.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run:

```powershell
cd D:\HiveManager\management-ui
node --test tests/permission-v3-catalog.test.js tests/deploy-migration-immutability.test.js
```

Expected: PASS and all historical migration hashes remain accepted.

- [ ] **Step 6: Commit the schema and catalog slice**

```powershell
git add management/src/main/java/my/hive/shared/permission/PermissionCatalogV3.java management/src/main/java/my/hive/domain/permission/service/BuiltInRoleCatalog.java management/src/main/java/my/hive/domain/dashboard/service/DashboardService.java management-ui/tests/permission-v3-catalog.test.js management-ui/tests/deploy-migration-immutability.test.js
git commit -m "feat: add order note and material approval permissions"
```

Commit the deployment migration in the repository location that owns release migrations if mirrored there; never stage unrelated files.

### Task 2: Management Backend Order Note Aggregate

**Files:**
- Create: `management/src/main/java/my/hive/domain/order/model/entity/SalesOrderNote.java`
- Create: `management/src/main/java/my/hive/domain/order/model/dto/SalesOrderNoteSaveRequest.java`
- Create: `management/src/main/java/my/hive/domain/order/model/vo/SalesOrderNoteVO.java`
- Create: `management/src/main/java/my/hive/domain/order/mapper/SalesOrderNoteMapper.java`
- Create: `management/src/main/java/my/hive/domain/order/service/OrderNoteService.java`
- Create: `management/src/test/java/my/management/module/order/service/OrderNoteServiceTest.java`
- Modify: `management/src/main/java/my/hive/domain/order/model/dto/SalesOrderSaveRequest.java`
- Modify: `management/src/main/java/my/hive/domain/order/model/vo/SalesOrderDetailVO.java`
- Modify: `management/src/main/java/my/hive/domain/order/model/vo/SalesOrderPageVO.java`
- Modify: `management/src/main/java/my/hive/domain/order/model/entity/SalesOrder.java`
- Modify: `management/src/main/java/my/hive/domain/order/mapper/SalesOrderMapper.java`
- Modify: `management/src/main/java/my/hive/domain/order/service/OrderService.java`
- Modify: `management/src/main/java/my/hive/domain/installation/service/InstallationTaskService.java`
- Modify: `management/src/main/java/my/hive/domain/order/model/enums/OrderLogOperateTypeEnum.java`

**Interfaces:**
- Consumes the `sales_order_note` table and Permission V3 constants from Task 1.
- Produces `List<SalesOrderNoteVO> notes` on order detail and `List<SalesOrderNoteSaveRequest> notes` on create/update.
- Produces `OrderNoteService.saveNotes(String tenantCode, String orderId, List<SalesOrderNoteSaveRequest> requests)`.

- [ ] **Step 1: Write failing domain tests**

Cover these cases with mapper mocks or an H2 integration fixture:

```java
@Test void creates_multiple_notes_with_current_operator() {}
@Test void updates_only_changed_note_and_increments_version() {}
@Test void omitted_saved_note_is_not_deleted() {}
@Test void rejects_note_from_another_order_or_tenant() {}
@Test void rejects_blank_overlong_and_more_than_fifty_notes() {}
@Test void reports_conflict_when_version_changed() {}
```

- [ ] **Step 2: Run tests and verify RED**

```powershell
cd D:\HiveManager\management
mvn -Dtest=OrderNoteServiceTest test
```

Expected: FAIL because note types and service do not exist.

- [ ] **Step 3: Implement the focused note service**

Use this public boundary:

```java
@Transactional
public List<SalesOrderNoteVO> saveNotes(
        String tenantCode,
        String orderId,
        List<SalesOrderNoteSaveRequest> requests) {
    // validate count/content, insert new rows, update changed rows with id/order/tenant/version predicate
}

public List<SalesOrderNoteVO> listNotes(String tenantCode, String orderId) {
    // update_time DESC, id DESC
}
```

Read operator ID and name from trusted tenant/user context. Throw the existing business exception type with HTTP 409 semantics when an optimistic update affects zero rows.

- [ ] **Step 4: Integrate create, update, detail, and logs**

Replace `remark` in order save/detail contracts with:

```java
@Valid
@Size(max = 50, message = "每个订单最多添加50条备注")
private List<SalesOrderNoteSaveRequest> notes;
```

Call `saveNotes` inside the existing order transaction after the order ID exists. Add `NOTE_CREATE` and `NOTE_UPDATE` operation log types without copying note content into the log. Remove installation-task copying from `order.getRemark()`; installation remarks remain independent.

- [ ] **Step 5: Remove the legacy business field**

Remove `remark` from `SalesOrder`, `SalesOrderMapper`, `SalesOrderSaveRequest`, `SalesOrderDetailVO`, and `SalesOrderPageVO`. Keep transition request comments and `sales_order_status_log.remark` intact.

- [ ] **Step 6: Run management backend tests**

```powershell
cd D:\HiveManager\management
mvn -Dtest=OrderNoteServiceTest,PendingShipApprovalTest,UnifiedApprovalServiceTest test
```

Expected: PASS with no SQL mapping reference to `sales_order.remark`.

- [ ] **Step 7: Commit management backend notes**

```powershell
git add management/src/main/java/my/hive/domain/order management/src/main/java/my/hive/domain/installation/service/InstallationTaskService.java management/src/test/java/my/management/module/order/service/OrderNoteServiceTest.java
git commit -m "feat: add auditable order notes"
```

### Task 3: Management Backend Material Approval Trigger

**Files:**
- Modify: `management/src/main/java/my/hive/domain/order/service/OrderService.java`
- Modify: `management/src/main/java/my/hive/domain/approval/service/ApprovalService.java`
- Modify: `management/src/main/java/my/hive/domain/approval/service/ApprovalDefaultAuditorService.java`
- Create: `management/src/test/java/my/management/module/order/service/PendingPayMaterialApprovalTest.java`
- Modify: `management/src/test/java/my/hive/domain/approval/UnifiedApprovalServiceTest.java`

**Interfaces:**
- Consumes `PermissionCatalogV3.CODE_ORDER_AUDIT_MATERIAL`.
- Produces approval candidate `type=ORDER`, `businessCode=sales:<orderId>` only from advance requests.

- [ ] **Step 1: Write failing approval-trigger tests**

```java
@Test void creating_pending_pay_order_does_not_create_candidate() {}
@Test void editing_pending_pay_order_does_not_create_candidate() {}
@Test void advancing_pending_pay_to_material_creates_candidate_once() {}
@Test void approval_query_has_no_candidate_creation_side_effect() {}
@Test void material_auditor_requires_order_audit_material() {}
```

- [ ] **Step 2: Run tests and verify RED**

```powershell
cd D:\HiveManager\management
mvn -Dtest=PendingPayMaterialApprovalTest,UnifiedApprovalServiceTest test
```

Expected: FAIL because create/save currently derives candidates from `pending_pay` status.

- [ ] **Step 3: Remove status-derived candidate creation**

Delete create/update calls to `syncSalesOrderApprovalAuditorsIfNeeded`. Invoke candidate creation only inside the guarded branch:

```java
if (STATUS_PENDING_PAY.equals(oldStatus) && STATUS_PENDING_MATERIAL.equals(targetStatus)) {
    submitSalesMaterialApproval(order, request);
    insertSalesLog(order, oldStatus, targetStatus,
        OrderLogOperateTypeEnum.APPROVAL_PENDING.getCode(), "提交备料审批", LocalDateTime.now());
    return;
}
```

Do not change order status before approval succeeds. Reject duplicate active applications without adding duplicate candidates.

- [ ] **Step 4: Make approval reads side-effect free**

Remove candidate auto-creation from approval list/detail GET paths. Candidate existence or an explicit pending operation record is required for an order to appear in the approval center.

- [ ] **Step 5: Use exact material permission**

Replace material-flow uses of `order:audit:shipment` with `PermissionCatalogV3.CODE_ORDER_AUDIT_MATERIAL`. Keep shipment flow on `order:audit:shipment`.

- [ ] **Step 6: Run focused and module tests**

```powershell
cd D:\HiveManager\management
mvn -Dtest=PendingPayMaterialApprovalTest,UnifiedApprovalServiceTest,PendingShipApprovalTest test
```

Expected: PASS.

- [ ] **Step 7: Commit approval trigger fix**

```powershell
git add management/src/main/java/my/hive/domain/order/service/OrderService.java management/src/main/java/my/hive/domain/approval/service management/src/test/java/my/management/module/order/service/PendingPayMaterialApprovalTest.java management/src/test/java/my/hive/domain/approval/UnifiedApprovalServiceTest.java
git commit -m "fix: start material approval only on advance"
```

### Task 4: Mini Backend Contract Parity

**Files:**
- Create: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/model/entity/SalesOrderNote.java`
- Create: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/model/dto/SalesOrderNoteSaveRequest.java`
- Create: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/model/vo/SalesOrderNoteVO.java`
- Create: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/mapper/SalesOrderNoteMapper.java`
- Create: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/service/OrderNoteService.java`
- Create: `D:/HiveBackend/server/src/test/java/my/hive_back/order/OrderNoteServiceTest.java`
- Create: `D:/HiveBackend/server/src/test/java/my/hive_back/order/PendingPayMaterialApprovalTest.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/model/dto/SalesOrderAddRequest.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/model/dto/UnifiedOrderUpdateRequest.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/model/vo/SalesOrderVO.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/model/entity/SalesOrder.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/service/SalesOrderService.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/approval/service/ApprovalCenterService.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/approval/service/ApprovalDefaultAuditorService.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/sys/model/enums/PermissionCodeEnum.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/installation/service/InstallationTaskSyncService.java`

**Interfaces:**
- Must match Task 2 request/response property names and Task 3 approval behavior exactly.

- [ ] **Step 1: Write mini-backend parity tests**

Mirror management backend cases for note validation, optimistic locking, no deletion, create/edit no approval, and advance-only material approval. Add a static assertion that mini backend no longer references `approval:order:audit`.

- [ ] **Step 2: Run tests and verify RED**

```powershell
cd D:\HiveBackend\server
mvn -Dtest=OrderNoteServiceTest,PendingPayMaterialApprovalTest,ApprovalCenterServiceTest test
```

Expected: FAIL before the new contract exists.

- [ ] **Step 3: Implement the same aggregate and contract**

Use the same `notes: [{id, content, version}]` input and `SalesOrderNoteVO` output fields as management backend. Use the same table, validation limits, ordering, tenant checks, version predicate, and operation log types.

- [ ] **Step 4: Align approval permissions and triggers**

Add:

```java
ORDER_NOTE_VIEW("order:note:view"),
ORDER_NOTE_CREATE("order:note:create"),
ORDER_NOTE_UPDATE("order:note:update"),
ORDER_AUDIT_MATERIAL("order:audit:material");
```

Remove all material-flow references to `approval:order:audit`; create candidates only on `pending_pay -> pending_material` advance.

- [ ] **Step 5: Remove legacy order remark dependencies**

Remove the business `remark` field from sales order create/update/VO/entity mapping and stop copying it into installation tasks. Keep transition log remark fields.

- [ ] **Step 6: Run mini backend tests**

```powershell
cd D:\HiveBackend\server
mvn -Dtest=OrderNoteServiceTest,PendingPayMaterialApprovalTest,SalesOrderFlowAdvanceTest,ApprovalCenterServiceTest,InstallationTaskSyncServiceTest test
```

Expected: PASS.

- [ ] **Step 7: Commit mini backend parity**

Commit only files under `D:\HiveBackend\server` with message:

```text
feat: align mini order notes and material approval
```

### Task 5: Management Web Note Editor

**Files:**
- Modify: `management-ui/src/views/function/order/order.vue`
- Modify: `management-ui/src/views/function/order/api/order.js`
- Modify: `management-ui/src/views/function/order/orderPermissions.js`
- Create: `management-ui/tests/order-notes-ui.test.js`
- Modify: `management-ui/tests/order-permission-hardening.test.js`
- Modify: `management-ui/tests/order-unified.test.js`

**Interfaces:**
- Consumes `notes` from Tasks 2 and 3 and exact note permissions from Task 1.
- Produces canonical `/orders` and `/orders/{id}/advance` requests.

- [ ] **Step 1: Write failing UI contract tests**

Assert that the order view:

```js
assert.ok(source.includes('orderForm.notes'))
assert.ok(source.includes('新增备注'))
assert.ok(source.includes('最后修改'))
assert.ok(source.includes('order:note:create'))
assert.ok(source.includes('order:note:update'))
assert.ok(!source.includes('v-model.trim="orderForm.remark"'))
assert.ok(!apiSource.includes('/order/create'))
assert.ok(!apiSource.includes('/order/next/'))
```

- [ ] **Step 2: Run tests and verify RED**

```powershell
cd D:\HiveManager\management-ui
node --test tests/order-notes-ui.test.js tests/order-permission-hardening.test.js tests/order-unified.test.js
```

Expected: FAIL on the old textarea and legacy routes.

- [ ] **Step 3: Replace the textarea with note rows**

Add helpers with these responsibilities:

```js
function defaultOrderNote() {
  return { id: null, content: '', version: null, updaterName: '', updateTime: '', isNew: true }
}
function addOrderNote() { orderForm.notes.push(defaultOrderNote()) }
function discardUnsavedOrderNote(index) {
  if (!orderForm.notes[index]?.id) orderForm.notes.splice(index, 1)
}
```

Saved rows expose edit controls only when `order:note:update` is granted; new row controls require `order:note:create`. Saved rows have no delete action. Show updater and formatted update time; new rows show “保存后记录修改时间”.

- [ ] **Step 4: Align payload and canonical routes**

Send only:

```js
notes: orderForm.notes.map(({ id, content, version }) => ({ id, content: content.trim(), version }))
```

Use `POST /orders`, `PUT /orders/{id}`, `GET /orders/{id}`, and `POST /orders/{id}/advance` without legacy fallback.

- [ ] **Step 5: Verify UI tests and build**

```powershell
cd D:\HiveManager\management-ui
node --test tests/order-notes-ui.test.js tests/order-permission-hardening.test.js tests/order-unified.test.js
npm run build
```

Expected: tests PASS and Vite build exits 0.

- [ ] **Step 6: Commit web UI**

```powershell
git add management-ui/src/views/function/order management-ui/tests/order-notes-ui.test.js management-ui/tests/order-permission-hardening.test.js management-ui/tests/order-unified.test.js
git commit -m "feat: add multi-row order notes to management UI"
```

### Task 6: Mini-Program Note Editor

**Files:**
- Modify: `D:/productHiveFrontend/client/pages/salesOrder/salesOrder.js`
- Modify: `D:/productHiveFrontend/client/pages/salesOrder/salesOrder.wxml`
- Modify: `D:/productHiveFrontend/client/pages/salesOrder/salesOrder.wxss`
- Modify: `D:/productHiveFrontend/client/utils/api.js`
- Modify: `management-ui/tests/deploy-mini-order-runtime.test.js`

**Interfaces:**
- Consumes the same `notes` contract and permissions as Task 5.

- [ ] **Step 1: Add failing mini static-contract tests**

Assert WXML/JS contains note add/edit/update-time behavior, has no single `remark` form binding, uses canonical order APIs, and gates note content/actions with exact permissions.

- [ ] **Step 2: Run test and verify RED**

```powershell
cd D:\HiveManager\management-ui
node --test tests/deploy-mini-order-runtime.test.js
```

Expected: FAIL because the mini order form still uses the single remark field.

- [ ] **Step 3: Implement mini note rows**

Maintain `orderForm.notes` and event handlers:

```js
onAddOrderNote() {}
onOrderNoteInput(event) {}
onDiscardUnsavedOrderNote(event) {}
```

Use `data-index`, allow discarding only rows without an ID, show `updaterName` and formatted `updateTime`, and hide note content when `order:note:view` is absent.

- [ ] **Step 4: Align request mapping**

Send the exact `{id, content, version}` list and remove the old business `remark` request/display mapping. Keep operation-transition remarks used by scan, rollback, and approval actions.

- [ ] **Step 5: Validate mini source**

```powershell
cd D:\productHiveFrontend\client
node --check pages/salesOrder/salesOrder.js
node --check utils/api.js
cd D:\HiveManager\management-ui
node --test tests/deploy-mini-order-runtime.test.js
```

Expected: syntax and contract tests PASS.

- [ ] **Step 6: Commit mini frontend**

Commit only files under `D:\productHiveFrontend\client` with message:

```text
feat: add multi-row order notes to mini program
```

### Task 7: Cross-System Contract and Documentation Gates

**Files:**
- Create: `management-ui/tests/order-notes-cross-system.test.js`
- Modify: `management-ui/tests/permission-v3-exact.test.js`
- Modify: `management-ui/tests/deploy-commercial-cutover-hardening.test.js`
- Modify: `docs/order-workflow-and-operation-log.md` or the existing canonical order workflow document found during implementation
- Modify: `docs/default-role-permission-matrix.md`
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/RELEASE_BUILD_INFO.txt`
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/DEPLOYMENT_PACKAGE_20260714.md`

**Interfaces:**
- Proves both backends and both clients use the same note fields, routes, permission leaves, and approval trigger.

- [ ] **Step 1: Write cross-system static assertions**

The test must scan all four source trees and enforce:

```js
const exactCodes = ['order:note:view', 'order:note:create', 'order:note:update', 'order:audit:material']
// every runtime contains all relevant exact codes
// no material flow contains approval:order:audit
// no material flow uses order:audit:shipment
// no order business contract contains deliveryDate/delivery_date or a single remark field
// no server verifier requires mini-program source in the server package
```

- [ ] **Step 2: Run the new gate and verify failures expose remaining drift**

```powershell
cd D:\HiveManager\management-ui
node --test tests/order-notes-cross-system.test.js tests/permission-v3-exact.test.js tests/deploy-commercial-cutover-hardening.test.js
```

- [ ] **Step 3: Update canonical documentation**

Document note lifecycle, no-delete rule, optimistic conflict, operation log entries, exact permissions, and the advance-only material approval trigger. Update the default role matrix with the new leaves.

- [ ] **Step 4: Update release metadata**

Add markers:

```text
OrderMultiNoteContract=READY
OrderNotePermissionContract=READY
PendingPayMaterialApprovalTrigger=ADVANCE_ONLY
```

Keep `MiniProgramFrontendContract=READY` and `MiniProgramWechatDevtoolsVerification=UPLOADED` only after a new mini upload actually succeeds; update the mini version when uploaded.

- [ ] **Step 5: Run all management UI gates**

```powershell
cd D:\HiveManager\management-ui
node --test
```

Expected: all tests PASS with zero failures.

- [ ] **Step 6: Commit gates and docs**

```powershell
git add management-ui/tests docs
git commit -m "test: enforce order note and material approval contracts"
```

### Task 8: Build and Refresh the Deployment Package

**Files:**
- Replace: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/management-backend/management-0.0.1-SNAPSHOT.jar`
- Replace: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/backend/Hive_Back-0.0.1-SNAPSHOT.jar`
- Replace: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/management-ui/dist/`
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/RELEASE_BUILD_INFO.txt`

**Interfaces:**
- Produces the server deployment package and a separately verifiable mini-program source release.

- [ ] **Step 1: Run complete source tests**

```powershell
cd D:\HiveManager\management
mvn test
cd D:\HiveBackend\server
mvn test
cd D:\HiveManager\management-ui
node --test
```

Expected: every command exits 0.

- [ ] **Step 2: Build all production artifacts**

```powershell
cd D:\HiveManager\management
mvn clean package -DskipTests
cd D:\HiveBackend\server
mvn clean package -DskipTests
cd D:\HiveManager\management-ui
npm run build
```

Expected: both executable JARs and `management-ui/dist` are created.

- [ ] **Step 3: Mirror artifacts without touching runtime data**

Copy the two JARs and management UI `dist` into the exact deployment paths. Copy the new migration and updated manifest. Do not modify or delete `mysql/data`, `uploads`, logs, backups, snapshots, or certificates.

- [ ] **Step 4: Recreate and validate the mini release directory locally**

Build a clean mini release source from `D:\productHiveFrontend\client`, excluding `.git`, private project config, tests, docs, and temporary files. Run WeChat CLI preview before marking it uploaded; the server deployment directory must not contain `mini-program/`.

- [ ] **Step 5: Recalculate release hashes**

Update management JAR, mini JAR, management UI, migration manifest, and mini source hashes/file counts in `RELEASE_BUILD_INFO.txt`. Never retain stale `UPLOADED` metadata for a changed mini source unless the new version upload succeeds.

- [ ] **Step 6: Run release gates**

```powershell
cd D:\HiveManager\management-ui
node --test
```

On the server package, also run the Bash gates in a Linux environment:

```bash
bash scripts/check-deploy-health.sh
bash scripts/verify-release-integrity.sh
```

Expected: no missing artifact, migration drift, stale permission code, legacy single remark contract, or mini-source server dependency.

- [ ] **Step 7: Final commit and release report**

Commit only source, tests, migration ownership files, and documentation that belong in Git. Report the exact branch, commit IDs, artifact hashes, mini upload state, and any verification that could not run locally.
