# Order Multi-Shipment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the order-wide scalar logistics fields with an auditable list of non-deletable shipment records and query each tracking number independently on hover.

**Architecture:** Introduce a tenant-scoped `sales_order_shipment` child aggregate managed by a focused `OrderShipmentService`. Order create/save/status flows pass a complete shipment list to that service, while list/detail projections read persisted shipment VOs. The Kuaidi100 adapter resolves one persisted shipment by order and shipment ID, and the management UI renders one hover trigger per shipment.

**Tech Stack:** Java 21, Spring Boot 3.1, MyBatis-Plus, MySQL 8, JUnit 5, Mockito, Vue 3, Vite, Element Plus, Node test runner.

## Global Constraints

- Work on `codex/order-multi-shipment`; do not create a worktree.
- Do not modify historical versioned migrations. Append `V20260717_001_order_multi_shipment.sql` and regenerate the checksum catalog.
- Do not retain `sales_order.express_company`, `sales_order.express_no`, scalar request/response fields, endpoint fallback, JSON storage, or delimiter parsing.
- A shipment records only logistics company and tracking number; it does not record items or quantity.
- Saved shipments may be updated but never deleted. Unsaved browser rows may be discarded.
- Each tracking request is initiated only by opening that shipment's hover popover.
- Mini-program frontend changes are outside this plan.
- Installation tasks keep their own logistics fields and do not copy order shipment values.
- Use TDD for every production-code change and commit after each independently verified task.

---

### Task 1: Versioned Shipment Schema

**Files:**
- Create: `db-migrations/migrations/V20260717_001_order_multi_shipment.sql`
- Modify: `db-migrations/migration_manifest.txt`
- Modify: `db-migrations/migration_checksums.sha256`
- Modify: `db-migrations/baseline/hive_schema_baseline.sql`
- Create: `management-ui/tests/order-multi-shipment-schema.test.js`

**Interfaces:**
- Produces table `sales_order_shipment` with optimistic-lock and actor metadata columns.
- Removes active baseline columns `sales_order.express_company` and `sales_order.express_no`.
- Later tasks rely on unique key `uk_order_shipment_tracking (tenant_code, order_id, tracking_no)` and lookup index `idx_order_shipment_order (tenant_code, order_id, sort_order, id)`.

- [ ] **Step 1: Write the failing schema contract test**

Create assertions that read the baseline, new migration, manifest, and checksum catalog:

```js
test('order logistics use a normalized non-deletable shipment table', () => {
  assert.match(baseline, /CREATE TABLE `sales_order_shipment`/)
  for (const column of ['logistics_company', 'tracking_no', 'sort_order', 'version', 'updater_name']) {
    assert.match(baseline, new RegExp('`' + column + '`'))
  }
  const salesOrderBlock = tableBlock(baseline, 'sales_order')
  assert.doesNotMatch(salesOrderBlock, /`express_company`|`express_no`/)
  assert.match(migration, /CREATE TABLE `sales_order_shipment`/)
  assert.match(migration, /DROP COLUMN `express_company`/)
  assert.match(migration, /DROP COLUMN `express_no`/)
  assert.match(manifest, /migrations\/V20260717_001_order_multi_shipment\.sql\s*$/)
})
```

- [ ] **Step 2: Run the schema test and verify RED**

Run:

```powershell
cd D:\HiveManager\management-ui
node --test tests/order-multi-shipment-schema.test.js
```

Expected: FAIL because the migration and normalized table are absent.

- [ ] **Step 3: Add the migration and update the baseline**

The migration must create the table and remove the retired columns without editing prior migrations:

```sql
CREATE TABLE `sales_order_shipment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(50) NOT NULL,
  `order_id` varchar(50) NOT NULL,
  `logistics_company` varchar(100) NOT NULL,
  `tracking_no` varchar(100) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `updater_name` varchar(100) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_shipment_tracking` (`tenant_code`,`order_id`,`tracking_no`),
  KEY `idx_order_shipment_order` (`tenant_code`,`order_id`,`sort_order`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `sales_order`
  DROP COLUMN `express_company`,
  DROP COLUMN `express_no`;
```

Append the migration path to the manifest and regenerate `migration_checksums.sha256` using the repository's lowercase SHA-256 format.

