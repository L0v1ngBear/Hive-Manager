# Order Workflow and Information Channel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship one consistent order workflow across management web and WeChat mini program, directly replace delivery-date semantics with information-channel text, and close the drawing-budget, cancellation, auditor, and flow-QR defects.

**Architecture:** Use a single incompatible contract named `informationChannel` backed by `information_channel`, deploy it through one new immutable migration, then update both backends and both clients as one release unit. A flow action saves edits while retaining the current status and calls the transition endpoint only after save succeeds; backend state machines and permissions remain authoritative.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, MySQL 8, Vue 3, Vite, WeChat Mini Program, Node test runner, Maven.

## Global Constraints

- Never modify an executed historical migration; add a new migration after `V20260710_004_order_role_status_scope.sql`.
- Do not retain `deliveryDate`, `delivery_date`, alias reads, dual writes, or old-client fallback code.
- Use `informationChannel: String` and `information_channel VARCHAR(100)` everywhere.
- Convert historical values to `历史交付日期：YYYY-MM-DD` or `历史交付日期：<原值>`.
- Preserve order visibility by status and role: sales and production staff only see their authorized scopes.
- `drawing_budget` permits only `budgeting -> budget_completed`; `budget_completed` is terminal.
- Save edits with the current status before calling the next-stage endpoint; never prewrite the target status.
- `pending_ship -> shipped` requires order approval; save logistics first, retain `pending_ship`, and change to `shipped` only after all auditors approve.
- Cancellation requires `cancelReason`, trimmed nonblank, maximum 500 characters.
- Uninvoiced warning defaults to 30 days, is configurable from 1 to 365 days, starts at order creation, and excludes drawing-budget, pending-cancel, and cancelled orders.
- Explicit `auditorIds` override configured defaults; all selected auditors must approve and any rejection closes the approval.
- The QR payload is exactly `HIVE_ORDER_FLOW:<sales|production>:<base64url-hmac-sha256>:<orderId>`.
- Each task follows RED, GREEN, focused regression, `git diff --check`, and a focused commit.

---

### Task 1: Versioned Database Contract

