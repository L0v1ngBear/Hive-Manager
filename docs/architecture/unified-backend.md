# Unified Backend Architecture

## Target

The management application is the convergence shell for one Spring Boot application under `my.hive`, one `/api` context, and one runtime implementation of every shared capability. `D:\HiveBackend\server` and `D:\HiveCommon\hive-backend-common` are immutable source references, not additional runtimes.

`my.hive.HiveApplication` is the only executable entry point and uses the default `my.hive` component-scan boundary. MyBatis scans only `my.hive.domain.**.mapper` and `my.hive.infrastructure.**.mapper`. Shared infrastructure lives in the application artifact under `my.hive.shared`, with RabbitMQ operation-log adapters under `my.hive.infrastructure.messaging`; the external common JAR is no longer a runtime dependency.

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
| permission | COMPLETE |
| auth | COMPLETE |
| order | COMPLETE |
| approval | COMPLETE |
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
| migration | COMPLETE |
| deployment | PACKAGE SYNCED (RELEASE-HOST DOCKER GATE OPEN) |

## Task 6 domain convergence

Inventory, quality, and installation now use canonical implementations under `my.hive.domain.inventory`, `my.hive.domain.quality`, and `my.hive.domain.installation`, with public adapters under `my.hive.api.inventory`, `my.hive.api.quality`, and `my.hive.api.installation`. Dashboard, notification, approval, and order collaborators import the canonical services instead of the retired `my.management.module.inventory`, `my.management.module.badproduct`, and `my.management.module.installation` packages.

## Task 7 domain convergence

Customer, document, equipment, label-template, receipt-print, and generic print-task code now live under canonical `my.hive.domain.customer`, `my.hive.domain.document`, `my.hive.domain.equipment`, `my.hive.domain.label`, and `my.hive.domain.print` packages. HTTP adapters were moved to `my.hive.api.customer`, `my.hive.api.document`, `my.hive.api.equipment`, `my.hive.api.label`, and `my.hive.api.print`. The previous `my.management.module.customer`, `document`, `equipment`, `label`, and `receipt` runtime packages were retired for these domains; `PrintTaskController` exists only once under the canonical print API.

## Task 8 notification, attendance, and scheduled-work convergence

Notifications and announcements now use the sole domain implementation under `my.hive.domain.notification`; SMS implementations are transport adapters under `my.hive.infrastructure.sms`. Management attendance and mini-program punch behavior share `my.hive.domain.attendance.service.AttendanceService` and one `my.hive.api.attendance.AttendanceController`. The mini-specific duplicate notification and attendance services are retired reference code, not runtime dependencies.

WeChat login and subscription delivery share `my.hive.infrastructure.wechat.WechatMiniProgramClient`. Subscription registration is stored by the authenticated tenant/user and successful one-time delivery changes the subscription state to `used`. Scheduled work is registered only under `my.hive.infrastructure.scheduler`: one XXL-JOB executor configuration, six unique handlers, and one RabbitMQ operation-log listener declaration. `UniqueScheduledWorkTest` prevents duplicate handler names or queue consumers.

## Task 9 legacy-root removal

All production and test Java sources now live below `my.hive`; the `my.management` and `my.hive_back` trees are absent. HTTP adapters are grouped under `my.hive.api`, domain services under `my.hive.domain`, shared request/runtime contracts under `my.hive.shared`, and external transports or schedulers under `my.hive.infrastructure`. No compatibility package, forwarding service, or duplicate controller was retained.

`UnifiedBackendSourceGuardTest` enforces one Spring Boot application, no legacy package/import, no `/web` mapping, and no wildcard Permission Catalog V3 constant. Runtime tests additionally resolve the Spring bean and request-mapping registries. The Task 9 clean gate compiled 456 production sources and 45 test sources and passed all 184 tests without duplicate Bean, mapping, scheduled handler, or Rabbit listener failures.

## Task 10 migration convergence

The repository now owns the versioned migration tree at `db-migrations`, with `scripts/migrate-db.sh` as its only top-level migration entry. The imported manifest lists exactly 74 `migrations/V*.sql` files, and `migration_checksums.sha256` protects every imported byte plus explicit critical-history hashes. No schema delta was introduced by backend package convergence, so Task 10 adds no new version file and changes no historical SQL.