- [ ] **Step 4: Run schema and migration-integrity tests**

Run:

```powershell
cd D:\HiveManager\management-ui
node --test tests/order-multi-shipment-schema.test.js tests/deploy-migration-immutability.test.js tests/repository-release-convergence.test.js
```

Expected: all tests PASS and no historical migration checksum changes.

- [ ] **Step 5: Commit the schema**

```powershell
git add db-migrations management-ui/tests/order-multi-shipment-schema.test.js
git commit -m "feat: add normalized order shipment schema"
```

### Task 2: Shipment Aggregate and No-Delete Save Service

**Files:**
- Create: `management/src/main/java/my/hive/domain/order/model/entity/SalesOrderShipment.java`
- Create: `management/src/main/java/my/hive/domain/order/model/dto/SalesOrderShipmentSaveRequest.java`
- Create: `management/src/main/java/my/hive/domain/order/model/vo/SalesOrderShipmentVO.java`
- Create: `management/src/main/java/my/hive/domain/order/mapper/SalesOrderShipmentMapper.java`
- Create: `management/src/main/java/my/hive/domain/order/service/OrderShipmentService.java`
- Create: `management/src/test/java/my/hive/domain/order/service/OrderShipmentServiceTest.java`

**Interfaces:**
- Produces `List<SalesOrderShipmentVO> saveShipments(String tenantCode, String orderId, List<SalesOrderShipmentSaveRequest> requests)`.
- Produces `List<SalesOrderShipmentVO> listShipments(String tenantCode, String orderId)` and batched `Map<String, List<SalesOrderShipmentVO>> listShipmentsByOrderIds(String tenantCode, Collection<String> orderIds)`.
- Produces `SalesOrderShipment requireShipment(String tenantCode, String orderId, Long shipmentId)` for tracking.

- [ ] **Step 1: Write failing service tests**

Cover create, update, omission, duplicate, ownership, count, and optimistic lock:

```java
@Test
void rejectsOmissionOfPersistedShipment() {
    when(mapper.selectList(any())).thenReturn(List.of(existingShipment(11L, 2)));
    BusinessException error = assertThrows(BusinessException.class,
            () -> service.saveShipments("TENANT_001", "SO-1", List.of()));
    assertEquals("已保存的物流记录不允许删除", error.getMessage());
    verify(mapper, never()).delete(any());
}

@Test
void updatesWithVersionAndRejectsConcurrentChange() {
    when(mapper.selectList(any())).thenReturn(List.of(existingShipment(11L, 2)));
    when(mapper.updateShipment(eq(11L), eq("TENANT_001"), eq("SO-1"), eq(2),
            eq("顺丰速运"), eq("SF-NEW"), eq(0), any(), any(), any())).thenReturn(0);
    SalesOrderShipmentSaveRequest request = request(11L, 2, "顺丰速运", "SF-NEW");
    BusinessException error = assertThrows(BusinessException.class,
            () -> service.saveShipments("TENANT_001", "SO-1", List.of(request)));
    assertEquals(409, error.getCode());
}
```

- [ ] **Step 2: Run the service test and verify RED**

Run:

```powershell
cd D:\HiveManager\management
.\mvnw.cmd -Dtest=OrderShipmentServiceTest test
```

Expected: test compilation fails because shipment types do not exist.

- [ ] **Step 3: Implement entity, DTO, VO, mapper, and service**

Use these DTO fields:

```java
public class SalesOrderShipmentSaveRequest {
    private Long id;
    private String logisticsCompany;
    private String trackingNo;
    private Integer version;
}
```

The mapper update must include tenant, order, ID, and version in its predicate:

```java
@Update("""
    UPDATE sales_order_shipment
       SET logistics_company = #{company}, tracking_no = #{trackingNo},
           sort_order = #{sortOrder}, updater = #{updater},
           updater_name = #{updaterName}, version = version + 1,
           update_time = #{updateTime}
     WHERE id = #{id} AND tenant_code = #{tenantCode}
       AND order_id = #{orderId} AND version = #{version}
    """)
int updateShipment(...);
```

`saveShipments` must trim values, reject blank rows and duplicate tracking numbers, cap the list at 50, verify the exact existing ID set is retained, insert new rows, update changed rows only, and never call delete.

