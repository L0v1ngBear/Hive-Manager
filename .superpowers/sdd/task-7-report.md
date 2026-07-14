# Task 7 Report: Customer, Document, Equipment, Label, and Printing

## Status

Customer, document, equipment, label-template, receipt-print, and generic print-task code was converged into canonical `my.hive` packages. Public adapters now live under `my.hive.api.customer`, `my.hive.api.document`, `my.hive.api.equipment`, `my.hive.api.label`, and `my.hive.api.print`; domain implementations live under `my.hive.domain.customer`, `my.hive.domain.document`, `my.hive.domain.equipment`, `my.hive.domain.label`, and `my.hive.domain.print`.

## Behavior and parity evidence

- Retired the runtime package roots `my.management.module.customer`, `my.management.module.document`, `my.management.module.equipment`, `my.management.module.label`, and `my.management.module.receipt`.
- Moved generic print-task persistence/service code from shared print infrastructure into canonical `my.hive.domain.print`.
- Moved `PrintTaskController` into `my.hive.api.print`; `PrintEndpointUniquenessTest` asserts exactly one controller file exists.
- Updated dependent order, dashboard, price, receipt, label, and equipment references to canonical packages.
- Kept public route roots under the unified `/api` context: `/customer`, `/document`, `/equipment`, `/label-template`, `/receipt`, and `/print-task` at controller level.

## TDD and verification

RED:

- `management\.\mvnw.cmd "-Dtest=PrintEndpointUniquenessTest" test`
- Failed because Task 7 domains were still under `my.management` packages and `PrintTaskController` was under `my.hive.shared.print`.

GREEN:

- `management\.\mvnw.cmd clean "-Dtest=*Customer*Test,*Document*Test,*Equipment*Test,*Label*Test,*Print*Test,UniqueRuntimeComponentTest" test`
- PASS: 15 tests, 0 failures, 0 errors, 0 skipped; 448 main sources were recompiled.

## Self-review

- One canonical print-task controller: PASS.
- Customer/document/equipment/label/print canonical source roots: PASS.
- No Task 7 legacy runtime package imports in main/test source: PASS.
- No new SQL migration required: PASS.
- Existing unrelated `management-ui` changes were not staged: PASS.

## Concerns

- This task is primarily package/API convergence and structural behavior gating. Deeper end-to-end business tests for customer/document/equipment/label/print should be expanded if these domains change further.
