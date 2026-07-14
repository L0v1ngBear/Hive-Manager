# Task 5 Report: Unified Order and Approval Domains

## Status

Implementation complete. The management implementation was selected as the canonical behavioral superset and moved, without parallel admin/mini variants, to `my.hive.domain.order` and `my.hive.domain.approval`. Public controllers now live in `my.hive.api.order` and `my.hive.api.approval`; with the `/api` servlet context their collections are `/api/orders/**` and `/api/approval/**`.

## Behavior and parity evidence

- Added a method-level matrix to `docs/api/unified-api-catalog.md`: 19 order rows and 24 approval rows covering old management/mini method and route, canonical route, exact Permission Catalog V3 code, request/response type, service method, and disposition.
- Compared the read-only mini source under `D:\HiveBackend\server` with management models/mappers/services before deleting duplicates. Table-backed order and approval entities map the same business records; management retained additional validation, tenant constraints, warning, attachment, approval-candidate, and transaction behavior.
- Preserved management information-channel propagation, guarded state transitions, drawing-budget terminal behavior, rollback restrictions, pending-shipment approval, multi-auditor selection, and row-lock concurrency behavior.
- Ported the mini-only signed flow-code advance entry point into the canonical `OrderService`; it verifies order type, tenant-bound signature, and then uses the guarded canonical transition.
- Runtime scan finds exactly one public `OrderService`, `ApprovalService`, `OrderController`, and `ApprovalController`. No `my.management.module.order` or `my.management.module.approval` references remain.
- No legacy wildcard permission remains in Task 5 fixtures; old `Set.of("*")` setup was replaced with exact V3 state-action and audit codes.

## TDD evidence

RED:

- `management/.\mvnw.cmd "-Dtest=*Order*Test,*Approval*Test" test`
- Failed at test compilation because `my.hive.domain.order` and `my.hive.domain.approval` did not yet exist (4 missing-package errors).

GREEN:

- `management/.\mvnw.cmd "-Dtest=*Order*Test,*Approval*Test" test`
- PASS: 36 tests, 0 failures, 0 errors, 0 skipped.
- Coverage includes information-channel propagation, budget and normal status transitions, rollback restrictions, pending-shipment approval, concurrent submission/tenant isolation, auditor decision locking, and transaction-boundary assertions.

Runtime uniqueness/mapping gate:

- `management/.\mvnw.cmd "-Dtest=UniqueRuntimeComponentTest" test`
- PASS: 3 tests, 0 failures, 0 errors, 0 skipped.
- The Spring context loads both canonical mapper packages, has no duplicate bean names, and resolves the canonical `/orders` controller mappings under servlet context `/api`.

## Self-review

- One service/model/mapper implementation per domain: PASS.
- No Admin/Mini service split: PASS.
- Approved public route roots: PASS (`/api` context + `/orders`; `/api` context + `/approval`).
- Exact Permission Catalog V3 codes only: PASS.
- Existing unrelated `management-ui` changes were neither edited nor staged: PASS.
- Read-only source repositories were not modified: PASS.

## Concerns

- Maven emits existing Byte Buddy dynamic-agent and deprecated-API warnings; tests still exit successfully.
- The source tree still contains other `my.management.module.*` domains by design; only order and approval were in Task 5 scope.
