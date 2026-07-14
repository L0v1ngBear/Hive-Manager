# Unified Backend Architecture

## Target

The management application is the convergence shell for one Spring Boot application under `my.hive`, one `/api` context, and one runtime implementation of every shared capability. `D:\HiveBackend\server` and `D:\HiveCommon\hive-backend-common` are immutable source references, not additional runtimes.

Task 1 intentionally installs future-state architecture gates before changing production code. `UnifiedBackendSourceGuardTest` therefore remains red while `my.management` and `/web` exist. `UniqueRuntimeComponentTest` is a compile-safe source gate until Task 2 creates `my.hive.HiveApplication`; Task 2 must upgrade it to inspect Spring bean definitions and `RequestMappingHandlerMapping` from the unified application context.

## Module status

| Module | Status |
| --- | --- |
| foundation | PLANNED |
| permission | PLANNED |
| auth | PLANNED |
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