The former `management/src/main/resources/sql` tree was unreferenced and duplicated versioned migration responsibilities; all 28 files were removed. Source-contract tests now read the authoritative versioned migration when schema evidence is required.

## Task 11 management-client convergence

The management UI now uses `/api` as its only backend base path in all Vite environments, the Axios fallback, and the development proxy. Administrator authentication is explicitly namespaced below `/api/auth/admin/**`; it remains a distinct authentication adapter while sharing the backend token, tenant, permission, and employee-state pipeline with mini-program authentication.

Management order, approval, quality, and installation clients now call the canonical resource routes (`/api/orders/**`, `/api/approval/**`, `/api/quality/**`, and `/api/installation-tasks/**`). The retired singular and action-suffixed request paths are not client fallbacks. A source contract test prevents `/web` or those retired API roots from returning.

## Task 12 deployment topology convergence

The version-controlled deployment source now lives under `deploy`. Compose contains exactly one Hive business service named `backend` and one container identity `hive-backend`; MySQL, Redis, optional RabbitMQ, optional XXL-JOB admin, and nginx are infrastructure services rather than alternate application runtimes. The backend exposes only port 8080 to the Compose network and owns one log/upload mount, one health check, one executor configuration, and the union of admin and mini-program channel variables.

Nginx has one `/api/**` upstream at `backend:8080` and no compatibility location. Operational start, restart, health, smoke, low-cost, artifact inspection, release-integrity, snapshot, rollback, stop, and log scripts all address the same service. Runtime secrets, certificates, persistent data, generated UI assets, release JARs, reports, and snapshots are excluded from the repository template.

## Task 13 artifact and process identity

The Maven build generates `META-INF/build-info.properties`. `BuildIdentityFilter` adds `X-Hive-Build` (application, version, build time) and a process-lifetime `X-Hive-Instance` UUID to every response, including validation and authentication failures. `/api/health` is the public application liveness route; the identity headers are also exposed through CORS.

`UnifiedEndpointSmokeTest` resolves health, admin/mini authentication, current-user, employee, order, approval, inventory, notification, and print mappings and verifies the same build/instance values across both authentication adapters. The packaged JAR was also started as a Java 21 process and the release smoke script confirmed all ten representative requests reached one build and one process.

## Task 14 release-package convergence

The desktop deployment directory is synchronized from the committed unified source and now contains one `backend/hive-backend.jar`, one backend Dockerfile, one Compose business service, one `/api` nginx upstream, the clean management UI build, the 74-version migration tree, unified operational scripts, four living documents, and release metadata. The retired application directory, old gateway file, old dual-runtime scripts, and obsolete deployment notes were removed only after external snapshots were verified.

Existing MySQL/Redis/nginx container identities, persistent data directories, uploads, certificates, and ACME material were preserved to minimize infrastructure churn. Only the application runtime converges to `hive-backend`. A fresh-data reset is explicit, backup-gated, and never invoked automatically.

## Permission, employee, role, and tenant convergence

Permission Catalog V3 now lives only at `my.hive.shared.permission.PermissionCatalogV3`. Runtime checks validate an exact assignable catalog leaf before consulting the request grants. `EffectivePermissionService` resolves the canonical management employee/role persistence query and discards all non-catalog values. The single authenticated-route initializer is `TenantContextFilter`, backed by the shared authenticated session and tenant context contracts.

Authentication now has three HTTP adapters (`AdminAuthController`, `MiniAuthController`, and `SessionController`) and one stateful domain implementation, `AuthenticationService`. Both credential channels use the canonical employee status, tenant, effective-permission, versioned-token, and response-key pipeline. Logout increments `auth_version`, invalidating previously issued tokens.

## Latest-main integration

Commit `c1d3733` merges the latest `origin/main` Element Plus management UI into the unified-backend line without reintroducing a second backend. The merged client still has one `/api` base, canonical order/approval/quality/installation routes, one V3 employee permission-profile tree, and exact action permissions. Retired UI permissions from the incoming branch were converted to V3 leaves, including customer create, quality process, document list, equipment create/update/disable/export, inventory record list, and print label/receipt actions.
