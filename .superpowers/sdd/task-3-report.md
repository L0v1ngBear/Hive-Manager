# Task 3 Report: Integrate Shipments Into Order Lifecycle

## Status

Implemented and review-remediated. The standard Maven lifecycle now compiles normally, and the complete management test suite passes with 259 tests.

Original Task 3 commit SHA: `fb1f7c8`

## Files

- `management/src/main/java/my/hive/domain/order/model/entity/SalesOrder.java`
- `management/src/main/java/my/hive/domain/order/model/dto/SalesOrderSaveRequest.java`
- `management/src/main/java/my/hive/domain/order/model/dto/SalesOrderUpdateRequest.java`
- `management/src/main/java/my/hive/domain/order/model/vo/SalesOrderPageVO.java`
- `management/src/main/java/my/hive/domain/order/model/vo/SalesOrderDetailVO.java`
- `management/src/main/java/my/hive/domain/order/service/OrderService.java`
- `management/src/main/java/my/hive/domain/installation/service/InstallationTaskService.java`
- `management/src/test/java/my/hive/domain/order/service/OrderMultiShipmentLifecycleTest.java`
- `management/src/test/java/my/hive/domain/order/service/PendingShipApprovalTest.java`
- `management/src/test/java/my/hive/domain/order/service/OrderApprovalSubmissionConcurrencyTest.java`

The two existing lifecycle tests required contract updates because removing the scalar methods otherwise produced 17 test-compilation errors. No controller, tracking service, UI, migration, operation-log, or progress file was changed.

## TDD Evidence

### RED

Command:

```powershell
cd D:\HiveManager\management
.\mvnw.cmd -Dtest=OrderMultiShipmentLifecycleTest test
```

Result: expected test-compilation failure with 6 missing shipment accessors (`getShipments`/`setShipments`) in the save request and list/detail VOs.

### GREEN

The Task 3 sources and focused tests were compiled directly against the existing build output to avoid compiling the explicitly deferred Task 4 tracking source, then run with:

```powershell
.\mvnw.cmd "-Dtest=OrderMultiShipmentLifecycleTest,UnifiedOrderServiceTest,PendingShipApprovalTest,UnifiedInstallationServiceTest" surefire:test
```

Result: `Tests run: 29, Failures: 0, Errors: 0, Skipped: 0`; exit 0. Breakdown: lifecycle 8, pending shipment approval 15, unified order 3, unified installation 3.

### Exact Brief Command

```powershell
.\mvnw.cmd -Dtest=OrderMultiShipmentLifecycleTest test
```

After updating the Task 3 lifecycle tests, this reaches test compilation and fails only at:

- `OrderLogisticsTrackingServiceTest.java:139`: missing `SalesOrder.setExpressCompany(String)`
- `OrderLogisticsTrackingServiceTest.java:140`: missing `SalesOrder.setExpressNo(String)`

Earlier full main compilation likewise identified the matching deferred production dependency in `OrderLogisticsTrackingService.java:69-70` (`getExpressCompany` and `getExpressNo`). Those Task 4 files were not modified per the boundary.

## Self-Review

- Full saves own `shipments`; update/advance requests have no scalar logistics fields and no temporary shipment payload.
- Create and complete-save call `saveShipments` inside rollback transactions, then validate the returned persisted list.
- All shipped transitions, approval completion, and production-to-sales synchronization validate `OrderShipmentService.listShipments` data.
- Page projection performs one batched lookup; detail performs one order-scoped lookup.
- Scalar logistics fields and generic scalar change detection were removed from the order lifecycle.
- Installation synchronization no longer copies order logistics; installation status updates retain their own express fields.
- `git diff --cached --check` passed. The pre-existing `.superpowers/sdd/progress.md` modification was excluded.

## Concerns

- No remaining compile or test blocker is known for Task 3 or Task 4; the complete Maven test lifecycle passes.
- Maven still emits existing JVM dynamic-agent/bootstrap classpath warnings. They do not change the exit code or test results.

## Review-Blocker Fix (Task 4 Follow-up)

This update supersedes the earlier statement that all shipped restoration paths were covered. Review found two paths that restored `shipped` without validating persisted shipments before updating `sales_order`:

- `approveSalesOrderRollback`, when the approved rollback target is `shipped`.
- `rejectPendingCancelSalesOrder`, when the cancellation source status is `shipped`.

### RED

Command:

```powershell
cd D:\HiveManager\management
.\mvnw.cmd -Dtest=OrderMultiShipmentLifecycleTest test
```

The first run reached standard `testCompile` and exposed the known deferred Task 4 test contract (`SalesOrder.setExpressCompany/setExpressNo`). After removing only those obsolete test helper calls, the same command executed 10 tests and failed exactly the two new regressions:

- `approvedRollbackToShippedRejectsEmptyPersistedShipmentsBeforeWrite`
- `rejectedCancellationRestoringShippedRejectsEmptyPersistedShipmentsBeforeWrite`

Both failed with `Expecting code to raise a throwable`; result: `Tests run: 10, Failures: 2, Errors: 0, Skipped: 0`.

### GREEN

Minimal fix: call `validatePersistedShippingInfo` with the target/restored status before mutating status or calling `salesOrderMapper.updateById`.

Command:

```powershell
cd D:\HiveManager\management
.\mvnw.cmd -Dtest=OrderMultiShipmentLifecycleTest test
```

Result: `BUILD SUCCESS`; `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`.

The final combined standard Maven run also includes this class and reports the same 10/10 XML total. No Maven compile phase was bypassed.

## Task 3/4 Review Remediation (2026-07-17)

### RED

Command:

```powershell
cd D:\HiveManager\management
.\mvnw.cmd "-Dtest=OrderLegacyLogisticsColumnStaticTest,OperationLogAspectScopeTest,OrderShipmentServiceTest" test
```

The standard lifecycle completed `compile` and `testCompile`, then ran 19 tests with 6 expected failures:

- The active order mapper guard found `express_company`/`express_no` in `SalesOrderMapper`.
- The real create/save `@CollectLog` path retained the complete nested `trackingNo`.
- A first successful shipment update emitted an event before a later optimistic-lock failure.
- Transaction-synchronized success emitted before commit, and simulated rollback still had an event.
- A zero-row insert returned normally instead of throwing.

Result: `Tests run: 19, Failures: 6, Errors: 0, Skipped: 0`; `BUILD FAILURE` as expected.

### Implementation

- Removed retired scalar logistics columns from the `FOR UPDATE` projection and added an order-mapper source guard.
- Added normalized `trackingno` to the global sensitive-key matcher. AOP tests use the actual `OrderController.create` and `replace` annotations and assert the request DTO remains unchanged.
- Buffered shipment event descriptors for the complete batch, built events only after all writes and the final read succeeded, and registered delivery in `afterCommit` when Spring transaction synchronization is active. Pure unit calls without synchronization publish after successful method completion.
- Required exactly one affected row for shipment inserts; zero rows now throw code 500 and produce no add event.
- Injected the Task 3 `OrderShipmentService` mock into two older hand-wired `OrderService` tests found by the complete suite.

### GREEN

Focused regression command above: `BUILD SUCCESS`; 19/19 passed after compiling 481 main sources and 61 test sources through the normal lifecycle.

Required coverage command:

```powershell
.\mvnw.cmd "-Dtest=OrderShipmentServiceTest,OrderMultiShipmentLifecycleTest,OrderLogisticsTrackingServiceTest,UniqueRuntimeComponentTest,OperationLogAspectScopeTest,OrderLegacyLogisticsColumnStaticTest" test
```

Result: `BUILD SUCCESS`; `Tests run: 38, Failures: 0, Errors: 0, Skipped: 0`.

Complete standard Maven command:

```powershell
.\mvnw.cmd test
```

The first complete run found two stale test fixtures with a null `OrderShipmentService` (`InformationChannelPropagationServiceTest` and `OrderBudgetStateTest`). After injecting the existing mock dependency, the final run returned `BUILD SUCCESS`; `Tests run: 259, Failures: 0, Errors: 0, Skipped: 0`. No compile phase or Maven lifecycle phase was bypassed.

### Review Files

- `management/src/main/java/my/hive/domain/order/mapper/SalesOrderMapper.java`
- `management/src/main/java/my/hive/domain/order/service/OrderShipmentService.java`
- `management/src/main/java/my/hive/shared/log/SensitiveDataSanitizer.java`
- `management/src/test/java/my/hive/architecture/OrderLegacyLogisticsColumnStaticTest.java`
- `management/src/test/java/my/hive/shared/aop/OperationLogAspectScopeTest.java`
- `management/src/test/java/my/hive/domain/order/service/OrderShipmentServiceTest.java`
- `management/src/test/java/my/hive/domain/order/service/InformationChannelPropagationServiceTest.java`
- `management/src/test/java/my/hive/domain/order/service/OrderBudgetStateTest.java`

### Self-Review

- No active order mapper references `express_company` or `express_no`; installation-domain columns remain untouched.
- Tracking numbers are masked only in serialized operation-log data; API request/response objects are not mutated and order logging remains enabled.
- No shipment success event is delivered for a failed batch or rollback; transaction-synchronized delivery happens only after commit.
- Insert and optimistic-lock failure paths both produce zero success events.
- The pre-existing `.superpowers/sdd/progress.md` modification remains excluded.
