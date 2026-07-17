# Task 4 Report: Shipment-Specific Tracking and Audit

## Status

Implemented and review-remediated shipment-specific logistics tracking, shipment ownership rejection, shipment-scoped cache fingerprints, and transaction-safe add/update shipment audit events. The old order-level tracking route and all tracking reads from `sales_order` scalar logistics fields are removed.

Task 3's two shipped-restoration review blockers were fixed first and are documented in `task-3-report.md`.

## Changed Files

- `management/src/main/java/my/hive/api/order/OrderController.java`
- `management/src/main/java/my/hive/domain/order/service/OrderLogisticsTrackingService.java`
- `management/src/main/java/my/hive/domain/order/service/OrderShipmentService.java`
- `management/src/main/java/my/hive/domain/order/service/OrderService.java`
- `management/src/test/java/my/hive/domain/order/service/OrderLogisticsTrackingServiceTest.java`
- `management/src/test/java/my/hive/domain/order/service/OrderShipmentServiceTest.java`
- `management/src/test/java/my/hive/domain/order/service/OrderMultiShipmentLifecycleTest.java`
- `management/src/test/java/my/hive/architecture/UniqueRuntimeComponentTest.java`
- `.superpowers/sdd/task-3-report.md`
- `.superpowers/sdd/task-4-report.md`

The route architecture test was updated because it is the repository's authoritative assertion for canonical controller mappings.

## TDD Evidence

### Task 4 RED

Command:

```powershell
cd D:\HiveManager\management
.\mvnw.cmd "-Dtest=OrderLogisticsTrackingServiceTest,OrderShipmentServiceTest" test
```

Result: expected `testCompile` failure with 12 errors. The tests required the new four-dependency service constructors and `getTracking(String orderId, Long shipmentId)`, while production still exposed the old order-level contract.

### Task 4 GREEN

Command:

```powershell
cd D:\HiveManager\management
.\mvnw.cmd "-Dtest=OrderLogisticsTrackingServiceTest,OrderShipmentServiceTest" test
```

Result: `BUILD SUCCESS`; `Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`.

Breakdown:

- `OrderLogisticsTrackingServiceTest`: 6 passed.
- `OrderShipmentServiceTest`: 11 passed.

The command ran the standard Maven `compile`, `testCompile`, and `test` lifecycle; it did not invoke `surefire:test` directly.

### Coverage Run

Command:

```powershell
cd D:\HiveManager\management
.\mvnw.cmd "-Dtest=OrderMultiShipmentLifecycleTest,OrderLogisticsTrackingServiceTest,OrderShipmentServiceTest,UniqueRuntimeComponentTest" test
```

Result: `BUILD SUCCESS`; `Tests run: 30, Failures: 0, Errors: 0, Skipped: 0`.

Surefire XML totals:

- `UniqueRuntimeComponentTest`: tests=3, failures=0, errors=0, skipped=0.
- `OrderLogisticsTrackingServiceTest`: tests=6, failures=0, errors=0, skipped=0.
- `OrderMultiShipmentLifecycleTest`: tests=10, failures=0, errors=0, skipped=0.
- `OrderShipmentServiceTest`: tests=11, failures=0, errors=0, skipped=0.

The run compiled the standard Maven project lifecycle and exited normally. Its dumpstream contains the environment warning `Boot Manifest-JAR contains absolute paths in classpath` followed by `'other' has different root`; this did not change the exit code or XML results.

## Implementation Review

- Only `GET /orders/{orderId}/shipments/{shipmentId}/logistics-tracking` is exposed; the old order-level mapping is explicitly asserted absent.
- `OrderService.getSalesOrderForLogisticsTracking` is used only for tenant/data-scope validation and customer phone.
- Company and tracking number come from `OrderShipmentService.requireShipment`.
- The shipment's tenant, order ID, and shipment ID are defensively checked before fingerprinting or calling `Kuaidi100Client`.
- Cache input is exactly `tenant|order|shipmentId|trimmedCompany|trimmedTrackingNo`.
- New inserts emit `add_order_shipment`; successful changed updates emit `update_order_shipment`; unchanged and failed optimistic-lock updates emit no event.
- Audit `argsJson` contains only shipment ID and a SHA-256 tracking fingerprint. Tests assert the complete tracking number is absent.
- No `expressCompany` or `expressNo` field was restored on `SalesOrder`.
- Task 3 shipped restoration validates persisted shipments before mutating status or writing the order.

## Cache Identity Review Correction (2026-07-17)

The original Task 4 cache identity omitted the shipment's current logistics company. That allowed a shipment changed to another company while retaining the same tracking number to reuse the previous company's cached response. The corrected identity fingerprints `tenant|order|shipmentId|trimmedCompany|trimmedTrackingNo`; both company and tracking number come from the existing `required(...)` validation/trim path.

`OrderLogisticsTrackingServiceTest` now asserts the complete corrected fingerprint, including a whitespace-padded company and tracking number fixture to prove normalization occurs before fingerprinting.

## Concerns

- No remaining compile or test blocker is known; the complete management suite passes with 259 tests.
- Existing JVM dynamic-agent/bootstrap warnings remain and do not affect the successful Maven result.

## Task 3/4 Review Remediation (2026-07-17)

### RED

```powershell
cd D:\HiveManager\management
.\mvnw.cmd "-Dtest=OrderLegacyLogisticsColumnStaticTest,OperationLogAspectScopeTest,OrderShipmentServiceTest" test
```

Result after normal `compile`/`testCompile`: `Tests run: 19, Failures: 6, Errors: 0, Skipped: 0`. Failures proved the stale mapper projection, unmasked nested tracking number, early batch/transaction event delivery, rollback event leakage, and unchecked zero-row insert.

### GREEN

The same focused command passed 19/19 after the minimal fixes. Required coverage then passed with:

```powershell
.\mvnw.cmd "-Dtest=OrderShipmentServiceTest,OrderMultiShipmentLifecycleTest,OrderLogisticsTrackingServiceTest,UniqueRuntimeComponentTest,OperationLogAspectScopeTest,OrderLegacyLogisticsColumnStaticTest" test
```

Result: `BUILD SUCCESS`; `Tests run: 38, Failures: 0, Errors: 0, Skipped: 0`.

The complete command `.\mvnw.cmd test` initially exposed two stale hand-wired `OrderService` fixtures missing the Task 3 shipment-service mock. After fixing only those fixtures, the final standard Maven run passed: `Tests run: 259, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`.

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

- Active order mappers are statically barred from the retired scalar columns.
- Actual order create/save AOP logging masks nested `trackingNo` while preserving the API DTO and keeping business logging enabled.
- Shipment events are prepared after the whole batch succeeds, delivered after commit when transaction synchronization exists, and never delivered on rollback or any failed write.
- Insert zero-row and optimistic-lock failure paths produce no success audit event.
- The pre-existing `.superpowers/sdd/progress.md` modification remains outside this review fix.