**Files:**
- Create: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/db-migrations/migrations/V20260713_001_order_information_channel_and_cancel_reason.sql`
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/db-migrations/migration_manifest.txt`
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/db-migrations/scripts/verify-online-schema.sh`
- Create: `D:/HiveManager/management-ui/tests/order-information-channel-migration.test.js`
- Modify: `D:/HiveManager/management-ui/tests/deploy-migration-immutability.test.js`

**Interfaces:**
- Produces: `sales_order.information_channel`, `production_order.information_channel`, `installation_task.information_channel`, and `sales_order.cancel_reason`.
- Removes: the three `delivery_date` columns and their delivery-date index.

- [ ] **Step 1: Write the failing migration contract test**

```js
assert.ok(sql.includes('information_channel'))
assert.ok(sql.includes('cancel_reason'))
assert.ok(sql.includes('历史交付日期：'))
assert.ok(sql.includes('DROP COLUMN `delivery_date`'))
assert.ok(!manifest.includes('manual/V20260713_001'))
```

- [ ] **Step 2: Run the test and confirm RED**

Run: `node management-ui/tests/order-information-channel-migration.test.js`

Expected: failure because `V20260713_001_order_information_channel_and_cancel_reason.sql` does not exist.

- [ ] **Step 3: Add the immutable migration**

Use `information_schema` guards following the existing deployment migrations. For each table, add the new column, copy history using `CASE` and `DATE_FORMAT`, drop the old index when present, then drop `delivery_date`. Add `cancel_reason VARCHAR(500)` to `sales_order` and append the migration path to the manifest.

- [ ] **Step 4: Extend online schema verification**

Require all four new columns, reject remaining `delivery_date` columns, and require the migration history row to be `SUCCESS`.

- [ ] **Step 5: Run migration tests and commit**

Run: `node management-ui/tests/order-information-channel-migration.test.js && node management-ui/tests/deploy-migration-immutability.test.js && node management-ui/tests/deploy-schema-verifier.test.js`

Expected: all pass.

Commit: `feat: migrate orders to information channel`

### Task 2: Management Backend Contract

**Files:**
- Modify: `D:/HiveManager/management/src/main/java/my/management/module/order/model/**`
- Modify: `D:/HiveManager/management/src/main/java/my/management/module/order/service/OrderService.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/module/installation/model/**`
- Modify: `D:/HiveManager/management/src/main/resources/sql/installation_task.sql`
- Modify: `D:/HiveManager/management/src/main/resources/sql/core_performance_indexes.sql`
- Test: `D:/HiveManager/management/src/test/java/my/management/order/OrderInformationChannelContractTest.java`

**Interfaces:**
- Consumes: the Task 1 database columns.
- Produces: request and response JSON containing `informationChannel` only.

- [ ] **Step 1: Add a failing reflection/static contract test**

Assert that sales, production, and installation entities expose a `String informationChannel`, DTO/VO classes contain no `deliveryDate`, and resource SQL contains no `delivery_date`.

- [ ] **Step 2: Confirm RED**

Run: `mvn -pl management -Dtest=OrderInformationChannelContractTest test`

Expected: failures naming existing `deliveryDate` fields.

- [ ] **Step 3: Rename the complete backend contract**

Replace annotations with `@TableField("information_channel")`, remove date types and validators, preserve the ordinary-order/drawing-budget requiredness, and pass the string through sales-to-production and installation-task synchronization. Print payloads use `payload.put("informationChannel", ...)` only.

- [ ] **Step 4: Run focused and regression tests**

Run: `mvn -pl management -Dtest=OrderInformationChannelContractTest,CommercialHardeningStaticTest test`

Expected: all pass.

- [ ] **Step 5: Commit**

Commit: `refactor: replace delivery date with information channel`

### Task 3: Mini Backend Contract

**Files:**
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/**`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/installation/**`
- Test: `D:/HiveBackend/server/src/test/java/my/hive_back/order/OrderInformationChannelContractTest.java`

**Interfaces:**
- Produces: `/api/orders/list`, detail, create, update, print, and installation sync responses containing `informationChannel` only.
- Preserves: category-specific `flow-advance` behavior from the drawing-budget fix.

- [ ] **Step 1: Write the failing backend contract test**

Assert mapper SQL uses `information_channel`, entities and VOs expose `String informationChannel`, request validation has no `@Future`, and print payload has no `deliveryDate` key.

- [ ] **Step 2: Confirm RED**

Run: `mvn -Dtest=OrderInformationChannelContractTest test`

Expected: failures for old fields, SQL, and validators.

- [ ] **Step 3: Implement the direct replacement**

Rename mapper projections and writes, remove delivery-date range behavior, preserve tenant and permission predicates, and synchronize the text value into production and installation records.

- [ ] **Step 4: Run focused state-machine and permission regressions**

Run: `mvn -Dtest=OrderInformationChannelContractTest,SalesOrderFlowAdvanceTest,CommercialHardeningStaticTest test`

Expected: all pass.

- [ ] **Step 5: Commit**

Commit: `refactor: align mini orders on information channel`

### Task 4: Management Web Flow Editor

**Files:**
- Modify: `D:/HiveManager/management-ui/src/views/function/order/order.vue`
- Modify: `D:/HiveManager/management-ui/src/views/function/installationTask/installationTask.vue`
- Modify: `D:/HiveManager/management-ui/src/views/manual/UserManual.vue`
- Create: `D:/HiveManager/management-ui/tests/order-information-channel-ui.test.js`
- Create: `D:/HiveManager/management-ui/tests/order-advance-editor.test.js`

**Interfaces:**
- Consumes: `informationChannel` management API and existing order save/next endpoints.
- Produces: an advance intent that saves the current state and then invokes next-stage flow.

- [ ] **Step 1: Add failing UI contract tests**

Assert that no target file contains `deliveryDate`, `交付日期`, a delivery date picker, or a direct next-stage call from the click handler. Assert an `advanceIntent` branch saves first and advances second.

- [ ] **Step 2: Confirm RED**

Run: `node tests/order-information-channel-ui.test.js && node tests/order-advance-editor.test.js`

Expected: both fail against the old page.

- [ ] **Step 3: Replace field and display semantics**

Use a text input bound to `orderForm.informationChannel`, display the same field in list/detail/installation views, remove date conversion and date ordering, and update manual copy.

- [ ] **Step 4: Implement edit-before-advance**

Set an explicit advance intent, load detail, open the existing modal, save with `status: currentStatus`, then call next. For target `shipped`, require `expressCompany` and `expressNo`; treat a successful next call as an approval submission, show `已提交发货审批，审批通过后进入已发货`, and retain `pending_ship` locally. On partial failure show `订单已保存，流转未完成，请重试` and reload without changing status locally.

- [ ] **Step 5: Verify and commit**

Run: `node tests/order-information-channel-ui.test.js && node tests/order-advance-editor.test.js && npm run build`

Expected: tests and Vite build pass.

Commit: `feat: edit orders before stage advance`

### Task 5: Mini Program Flow Editor

**Files:**
- Modify: `D:/productHiveFrontend/client/pages/salesOrderCreate/salesOrderCreate.js`
- Modify: `D:/productHiveFrontend/client/pages/salesOrderCreate/salesOrderCreate.wxml`
- Modify: `D:/productHiveFrontend/client/pages/salesOrder/salesOrder.js`
- Modify: `D:/productHiveFrontend/client/pages/salesOrder/salesOrder.wxml`
- Modify: `D:/productHiveFrontend/client/pages/orderDetail/orderDetail.js`
- Modify: `D:/productHiveFrontend/client/pages/orderDetail/orderDetail.wxml`
- Create: `D:/productHiveFrontend/client/tests/order-information-channel-mini.test.js`
- Create: `D:/productHiveFrontend/client/tests/order-advance-editor-mini.test.js`

**Interfaces:**
- Consumes: mini backend `informationChannel` contract and existing update/flow-advance APIs.
- Produces: `salesOrderCreate?id=<id>&advance=1` edit mode.

- [ ] **Step 1: Add failing mini source/behavior tests**

Assert no page binds `deliveryDate`, the field is an input rather than a date picker, and advancing navigates to edit mode before any transition request.

- [ ] **Step 2: Confirm RED**

Run: `node --test client/tests/order-information-channel-mini.test.js client/tests/order-advance-editor-mini.test.js`

Expected: both fail against the old client.

- [ ] **Step 3: Implement information-channel and edit mode**

Load detail when `id` exists, bind `formData.informationChannel`, update the current order first, and invoke flow advance only when `advance=1` and update succeeds. For `pending_ship`, require logistics, show `已提交发货审批，审批通过后进入已发货`, and keep the status unchanged until approval. Preserve horizontal click-only filters and the `budget_completed` terminal filter.

- [ ] **Step 4: Verify and commit**

Run: `node --test client/tests/order-information-channel-mini.test.js client/tests/order-advance-editor-mini.test.js client/tests/drawing-budget-flow-regression.test.js client/tests/order-mini-alignment.test.js`

Expected: all pass.

Commit: `feat: align mini order editing and information channel`

### Task 6: Uninvoiced Time Warning

**Files:**
- Modify: management `OrderSetting`, mapper/service, warning summary, order page request/VO, and warning cache.
- Modify: mini backend unified order list/summary request and VO.
- Modify: management and mini order pages.
- Create: `V20260713_002_order_invoice_warning_setting.sql` and focused backend/frontend tests.

**Interfaces:**
- Produces: `invoiceWarningDays`, `invoiceWarning`, `uninvoicedDays`, `invoiceWarningOnly`, and summary key `invoice_warning`.
- Preserves: status-scope and tenant-scope filtering in every count and page query.

- [ ] **Step 1: Add failing warning-contract tests**

Cover default 30 days, 1/365 boundaries, order `create_time` as the non-resetting origin, excluded categories/statuses, scoped counts, cache invalidation after invoicing, and both clients' card/filter/row marker.

- [ ] **Step 2: Add the immutable setting migration**

Add `order_setting.invoice_warning_days INT NOT NULL DEFAULT 30` through `V20260713_002_order_invoice_warning_setting.sql`, append it to the manifest, baseline, schema verifier, health checks, and immutable migration tests.

- [ ] **Step 3: Implement backend calculation**

Count and filter only rows where `is_invoice = 0`, `create_time <= NOW() - invoiceWarningDays`, category is not `drawing_budget`, and status is neither `pending_cancel` nor `cancelled`. Return `invoiceWarning` and whole-day `uninvoicedDays` without allowing edits to reset age.

- [ ] **Step 4: Implement both order-page surfaces**

Add the setting field, “未开票预警” summary card, `invoiceWarningOnly` filter, and row text `已未开票 N 天`. Clicking the card applies the filter; horizontal scrolling alone does not select it.

- [ ] **Step 5: Verify and commit by repository**

Run focused backend tests, management UI tests/build, full mini Node tests, migration checks, and `git diff --check`.

### Task 7: Cancellation and Auditor Enforcement

**Files:**
- Modify: management and mini order DTOs/services/controllers used by cancel and approval creation.
- Modify: management order UI and mini edit UI.
- Modify: management approval configuration UI and both approval-center access services.
- Create: focused cancel-reason and assigned-auditor access tests in each affected repository.

**Interfaces:**
- Produces: `cancelReason: String` and `auditorIds: number[]` request fields.
- Enforces: cancellation validation before side effects and assigned-auditor row access without broad order-list access.

- [ ] **Step 1: Write failing tests**

Cover blank/whitespace/over-500 cancellation reasons, explicit auditors overriding defaults, cross-tenant/disabled/unauthorized auditors, all-approve/any-reject behavior, assigned-auditor detail access, and `pending_ship -> shipped` remaining pending until all selected auditors approve.

- [ ] **Step 2: Implement server validation and persistence**

Validate before status logs or approval candidates are written, persist `cancel_reason`, expose it in order and approval detail, and record `取消原因：<内容>` in the status log.

- [ ] **Step 3: Implement checkbox auditor selection**

Use one checkbox selection component in create, edit, advance, and rollback flows; remove the old multi-select. Limit selections to 30 and send `auditorIds`.

- [ ] **Step 4: Correct default-auditor permissions and scoped reads**

Map configuration access to each approval type or the dedicated approval-management permission. Allow a selected auditor to read only the approval and source-order fields required for their assigned record.

- [ ] **Step 5: Run focused backend/UI tests and commit per repository**

Expected: all new tests pass and no broad order visibility is granted.

### Task 8: QR, Documentation, Integration, and Release

**Files:**
- Modify: `D:/productHiveFrontend/client/utils/templatePrinter.js`
- Test: `D:/productHiveFrontend/client/tests/order-flow-qr-rendering-regression.test.js`
- Modify: `D:/HiveManager/docs/management-ui/modules/order.md`
- Modify: `D:/HiveManager/docs/management-ui/modules/approval.md`
- Modify: deployment release/check scripts only where required for the new migration and artifacts.

**Interfaces:**
- Consumes: all prior repository commits and the fixed QR protocol.
- Produces: reviewed integration branches and deployment artifacts with recorded hashes.

- [ ] **Step 1: Fix QR rendering with a failing regression**

Render a template containing `${flowQrPayload}` where the payload contains JSON braces. Assert the TSPL `QRCODE` value is nonempty and byte-equal after unescaping. Replace placeholders in a single pass so inserted values are never parsed again.

- [ ] **Step 2: Update durable module documentation**

Document both state machines, information-channel contract, edit-before-advance sequence, cancellation, auditor scopes, and exact QR payload.

- [ ] **Step 3: Review every task commit before integration**

Check spec compliance and code quality, resolve Critical/Important findings, then cherry-pick in this order: migration, management backend, mini backend, management web, mini client, cancellation/auditor, QR/docs.

- [ ] **Step 4: Run full verification**

Run management Maven tests, mini backend Maven tests, management UI tests/build, mini Node tests, migration checks, package-size check, and `git diff --check` in every repository.

- [ ] **Step 5: Build release artifacts and verify hashes**

Build both jars and management dist, copy them into `C:/Users/HUAWEI/Desktop/hive部署_全新配置`, verify source artifact SHA256 equals the artifact inside each rebuilt container, then run smoke tests.

- [ ] **Step 6: Perform physical acceptance**

Create and edit an order, advance after editing, cancel with a reason, complete a drawing budget, approve with selected auditors, create an installation task, print a 40mm QR, and scan it with the mini program.
