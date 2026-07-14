# Task 7 Brief: Merge Customer, Document, Equipment, Label, and Printing

Plan: `docs/superpowers/plans/2026-07-14-unify-hive-backend.md`
Branch: `codex/unify-hive-backend`

## Objective

Converge customer, document, equipment, label, and printing into the unified backend so both management UI and mini-program clients use one canonical implementation per domain. Printing is the release-blocking focus: the application must expose exactly one `PrintTaskController`, one print-task persistence implementation, and one print task state machine.

## Scope

- Converge implementations into:
  - `management/src/main/java/my/hive/domain/customer`
  - `management/src/main/java/my/hive/domain/document`
  - `management/src/main/java/my/hive/domain/equipment`
  - `management/src/main/java/my/hive/domain/label`
  - `management/src/main/java/my/hive/domain/print`
- Create or retain API adapters under:
  - `management/src/main/java/my/hive/api/customer`
  - `management/src/main/java/my/hive/api/document`
  - `management/src/main/java/my/hive/api/equipment`
  - `management/src/main/java/my/hive/api/label`
  - `management/src/main/java/my/hive/api/print`
- Import only unique mini/common behavior from `D:\HiveBackend\server` and `D:\HiveCommon\hive-backend-common`; do not preserve duplicate wrapper implementations.
- Update the living documents when route or module status changes:
  - `docs/architecture/unified-backend.md`
  - `docs/api/unified-api-catalog.md`
  - `docs/migrations/unified-backend-migrations.md`
  - `docs/deployment/unified-backend-deployment.md`

## Required Tests

Create or update:

- `management/src/test/java/my/hive/architecture/PrintEndpointUniquenessTest.java`
- Focused domain tests under the target `my.hive.domain.*` packages for customer, document, equipment, label, and print behavior.

The tests must cover:

- Single print controller bean and unique print request mappings.
- Customer custom-field behavior.
- Document move/rename constraints.
- Equipment inspection behavior.
- Label default selection.
- Print task claim, report, cancel, and raw-command authorization.

## Verification Commands

Run before claiming completion:

```powershell
.\mvnw.cmd clean "-Dtest=*Customer*Test,*Document*Test,*Equipment*Test,*Label*Test,*Print*Test,UniqueRuntimeComponentTest" test
```

Also run targeted source checks:

```powershell
rg "class PrintTaskController|class PrintTaskService|@RequestMapping\\(\"/(print|print-task|print-tasks)" management/src/main/java -n
rg "my\\.management\\.module\\.(customer|document|equipment|label|print)|my\\.hive_back|PermissionCodeEnum" management/src/main/java/my/hive management/src/main/java/my/management management/src/test/java -n
```

## Constraints

- Keep all public business routes under `/api/**` through the unified context path.
- Use only `PermissionCatalogV3` exact permission codes; no wildcard, alias, old enum, or historical compatibility logic.
- Do not modify executed historical SQL. If schema changes are required, add a new migration version only.
- Preserve unrelated collaborator changes, especially current dirty `management-ui/**` files.
- Do not use git worktree.
- Finish with a focused commit containing only Task 7 files.

## Done Criteria

- One Service implementation exists per listed domain.
- Exactly one `PrintTaskController` and one print-task persistence/state implementation remain.
- Focused tests and `UniqueRuntimeComponentTest` pass from a clean compile.
- Four living documents reflect Task 7 status and route decisions.
- No duplicate Bean, duplicate mapping, old permission enum, or mini-backend runtime package reference is introduced.