- [ ] **Step 4: Run shipment service tests**

Run:

```powershell
cd D:\HiveManager\management
.\mvnw.cmd -Dtest=OrderShipmentServiceTest test
```

Expected: all shipment service tests PASS.

- [ ] **Step 5: Commit the aggregate**

```powershell
git add management/src/main/java/my/hive/domain/order management/src/test/java/my/hive/domain/order/service/OrderShipmentServiceTest.java
git commit -m "feat: add order shipment aggregate"
```

### Task 3: Integrate Shipments Into Order Lifecycle

**Files:**
- Modify: `management/src/main/java/my/hive/domain/order/model/entity/SalesOrder.java`
- Modify: `management/src/main/java/my/hive/domain/order/model/dto/SalesOrderSaveRequest.java`
- Modify: `management/src/main/java/my/hive/domain/order/model/dto/SalesOrderUpdateRequest.java`
- Modify: `management/src/main/java/my/hive/domain/order/model/vo/SalesOrderPageVO.java`
- Modify: `management/src/main/java/my/hive/domain/order/model/vo/SalesOrderDetailVO.java`
- Modify: `management/src/main/java/my/hive/domain/order/service/OrderService.java`
- Modify: `management/src/main/java/my/hive/domain/installation/service/InstallationTaskService.java`
- Create: `management/src/test/java/my/hive/domain/order/service/OrderMultiShipmentLifecycleTest.java`

**Interfaces:**
- `SalesOrderSaveRequest.shipments` is `List<SalesOrderShipmentSaveRequest>` with `@Valid` and maximum size 50.
- `SalesOrderPageVO.shipments` and `SalesOrderDetailVO.shipments` are `List<SalesOrderShipmentVO>`.
- `SalesOrder` no longer declares scalar logistics fields.

- [ ] **Step 1: Write failing lifecycle tests**

Tests must prove list/detail projections, create/save persistence, shipped-state validation, and installation independence:

```java
@Test
void shippedOrderRequiresAtLeastOneShipment() {
    SalesOrderSaveRequest request = validRequest();
    request.setStatus("shipped");
    request.setShipments(List.of());
    assertThatThrownBy(() -> service.createSalesOrder(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("至少填写一条物流信息");
}
```

Add static assertions that active order Java source contains no `getExpressCompany`, `setExpressCompany`, `getExpressNo`, or `setExpressNo` and that installation synchronization does not copy shipment fields.

- [ ] **Step 2: Run lifecycle tests and verify RED**

Run:

```powershell
cd D:\HiveManager\management
.\mvnw.cmd -Dtest=OrderMultiShipmentLifecycleTest test
```

Expected: FAIL because order contracts still use scalar logistics fields.

- [ ] **Step 3: Replace scalar lifecycle behavior**

Perform shipment writes in the same order transaction:

```java
List<SalesOrderShipmentVO> shipments = orderShipmentService.saveShipments(
        tenantCode, order.getOrderId(), request.getShipments());
validateShippingInfo(order.getStatus(), shipments);
```

For page results, call `listShipmentsByOrderIds` once for all rows, then attach lists by order ID. Detail uses `listShipments`. Validation for `shipped` checks a non-empty persisted list. Advance payloads carry `shipments`, not scalar fields.

Remove scalar logistics comparison from generic order-change detection. Let `OrderShipmentService` emit shipment-specific changes only when persisted values change.

- [ ] **Step 4: Keep installation tasks independent**

`InstallationTaskService.syncCompletedOrder` must continue creating/updating customer, project, brand, goods, quantity, and information channel fields but must not read scalar order logistics fields. Installation task status updates retain their own `expressCompany` and `expressNo` request fields.

- [ ] **Step 5: Run focused order and installation tests**

Run:

```powershell
cd D:\HiveManager\management
.\mvnw.cmd "-Dtest=OrderMultiShipmentLifecycleTest,UnifiedOrderServiceTest,PendingShipApprovalTest,UnifiedInstallationServiceTest" test
```

Expected: all focused tests PASS.

- [ ] **Step 6: Commit lifecycle integration**

```powershell
git add management/src/main/java/my/hive/domain/order management/src/main/java/my/hive/domain/installation management/src/test/java/my/hive/domain/order/service/OrderMultiShipmentLifecycleTest.java
git commit -m "feat: integrate shipments with order lifecycle"
```

