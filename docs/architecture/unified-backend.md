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
| inventory | PLANNED |
| quality | PLANNED |
| installation | PLANNED |
| customer | PLANNED |
| document | PLANNED |
| equipment | PLANNED |
| print | PLANNED |
| notification | PLANNED |
| attendance | PLANNED |
| migration | PLANNED |
| deployment | PLANNED |
## Permission, employee, role, and tenant convergence

Permission Catalog V3 now lives only at `my.hive.shared.permission.PermissionCatalogV3`. Runtime checks validate an exact assignable catalog leaf before consulting the request grants. `EffectivePermissionService` resolves the canonical management employee/role persistence query and discards all non-catalog values. The single authenticated-route initializer is `TenantContextFilter`, backed by the shared authenticated session and tenant context contracts.

Authentication now has three HTTP adapters (`AdminAuthController`, `MiniAuthController`, and `SessionController`) and one stateful domain implementation, `AuthenticationService`. Both credential channels use the canonical employee status, tenant, effective-permission, versioned-token, and response-key pipeline. Logout increments `auth_version`, invalidating previously issued tokens.
