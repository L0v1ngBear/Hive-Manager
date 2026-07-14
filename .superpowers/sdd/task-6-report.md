# Task 6 Report: Unified Inventory, Quality, and Installation Domains

## Status

Inventory, quality, and installation implementations were moved into canonical `my.hive.domain.inventory`, `my.hive.domain.quality`, and `my.hive.domain.installation` packages. Public adapters now live under `my.hive.api.inventory`, `my.hive.api.quality`, and `my.hive.api.installation`, with the application context path making the external routes `/api/inventory/**`, `/api/quality/**`, and `/api/installation-tasks/**`.

## Behavior and parity evidence

- Retired legacy management package implementations for inventory, bad-product quality, and installation task lifecycle.
- Updated order completion sync to depend on canonical `InstallationTaskService`.
- Updated approval quality flow to depend on canonical `QualityService` and `BadProductMapper`.
- Updated dashboard and notification collaborators to read inventory warnings from canonical inventory services.
- Kept exact Permission Catalog V3 annotations on canonical adapters; no legacy permission enum was introduced.
- Restored clean UTF-8 source from current HEAD legacy implementations after a failed subagent rewrite produced BOM and mojibake syntax damage.

## TDD and verification

RED:

- `management\.\mvnw.cmd "-Dtest=*Inventory*Test,*Quality*Test,*Installation*Test" test`
- Initially failed while canonical packages were incomplete, then failed on BOM/mojibake introduced by the interrupted implementer rewrite.

GREEN:

- `management\.\mvnw.cmd "-Dtest=*Inventory*Test,*Quality*Test,*Installation*Test" test`
- PASS: 10 tests, 0 failures, 0 errors, 0 skipped.

Final Task 6 gate:

- `management\.\mvnw.cmd "-Dtest=*Inventory*Test,*Quality*Test,*Installation*Test,*Permission*Test,UniqueRuntimeComponentTest" test`
- PASS: 47 tests, 0 failures, 0 errors, 0 skipped.

## Other branch and dev-target note

The user added that other-branch changes must be considered and the final delivery should land on the new repository `dev` branch. During Task 6, no branch switching or push was performed. `origin` was fetched and current local branch remains `codex/unify-hive-backend`; final integration into `dev` is deferred until the unified backend work is ready for controlled branch reconciliation.

## Self-review

- One canonical inventory service/controller: PASS.
- One canonical quality service/controller: PASS.
- One canonical installation service/controller: PASS.
- No `/web` route introduced: PASS.
- No `my.hive_back` import introduced: PASS.
- Existing unrelated `management-ui` changes were not staged: PASS.

## Concerns

- The repository still contains other legacy `my.management` domains by design; Task 6 only retired inventory, bad-product quality, and installation packages.
- Some existing tests under `my.management` still remain until Task 9 removes legacy roots.
