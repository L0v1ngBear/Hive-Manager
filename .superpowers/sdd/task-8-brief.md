# Task 8 Brief: Notifications, WeChat, Attendance, Statistics, and Scheduled Work

Plan: `docs/superpowers/plans/2026-07-14-unify-hive-backend.md`
Branch: `codex/unify-hive-backend`

## Objective

Converge notifications, WeChat channel adapters, attendance, statistics, and maintenance jobs into the unified backend. The result must have one notification service, one attendance/statistics implementation, one XXL-JOB executor configuration, one handler per job name, and one RabbitMQ listener per queue.

## Scope

- Converge domain code into:
  - `management/src/main/java/my/hive/domain/notification`
  - `management/src/main/java/my/hive/domain/attendance`
- Converge channel/infrastructure code into:
  - `management/src/main/java/my/hive/infrastructure/wechat`
  - `management/src/main/java/my/hive/infrastructure/sms`
  - `management/src/main/java/my/hive/infrastructure/scheduler`
- Retire duplicate runtime implementations from legacy `my.management.module.notification`, `my.management.module.attendance`, and mini/common duplicated scheduler/listener packages when parity is reached.
- Update the four living documents when route, scheduler, listener, or deployment contract changes.

## Required Tests

Create or update:

- `management/src/test/java/my/hive/architecture/UniqueScheduledWorkTest.java`
- `management/src/test/java/my/hive/domain/notification/UnifiedNotificationServiceTest.java`
- Attendance/statistics focused tests under `my.hive.domain.attendance` as needed.

The tests must cover:

- Unique `@XxlJob` handler names.
- Unique `@Scheduled` jobs for the unified runtime.
- Unique `@RabbitListener` queue consumers.
- Required handler set: attendance statistics, inventory statistics, notification closed loop, runtime audit, capacity report, and cleanup.
- Notification idempotent creation, read/close transitions, announcement visibility.
- WeChat subscription registration/send limits.
- Attendance punch rules and daily-stat recomputation.

## Verification Commands

Run before claiming completion:

```powershell
.\mvnw.cmd clean "-Dtest=*Notification*Test,*Attendance*Test,UniqueScheduledWorkTest,UniqueRuntimeComponentTest" test
```

Also run targeted source checks:

```powershell
rg "@XxlJob|@Scheduled|@RabbitListener" management/src/main/java -n
rg "my\\.management\\.module\\.(notification|attendance)|my\\.hive_back|PermissionCodeEnum" management/src/main/java/my/hive management/src/main/java/my/management management/src/test/java -n
```

## Constraints

- Keep public routes under `/api/**`.
- Use one app name `hive-backend`, one executor port, one scheduler enable flag, and one log path.
- Do not duplicate RabbitMQ consumers or scheduled handlers.
- Use Permission Catalog V3 exact permission codes only.
- Do not modify executed historical SQL; add a new migration only if schema changes are truly required.
- Preserve unrelated collaborator changes, especially current dirty `management-ui/**` files and `EmployeeController`.
- Do not use git worktree.

## Done Criteria

- One notification domain service and one attendance/statistics implementation remain.
- One WeChat channel adapter set is used by both clients.
- One scheduler/executor/listener set exists at runtime.
- Focused notification/attendance/scheduled-work tests and `UniqueRuntimeComponentTest` pass from a clean compile.
- Four living documents and SDD progress/report are updated.
