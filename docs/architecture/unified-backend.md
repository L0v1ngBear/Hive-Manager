# Unified Backend Architecture

## Target

The management application is the convergence shell for one Spring Boot application under `my.hive`, one `/api` context, and one runtime implementation of every shared capability. `D:\HiveBackend\server` and `D:\HiveCommon\hive-backend-common` are immutable source references, not additional runtimes.

Task 2 establishes `my.hive.HiveApplication` as the only executable entry point. Its transitional `my` component-scan root keeps existing management endpoints registered while domains move beneath `my.hive`. Shared infrastructure now lives in the application artifact under `my.hive.shared`, with RabbitMQ operation-log adapters under `my.hive.infrastructure.messaging`; the external common JAR is no longer a runtime dependency.

`UniqueRuntimeComponentTest` now boots the authoritative application context and reads Spring's resolved bean registry and `RequestMappingHandlerMapping`. This runtime resolution covers composed stereotypes, annotation aliases, path/value arrays, constants, and method-specific request conditions that the Task 1 source regexes could not model.

## Shared request lifecycle

1. `WebMvcConfig` applies the shared CORS, static upload, authentication, and platform-scope interceptors.
2. `TokenService` and `AuthenticatedSessionService` validate the unified token and resolve the caller.
3. `TenantContext` initializes tenant, user, and permission state for the request.
4. `PermissionEvaluator` and the shared permission aspect authorize the resolved operation.
5. Controllers and services execute with shared tenant isolation, Redis keys, storage contracts, exception handling, and operation logging.
6. Interceptors clear request context after completion; operation logs may flow through the single RabbitMQ adapter.

## Module status

| Module | Status |
| --- | --- |
| foundation | COMPLETE |
| permission | PLANNED |
| auth | COMPLETE |
| order | PLANNED |
| approval | PLANNED |
| inventory | COMPLETE |
| quality | COMPLETE |
| installation | COMPLETE |
| customer | COMPLETE |
| document | COMPLETE |
| equipment | COMPLETE |
| label | COMPLETE |
| print | COMPLETE |
| notification | COMPLETE |
| attendance | COMPLETE |
| migration | PLANNED |
| deployment | PLANNED |

## Task 6 domain convergence

Inventory, quality, and installation now use canonical implementations under `my.hive.domain.inventory`, `my.hive.domain.quality`, and `my.hive.domain.installation`, with public adapters under `my.hive.api.inventory`, `my.hive.api.quality`, and `my.hive.api.installation`. Dashboard, notification, approval, and order collaborators import the canonical services instead of the retired `my.management.module.inventory`, `my.management.module.badproduct`, and `my.management.module.installation` packages.

## Task 7 domain convergence

Customer, document, equipment, label-template, receipt-print, and generic print-task code now live under canonical `my.hive.domain.customer`, `my.hive.domain.document`, `my.hive.domain.equipment`, `my.hive.domain.label`, and `my.hive.domain.print` packages. HTTP adapters were moved to `my.hive.api.customer`, `my.hive.api.document`, `my.hive.api.equipment`, `my.hive.api.label`, and `my.hive.api.print`. The previous `my.management.module.customer`, `document`, `equipment`, `label`, and `receipt` runtime packages were retired for these domains; `PrintTaskController` exists only once under the canonical print API.

## Task 8 notification, attendance, and scheduled-work convergence

Notifications and announcements now use the sole domain implementation under `my.hive.domain.notification`; SMS implementations are transport adapters under `my.hive.infrastructure.sms`. Management attendance and mini-program punch behavior share `my.hive.domain.attendance.service.AttendanceService` and one `my.hive.api.attendance.AttendanceController`. The mini-specific duplicate notification and attendance services are retired reference code, not runtime dependencies.

WeChat login and subscription delivery share `my.hive.infrastructure.wechat.WechatMiniProgramClient`. Subscription registration is stored by the authenticated tenant/user and successful one-time delivery changes the subscription state to `used`. Scheduled work is registered only under `my.hive.infrastructure.scheduler`: one XXL-JOB executor configuration, six unique handlers, and one RabbitMQ operation-log listener declaration. `UniqueScheduledWorkTest` prevents duplicate handler names or queue consumers.
## Permission, employee, role, and tenant convergence

Permission Catalog V3 now lives only at `my.hive.shared.permission.PermissionCatalogV3`. Runtime checks validate an exact assignable catalog leaf before consulting the request grants. `EffectivePermissionService` resolves the canonical management employee/role persistence query and discards all non-catalog values. The single authenticated-route initializer is `TenantContextFilter`, backed by the shared authenticated session and tenant context contracts.

Authentication now has three HTTP adapters (`AdminAuthController`, `MiniAuthController`, and `SessionController`) and one stateful domain implementation, `AuthenticationService`. Both credential channels use the canonical employee status, tenant, effective-permission, versioned-token, and response-key pipeline. Logout increments `auth_version`, invalidating previously issued tokens.
