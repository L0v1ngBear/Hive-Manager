# Order Invoice Warning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the `other` invoice state and a fixed seven-day unissued-invoice warning without changing the existing stale-order warning.

**Architecture:** Keep `sales_order.is_invoice` as a tinyint and introduce an order-domain enum for values `0/1/2`. A deterministic invoice-warning policy calculates age from `create_time`; `OrderService` maps the result into list VOs and exposes separate summary counts. The management UI consumes these fields for filtering, labels, neutral settled colors, and overdue warning text.

**Tech Stack:** Java 21, Spring Boot 3.1, MyBatis-Plus, JUnit 5, Vue 3, Element Plus, Node test runner.

## Global Constraints

- `0` means unissued, `1` means issued, and `2` means other.
- Only unissued orders that are at least 7 complete days old trigger invoice warning.
- `pending_cancel` and `cancelled` orders never trigger invoice warning.
- Issued and other states use neutral gray styling and never trigger invoice warning.
- Existing stale-order warning settings and calculations remain unchanged.
- Do not build or refresh the deployment package in this task.

---

### Task 1: Invoice Status and Warning Policy

**Files:**
- Create: `management/src/main/java/my/hive/domain/order/model/enums/OrderInvoiceStatusEnum.java`
- Create: `management/src/main/java/my/hive/domain/order/service/OrderInvoiceWarningPolicy.java`
- Create: `management/src/test/java/my/hive/domain/order/service/OrderInvoiceWarningPolicyTest.java`

**Interfaces:**
- Produces: `OrderInvoiceStatusEnum.normalize(Integer): Integer`.
- Produces: `OrderInvoiceWarningPolicy.evaluate(Integer, String, LocalDateTime, LocalDateTime): WarningResult`.

- [ ] **Step 1: Write failing policy tests**

Cover `0/1/2`, invalid value rejection, 6-day and 7-day boundaries, update-time independence, and cancel exclusions. Assert `WarningResult.warning()`, `ageDays()`, and fixed `warningDays() == 7`.

- [ ] **Step 2: Verify the tests fail**

Run:

```powershell
cd D:\HiveManager\management
mvn "-Dtest=OrderInvoiceWarningPolicyTest" test
```

Expected: compilation failure because the enum and policy do not exist.

- [ ] **Step 3: Implement the enum and deterministic policy**

The enum accepts only `0`, `1`, and `2`; null defaults to `0`. The policy computes complete days with `ChronoUnit.DAYS.between(createTime, now)` and evaluates the threshold against `createTime` only.

- [ ] **Step 4: Verify the focused tests pass**

Run the command from Step 2. Expected: all policy tests pass.

### Task 2: Unified Backend List, Filter, and Summary Contract

**Files:**
- Modify: `management/src/main/java/my/hive/domain/order/service/OrderService.java`
- Modify: `management/src/main/java/my/hive/domain/order/model/vo/SalesOrderPageVO.java`
- Modify: `management/src/main/java/my/hive/domain/order/model/dto/SalesOrderSaveRequest.java`
- Create: `management/src/test/java/my/hive/domain/order/service/OrderInvoiceServiceContractTest.java`

**Interfaces:**
- Adds `invoiceWarning`, `invoiceAgeDays`, and `invoiceWarningDays` to `SalesOrderPageVO`.
- Adds `invoice_other` and `invoice_warning` to `GET /orders/status-summary`.
- Keeps request parameter `isInvoice` and expands accepted values to `0/1/2`.

- [ ] **Step 1: Write failing service contract tests**

Assert that save/filter normalization preserves `2`, invalid values fail, list mapping exposes the warning result, and summary query keys include `invoice_other` and `invoice_warning`.

- [ ] **Step 2: Verify the service tests fail**

```powershell
cd D:\HiveManager\management
mvn "-Dtest=OrderInvoiceServiceContractTest" test
```

Expected: failure because the VO fields and summary keys are absent and value `2` is normalized to `0`.

- [ ] **Step 3: Integrate the policy**

Replace binary normalization with `OrderInvoiceStatusEnum.normalize`. Apply `OrderInvoiceWarningPolicy` while mapping each sales-order row. Count values `0`, `1`, and `2` separately, and count invoice warnings with `is_invoice = 0`, `create_time <= now - 7 days`, excluding `pending_cancel` and `cancelled`.

- [ ] **Step 4: Run focused and existing order tests**

```powershell
mvn "-Dtest=OrderInvoiceWarningPolicyTest,OrderInvoiceServiceContractTest,PendingShipApprovalTest,OrderNoteServiceTest" test
```

Expected: all tests pass without changing approval or note behavior.

### Task 3: Management UI Three-State Display

**Files:**
- Modify: `management-ui/src/views/function/order/order.vue`
- Create: `management-ui/tests/order-invoice-warning.test.js`

**Interfaces:**
- Consumes `invoice_other`, `invoice_warning`, `invoiceWarning`, `invoiceAgeDays`, and `invoiceWarningDays`.
- Submits integer invoice statuses `0/1/2` through the existing save request.

- [ ] **Step 1: Write the failing UI contract test**

Assert the form and filter include value `2`, the summary includes `invoice_other`, labels use all three Chinese names, issued/other resolve to neutral classes, and overdue unissued rows render `未开票 X 天`.

- [ ] **Step 2: Verify the UI test fails**

```powershell
cd D:\HiveManager\management-ui
node --test tests/order-invoice-warning.test.js
```

Expected: failure because value `2` and invoice warning rendering are absent.

- [ ] **Step 3: Implement the minimal UI changes**

Add “其他类型” to filter and edit selects, add its summary card, switch label/class helpers to explicit three-state handling, render invoice warning metadata beside the invoice state, and make both settled states neutral gray. Keep the existing stale-warning tag and category warning settings unchanged.

- [ ] **Step 4: Verify UI contracts and production build**

```powershell
node --test tests/order-invoice-warning.test.js tests/element-plus-order.test.js tests/order-information-channel-and-advance.test.js
npm run build
```

Expected: tests and build pass.

### Task 4: Documentation and Full Regression

**Files:**
- Modify: `docs/management-ui/modules/order.md`
- Modify: `docs/api/unified-api-catalog.md`

**Interfaces:**
- Documents three invoice states and the independent fixed seven-day warning.

- [ ] **Step 1: Update module and API documentation**

Document `0/1/2`, list response warning fields, `invoice_other`, `invoice_warning`, exclusions, and the rule that editing does not restart invoice timing.

- [ ] **Step 2: Run full regression suites**

```powershell
cd D:\HiveManager\management
mvn test
cd D:\HiveManager\management-ui
node --test
npm run build
```

Expected: zero failures.

- [ ] **Step 3: Commit source and tests**

```powershell
cd D:\HiveManager
git add management management-ui docs
git commit -m "feat: add seven-day invoice warning"
```

- [ ] **Step 4: Confirm packaging is deferred**

Verify that `C:\Users\HUAWEI\Desktop\hive部署_全新配置` was not modified and report that a deployment package will be generated only after an explicit packaging request.