### Task 4: Shipment-Specific Tracking and Audit

**Files:**
- Modify: `management/src/main/java/my/hive/api/order/OrderController.java`
- Modify: `management/src/main/java/my/hive/domain/order/service/OrderLogisticsTrackingService.java`
- Modify: `management/src/test/java/my/hive/domain/order/service/OrderLogisticsTrackingServiceTest.java`
- Modify: `management/src/main/java/my/hive/domain/order/service/OrderShipmentService.java`
- Modify: `management/src/test/java/my/hive/domain/order/service/OrderShipmentServiceTest.java`

**Interfaces:**
- Replaces `getTracking(String orderId)` with `getTracking(String orderId, Long shipmentId)`.
- Exposes only `GET /orders/{orderId}/shipments/{shipmentId}/logistics-tracking`.
- Shipment change events use actions `add_order_shipment` and `update_order_shipment`.

- [ ] **Step 1: Rewrite tracking tests first and verify RED**

Required test shape:

```java
SalesOrderShipment shipment = shipment(7L, "顺丰速运", "SF123456");
when(orderShipmentService.requireShipment("TENANT_001", "SO-001", 7L)).thenReturn(shipment);
OrderLogisticsTrackingVO result = service.getTracking("SO-001", 7L);
verify(client).query("shunfeng", "SF123456", "13800000000");
```

Assert a shipment from another order/tenant is rejected before `Kuaidi100Client.query`, and assert the cache fingerprint input includes tenant, order, shipment ID, and tracking number.

Run:

```powershell
cd D:\HiveManager\management
.\mvnw.cmd -Dtest=OrderLogisticsTrackingServiceTest test
```

Expected: FAIL because the service still resolves logistics from `SalesOrder`.

- [ ] **Step 2: Implement shipment-specific tracking**

Use `OrderService.getSalesOrderForLogisticsTracking(orderId)` only for tenant validation and customer phone. Obtain company and tracking number from `OrderShipmentService.requireShipment`. Build the cache source as:

```java
String cacheSource = String.join("|", order.getTenantCode(), orderId,
        String.valueOf(shipmentId), shipment.getTrackingNo());
String cacheKey = externalApiGuardService.fingerprint(cacheSource);
```

Remove the old controller mapping entirely.

- [ ] **Step 3: Add precise shipment operation events**

Inject `OperationLogCollector` into `OrderShipmentService`. Collect only after a successful insert or changed update:

```java
OperationLogEvent event = new OperationLogEvent();
event.setTenantCode(tenantCode);
event.setUserId(TenantPermissionContext.getUserId());
event.setModule("order");
event.setAction(isNew ? "add_order_shipment" : "update_order_shipment");
event.setBizType("order_shipment");
event.setBizNo(orderId);
event.setDescription(isNew ? "新增订单物流记录" : "修改订单物流记录");
event.setArgsJson("{\"shipmentId\":" + shipmentId + ",\"trackingFingerprint\":\"" + fingerprint + "\"}");
event.setSuccess(true);
event.setCreateTime(LocalDateTime.now());
collector.collect(event);
```

Use `ExternalApiGuardService.fingerprint` or the shared sanitizer to avoid recording a full tracking number. Tests assert unchanged rows produce no event.

- [ ] **Step 4: Run focused tracking and aggregate tests**

Run:

```powershell
cd D:\HiveManager\management
.\mvnw.cmd "-Dtest=OrderLogisticsTrackingServiceTest,OrderShipmentServiceTest" test
```

Expected: all tests PASS.

- [ ] **Step 5: Commit tracking and audit changes**

```powershell
git add management/src/main/java/my/hive/api/order/OrderController.java management/src/main/java/my/hive/domain/order/service management/src/test/java/my/hive/domain/order/service
git commit -m "feat: query tracking per order shipment"
```

### Task 5: Management Order Form and Multiple Hover Triggers

**Files:**
- Modify: `management-ui/src/views/function/order/order.vue`
- Modify: `management-ui/src/views/function/order/api/order.js`
- Modify: `management-ui/tests/order-logistics-tracking.test.js`
- Modify: `management-ui/tests/order-information-channel-and-advance.test.js`
- Create: `management-ui/tests/order-multi-shipment-ui.test.js`

**Interfaces:**
- `orderForm.shipments` stores `{id, logisticsCompany, trackingNo, version, updaterName, updateTime, isNew}`.
- `getOrderLogisticsTracking(orderId, shipmentId)` calls the canonical shipment endpoint.
- `logisticsTrackingState(orderId, shipment)` isolates state by order and shipment.

- [ ] **Step 1: Write failing UI contracts**

Assert the new form and list behavior:

```js
test('order editor saves multiple non-deletable shipment rows', () => {
  assert.match(orderSource, /orderForm\.shipments/)
  assert.match(orderSource, /function addOrderShipment\(/)
  assert.match(orderSource, /function discardUnsavedOrderShipment\(/)
  assert.match(orderSource, /shipment\.id[^]*最后修改/)
  assert.doesNotMatch(orderSource, /orderForm\.expressCompany|orderForm\.expressNo/)
  assert.match(orderSource, /shipments: orderForm\.shipments\.map/)
})

test('each tracking number opens its own hover request', () => {
  assert.match(orderSource, /v-for="shipment in row\.shipments"/)
  assert.match(orderSource, /@show="loadLogisticsTracking\(row, shipment\)"/)
  assert.match(apiSource, /getOrderLogisticsTracking\(orderId, shipmentId\)/)
  assert.match(apiSource, /\/orders\/.*\/shipments\/.*\/logistics-tracking/)
})
```

- [ ] **Step 2: Run UI tests and verify RED**

Run:

```powershell
cd D:\HiveManager\management-ui
node --test tests/order-multi-shipment-ui.test.js tests/order-logistics-tracking.test.js tests/order-information-channel-and-advance.test.js
```

Expected: FAIL on the old scalar controls and endpoint.

- [ ] **Step 3: Implement the form list**

Add helpers:

```js
function defaultOrderShipment() {
  return {id: null, logisticsCompany: '', trackingNo: '', version: null,
    updaterName: '', updateTime: '', isNew: true}
}
function addOrderShipment() {
  orderForm.shipments.push(defaultOrderShipment())
}
function discardUnsavedOrderShipment(index) {
  if (!orderForm.shipments[index]?.id) orderForm.shipments.splice(index, 1)
}
```

Saved rows have no delete command. Show `updaterName` and formatted `updateTime`. Validate a maximum of 50 rows, both fields per row, and duplicate tracking numbers. When advancing to `shipped`, require at least one complete row.

- [ ] **Step 4: Implement list rendering and isolated hover state**

Render one `el-popover` per shipment and use this state key:

```js
function logisticsTrackingKey(row, shipment) {
  return `${row.orderId || ''}:${shipment.id || shipment.trackingNo || ''}`
}
async function loadLogisticsTracking(row, shipment) {
  const tracking = logisticsTrackingState(row, shipment)
  if (tracking.loading || logisticsTrackingCacheValid(tracking.data)) return
  tracking.loading = true
  try {
    tracking.data = await getOrderLogisticsTracking(row.orderId, shipment.id)
  } finally {
    tracking.loading = false
  }
}
```

Display all numbers in stable order, show `共 N 批` when `N > 1`, and export tracking numbers joined with `、`. Do not fetch tracking during order-list loading.

- [ ] **Step 5: Run UI tests and production build**

Run:

```powershell
cd D:\HiveManager\management-ui
node --test tests/order-multi-shipment-ui.test.js tests/order-logistics-tracking.test.js tests/order-information-channel-and-advance.test.js tests/order-permission-hardening.test.js
npm run build
```

Expected: tests PASS and Vite exits 0.

- [ ] **Step 6: Commit the management UI**

```powershell
git add management-ui/src/views/function/order management-ui/tests/order-multi-shipment-ui.test.js management-ui/tests/order-logistics-tracking.test.js management-ui/tests/order-information-channel-and-advance.test.js
git commit -m "feat: support multiple order tracking numbers"
```

### Task 6: Cross-Layer Contracts, Documentation, Merge, and Package

**Files:**
- Create: `management-ui/tests/order-multi-shipment-cross-layer.test.js`
- Modify: `docs/management-ui/modules/order.md`
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/RELEASE_BUILD_INFO.txt`
- Replace generated artifact: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/backend/hive-backend.jar`
- Replace generated artifact tree: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/management-ui/dist/`
- Mirror release files: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/db-migrations/`, `scripts/`, `nginx/conf.d/`, `docker-compose.yml`, `.env.example`, `README.md`

**Interfaces:**
- Release metadata adds `OrderMultiShipmentContract=READY`.
- Final package source fields resolve to the merged `main` commit.

- [ ] **Step 1: Add the failing cross-layer gate**

The test scans Java, Vue, baseline, migration, and API source:

```js
test('active order contracts contain only shipment-list logistics', () => {
  for (const source of activeOrderSources) {
    assert.doesNotMatch(source, /expressCompany|expressNo|getExpressCompany|getExpressNo/)
  }
  assert.match(saveRequest, /List<SalesOrderShipmentSaveRequest> shipments/)
  assert.match(orderVue, /orderForm\.shipments/)
  assert.match(orderApi, /shipments\/\$\{encodeURIComponent\(shipmentId\)\}\/logistics-tracking/)
})
```

Exclude installation-task sources because that domain intentionally retains its own fields.

- [ ] **Step 2: Run the cross-layer gate**

Run:

```powershell
cd D:\HiveManager\management-ui
node --test tests/order-multi-shipment-cross-layer.test.js
```

Expected: PASS after Tasks 1-5; any remaining scalar order contract fails the gate.

- [ ] **Step 3: Update the order workflow documentation**

Document the child-table lifecycle, no-delete rule, optimistic conflict, `shipped` validation, shipment-specific tracking route, hover-only provider call, 30-minute cache, short failure cooldown, and `add_order_shipment` / `update_order_shipment` operation actions.

- [ ] **Step 4: Run complete test suites**

Run:

```powershell
cd D:\HiveManager\management
.\mvnw.cmd test
cd D:\HiveManager\management-ui
npm test
npm run build
git diff --check
```

Expected: 0 backend failures, 0 management UI failures, successful Vite build, and no whitespace errors.

- [ ] **Step 5: Commit gates and documentation**

```powershell
git add management-ui/tests/order-multi-shipment-cross-layer.test.js docs/management-ui/modules/order.md
git commit -m "test: enforce multi-shipment order contract"
```

- [ ] **Step 6: Merge the verified branch to main**

```powershell
git checkout main
git merge --ff-only codex/order-multi-shipment
```

Run the complete backend tests, management UI tests, and Vite build again from `main` before packaging.

- [ ] **Step 7: Build production artifacts from main**

```powershell
cd D:\HiveManager\management
.\mvnw.cmd clean package -DskipTests
cd D:\HiveManager\management-ui
npm run build
```

Expected: one executable `management/target/hive-backend-0.0.1-SNAPSHOT.jar` and a fresh `management-ui/dist`.

- [ ] **Step 8: Mirror the deployment package safely**

Verify source and target absolute paths first. Mirror only release-owned paths with stale-file deletion. Preserve `.env`, `mysql/data`, `redis/data`, `rabbitmq/data`, `nginx/certs`, uploads, logs, and backups. Copy the unified JAR as `backend/hive-backend.jar`.

- [ ] **Step 9: Recalculate and verify release metadata**

Update `SourceGitCommit`, `ReleasePackageGitCommit`, and
`ManagementUiSourceGitCommit` with the exact full SHA printed by
`git rev-parse HEAD` on `main`; keep `SourceBranch=main`. Calculate and write the
current backend JAR SHA-256, sorted management UI tree-manifest SHA-256,
management UI `index.html` SHA-256, management UI file count, migration manifest
SHA-256, migration checksum-catalog SHA-256, and migration count. Write the fresh
Maven and Node test totals reported by Step 4, then append exactly:

```text
OrderMultiShipmentContract=READY
```

Run `scripts/verify-release-integrity.sh` in an available Bash environment or reproduce every hash/count check locally. Report any server-only gate that cannot run on Windows.

- [ ] **Step 10: Report exact package provenance**

Report the final `main` commit, JAR SHA-256, management UI manifest SHA-256/file count, migration count, package directory, and preserved runtime-owned paths. Do not create a ZIP.
