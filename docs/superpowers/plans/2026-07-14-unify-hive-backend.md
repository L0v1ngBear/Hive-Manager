# Hive Unified Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the management and mini-program Spring Boot applications with one `/api` application, one domain implementation per capability, one executable JAR, one migration entry point, and one Docker business service.

**Architecture:** `D:\HiveManager` is the source of truth. The existing management application is the convergence shell; mini-program-only behavior and the common library source are absorbed domain by domain, then duplicate packages and the external common dependency are removed. HTTP adapters may differ by authentication channel, but both clients use the same domain services, authorization engine, tenant context, persistence, schedulers, consumers, and printing infrastructure.

**Tech Stack:** Java 21, Spring Boot 3.1.8, Maven, MyBatis-Plus 3.5.5, MySQL 8, Redis 7, RabbitMQ, XXL-JOB 2.4.1, Vue 3/Vite, Node test runner, Docker Compose, nginx.

## Global Constraints

- Work only on branch `codex/unify-hive-backend`; do not use a Git worktree.
- Preserve all existing user and collaborator changes; never reset, revert, or overwrite unrelated work.
- All public business routes use `/api/**`; no `/web/**` compatibility route remains.
- Authentication routes are `/api/auth/admin/**` and `/api/auth/mini/**`; token validation, tenant context, permission resolution, and user-state validation are shared.
- Permission Catalog V3 exact permission codes are the only authorization vocabulary; reject legacy codes, aliases, prefixes, and wildcards.
- Each domain has one Controller contract and one Service implementation unless the Controller is an explicitly different authentication or transport adapter.
- Historical versioned SQL is immutable; database changes use new `V*.sql` files and update the manifest.
- Production cutover does not preserve old business data, tokens, caches, or retired API behavior.
- Every task updates `docs/architecture/unified-backend.md`, `docs/api/unified-api-catalog.md`, `docs/migrations/unified-backend-migrations.md`, and `docs/deployment/unified-backend-deployment.md` when its contract changes.
- Every task ends with a focused commit containing only its declared files.

---

## Planned File Structure

```text
D:\HiveManager
├── management
│   ├── pom.xml
│   └── src
│       ├── main/java/my/hive
│       │   ├── HiveApplication.java
│       │   ├── api/<domain>
│       │   ├── domain/<domain>/{model,repository,service}
│       │   ├── infrastructure/{messaging,persistence,printing,scheduler,storage,wechat}
│       │   └── shared/{auth,config,exception,permission,tenant,web}
│       └── test/java/my/hive
├── management-ui
├── db-migrations
├── deploy
│   ├── docker-compose.yml
│   ├── backend/Dockerfile
│   ├── nginx/conf.d/hive.conf
│   └── scripts
├── docs/{api,architecture,deployment,migrations}
└── RELEASE_BUILD_INFO.txt
```

The final Maven build produces `management/target/hive-backend-0.0.1-SNAPSHOT.jar`. `D:\HiveBackend\server` and `D:\HiveCommon\hive-backend-common` remain read-only source references and are not runtime dependencies.

---

### Task 1: Freeze the Baseline and Add Architecture Gates

**Files:**
- Create: `management/src/test/java/my/hive/architecture/UnifiedBackendSourceGuardTest.java`
- Create: `management/src/test/java/my/hive/architecture/UniqueRuntimeComponentTest.java`
- Create: `docs/architecture/unified-backend.md`
- Create: `docs/api/unified-api-catalog.md`
- Create: `docs/migrations/unified-backend-migrations.md`
- Create: `docs/deployment/unified-backend-deployment.md`

**Interfaces:**
- Produces: source and Spring-context gates used by every later task.
- Produces: four living documents with module status tables.

- [ ] **Step 1: Record the immutable source baseline**

Run:

```powershell
git status --short > docs/architecture/unified-backend-baseline-status.txt
git rev-parse HEAD > docs/architecture/unified-backend-baseline-commit.txt
git -C D:\HiveBackend\server rev-parse HEAD > docs/architecture/mini-source-commit.txt
git -C D:\HiveCommon\hive-backend-common rev-parse HEAD > docs/architecture/common-source-commit.txt
```

Expected: four files record the exact merge inputs without modifying either source repository.

- [ ] **Step 2: Write the failing source guard**

```java
@Test
void unifiedSourceMustNotContainLegacyRuntimeRoots() throws IOException {
    String source = Files.walk(Path.of("src/main/java"))
        .filter(Files::isRegularFile)
        .map(Path::toString)
        .collect(Collectors.joining("\n"));
    assertThat(source).doesNotContain("my\\management", "my\\hive_back");
}

@Test
void publicConfigurationMustNotExposeWebContext() throws IOException {
    String yaml = Files.readString(Path.of("src/main/resources/application.yaml"));
    assertThat(yaml).contains("context-path: /api").doesNotContain("context-path: /web");
}
```

- [ ] **Step 3: Run the guard and confirm it fails**

Run: `./mvnw -Dtest=UnifiedBackendSourceGuardTest test`

Expected: FAIL because legacy packages and `/web` still exist.

- [ ] **Step 4: Add the component uniqueness test skeleton**

Create a Spring Boot test that groups bean definitions by bean name and request mappings by HTTP method plus path, then fails on duplicates. Explicitly assert one `PrintTaskController`, one `WebMvcConfigurer`, one XXL-JOB executor configuration, and one operation-log listener.

- [ ] **Step 5: Seed the four living documents**

Each document begins with a module table containing `foundation`, `permission`, `auth`, `order`, `approval`, `inventory`, `quality`, `installation`, `customer`, `document`, `equipment`, `print`, `notification`, `attendance`, `migration`, and `deployment`, initially marked `PLANNED`.

- [ ] **Step 6: Commit the gates and baseline**

```powershell
git add management/src/test/java/my/hive/architecture docs/architecture docs/api docs/migrations docs/deployment
git commit -m "test: establish unified backend architecture gates"
```

---

### Task 2: Create the Single Application and Absorb Shared Infrastructure

**Files:**
- Modify: `management/pom.xml`
- Create: `management/src/main/java/my/hive/HiveApplication.java`
- Create: `management/src/main/java/my/hive/shared/**`
- Create: `management/src/main/java/my/hive/infrastructure/messaging/**`
- Delete after parity: `management/src/main/java/my/management/ManagementApplication.java`
- Modify: `management/src/main/resources/application.yaml`
- Modify: `management/src/main/resources/application-dev.yaml`
- Modify: `management/src/main/resources/application-prod.yaml`
- Test: `management/src/test/java/my/hive/shared/SharedInfrastructureContextTest.java`

**Interfaces:**
- Produces: `my.hive.HiveApplication` and one component-scan root.
- Produces: shared `TokenService`, `AuthenticatedSessionService`, `TenantContext`, `PermissionEvaluator`, exception handler, Redis keys, operation logging, storage contracts, and web configuration.

- [ ] **Step 1: Write a failing application context test**

```java
@SpringBootTest(classes = HiveApplication.class)
class SharedInfrastructureContextTest {
    @Autowired ApplicationContext context;

    @Test void exposesExactlyOneSharedRuntimeStack() {
        assertThat(context.getBeansOfType(TokenService.class)).hasSize(1);
        assertThat(context.getBeansOfType(TenantContext.class)).hasSize(1);
        assertThat(context.getBeansOfType(PermissionEvaluator.class)).hasSize(1);
        assertThat(context.getBeansOfType(WebMvcConfigurer.class)).hasSize(1);
    }
}
```

- [ ] **Step 2: Verify the test fails to compile**

Run: `./mvnw -Dtest=SharedInfrastructureContextTest test`

Expected: FAIL because `my.hive.HiveApplication` and the shared contracts do not exist.

- [ ] **Step 3: Copy common-library source into the target package and reconcile duplicates**

Move behavior from `D:\HiveCommon\hive-backend-common\src\main\java\my\hive\common` into `my.hive.shared` or `my.hive.infrastructure`. For every overlapping management implementation, compare behavior and keep one class. Do not retain wrapper classes whose only purpose is delegating to the old package.

- [ ] **Step 4: Remove the external common dependency**

Delete this dependency from `management/pom.xml`:

```xml
<dependency>
  <groupId>my.hive</groupId>
  <artifactId>hive-backend-common</artifactId>
  <version>0.2.0</version>
</dependency>
```

Set Maven coordinates to `my.hive:hive-backend:0.0.1-SNAPSHOT` and keep one Spring Boot repackage execution.

- [ ] **Step 5: Set the only server prefix and port**

```yaml
server:
  port: 8080
  servlet:
    context-path: /api
```

Replace separate mini/management token secrets and executor ports with one required production configuration.

- [ ] **Step 6: Run foundation tests**

Run: `./mvnw -Dtest=SharedInfrastructureContextTest,UniqueRuntimeComponentTest test`

Expected: PASS with one shared runtime stack and no duplicate infrastructure beans.

- [ ] **Step 7: Update documents and commit**

Mark `foundation` complete and document the shared request lifecycle.

```powershell
git add management/pom.xml management/src management/src/main/resources docs
git commit -m "refactor: establish single Hive application foundation"
```

---

### Task 3: Converge Permission Catalog V3, Employees, Roles, and Tenant Context

**Files:**
- Move/repackage: `management/src/main/java/my/management/module/sys/**`
- Move/repackage: `management/src/main/java/my/management/module/employee/**`
- Move/repackage: `management/src/main/java/my/management/module/tenant/**`
- Create: `management/src/main/java/my/hive/shared/permission/PermissionCatalogV3.java`
- Create: `management/src/test/java/my/hive/shared/permission/ExactPermissionRuntimeTest.java`
- Create: `management/src/test/java/my/hive/architecture/SinglePermissionCatalogTest.java`

**Interfaces:**
- Produces: `Set<String> PermissionCatalogV3.codes()`.
- Produces: `EffectivePermissionService.resolve(long userId, String tenantCode)`.
- Produces: one `TenantContextFilter` used by all authenticated routes.

- [ ] **Step 1: Port existing V3 tests and make legacy acceptance fail**

```java
@ParameterizedTest
@ValueSource(strings = {"*", "order:*", "ORDER_VIEW", "order.view"})
void rejectsNonCatalogPermissionCodes(String code) {
    assertThatThrownBy(() -> permissionEvaluator.require(code))
        .isInstanceOf(IllegalArgumentException.class);
}
```

- [ ] **Step 2: Run permission tests**

Run: `./mvnw -Dtest='*Permission*Test,*Tenant*Test' test`

Expected: FAIL until old enums, aliases, wildcard paths, and duplicated tenant interceptors are removed.

- [ ] **Step 3: Repackage the management V3 implementation as the only catalog**

Delete or replace mini-program `PermissionCodeEnum`, user-role models, and permission fallback logic during import. Keep exact V3 annotations on every protected endpoint.

- [ ] **Step 4: Unify employee, role, and tenant persistence**

Use the management tables and Mapper contracts as canonical. Mini authentication resolves into the same employee/user status and effective-permission pipeline.

- [ ] **Step 5: Run the catalog and context gates**

Run: `./mvnw -Dtest='*Permission*Test,*Tenant*Test,UniqueRuntimeComponentTest' test`

Expected: PASS; source scan finds one permission catalog and no wildcard permission literal.

- [ ] **Step 6: Update documents and commit**

```powershell
git add management/src docs
git commit -m "refactor: unify permission employee role and tenant runtime"
```

---

### Task 4: Implement Shared Authentication with Separate Entry Adapters

**Files:**
- Create: `management/src/main/java/my/hive/api/auth/AdminAuthController.java`
- Create: `management/src/main/java/my/hive/api/auth/MiniAuthController.java`
- Create: `management/src/main/java/my/hive/api/auth/SessionController.java`
- Create: `management/src/main/java/my/hive/domain/auth/service/AuthenticationService.java`
- Create: `management/src/main/java/my/hive/infrastructure/wechat/WechatMiniProgramClient.java`
- Delete after parity: old management and mini `AuthController`/`AuthService` implementations
- Test: `management/src/test/java/my/hive/api/auth/UnifiedAuthenticationIntegrationTest.java`

**Interfaces:**
- `POST /api/auth/admin/login`
- `POST /api/auth/admin/scan-login/session`
- `GET /api/auth/admin/scan-login/status`
- `POST /api/auth/admin/scan-login/confirm`
- `POST /api/auth/mini/login`
- `POST /api/auth/mini/wechat-login`
- `GET /api/auth/me`
- `POST /api/auth/logout`

- [ ] **Step 1: Write MockMvc tests for all authentication routes**

Assert distinct request DTOs enter one mocked `AuthenticationService`, all successful logins return the same token response shape, disabled users are rejected identically, and `/api/auth/me` resolves the same principal model.

- [ ] **Step 2: Run the authentication test**

Run: `./mvnw -Dtest=UnifiedAuthenticationIntegrationTest test`

Expected: FAIL with missing routes and duplicate old `/auth/login` mappings.

- [ ] **Step 3: Implement the three adapters and one service**

The adapters translate channel-specific credentials only. `AuthenticationService` performs tenant resolution, status validation, effective-permission loading, versioned-session creation, token issue, logout, and session invalidation.

- [ ] **Step 4: Remove old public-path declarations and configure one allowlist**

The only unauthenticated business routes are the declared admin and mini login/reset/session-establishment endpoints. `/api/auth/me`, scan confirmation, and logout require authentication as appropriate.

- [ ] **Step 5: Run auth, permission, and context tests**

Run: `./mvnw -Dtest='*Authentication*Test,*Permission*Test,UniqueRuntimeComponentTest' test`

Expected: PASS and no `POST /api/auth/login` mapping.

- [ ] **Step 6: Update documents and commit**

```powershell
git add management/src docs
git commit -m "refactor: unify admin and mini authentication runtime"
```

---

### Task 5: Merge Orders and Approval into Shared Domain Services

**Files:**
- Repackage/converge: management order and approval packages into `my.hive.domain.order` and `my.hive.domain.approval`
- Import unique behavior from: `D:\HiveBackend\server\src\main\java\my\hive_back\module\order`
- Import unique behavior from: `D:\HiveBackend\server\src\main\java\my\hive_back\module\approval`
- Create: `management/src/main/java/my/hive/api/order/OrderController.java`
- Create: `management/src/main/java/my/hive/api/approval/ApprovalController.java`
- Test: `management/src/test/java/my/hive/domain/order/UnifiedOrderServiceTest.java`
- Test: `management/src/test/java/my/hive/domain/approval/UnifiedApprovalServiceTest.java`

**Interfaces:**
- Produces one `OrderService` for management maintenance and mini workflow operations.
- Produces one `ApprovalService` for leave, finance, resignation, order, and quality approval.
- Public collection route is `/api/orders/**`; approval route is `/api/approval/**`.

- [ ] **Step 1: Build a method-level behavior matrix in the API catalog**

For every old management and mini endpoint, record HTTP method, old route, new route, permission code, request type, response type, canonical Service method, and disposition (`MERGED`, `RENAMED`, or `REMOVED`).

- [ ] **Step 2: Write failing shared-Service tests**

Cover information channel propagation, status transitions, rollback restrictions, pending shipment approval, concurrent approval submission, auditor selection, and transaction rollback.

- [ ] **Step 3: Run focused tests**

Run: `./mvnw -Dtest='*Order*Test,*Approval*Test' test`

Expected: FAIL until the mini-only workflow methods use the canonical services.

- [ ] **Step 4: Merge models and Mappers by table contract**

For each duplicate class, compare table, columns, validation, enum values, soft-delete behavior, tenant conditions, and serialization. Retain one type; update all consumers before deleting the duplicate.

- [ ] **Step 5: Implement one Controller per domain and remove duplicates**

Both clients call the same Controller/Service for equal operations. Do not create `AdminOrderService` or `MiniOrderService`.

- [ ] **Step 6: Run domain and mapping gates**

Run: `./mvnw -Dtest='*Order*Test,*Approval*Test,UniqueRuntimeComponentTest' test`

Expected: PASS with one `OrderService`, one `ApprovalService`, and no duplicate request mapping.

- [ ] **Step 7: Update documents and commit**

```powershell
git add management/src docs
git commit -m "refactor: merge order and approval domains"
```

---

### Task 6: Merge Inventory, Quality, and Installation Domains

**Files:**
- Converge into: `management/src/main/java/my/hive/domain/{inventory,quality,installation}`
- Create controllers under: `management/src/main/java/my/hive/api/{inventory,quality,installation}`
- Test: `management/src/test/java/my/hive/domain/inventory/UnifiedInventoryServiceTest.java`
- Test: `management/src/test/java/my/hive/domain/quality/UnifiedQualityServiceTest.java`
- Test: `management/src/test/java/my/hive/domain/installation/UnifiedInstallationServiceTest.java`

**Interfaces:**
- Produces one inventory transaction boundary for in/out/finish/warning/trend operations.
- Produces one quality record and approval flow.
- Produces one installation-task lifecycle used by both clients.

- [ ] **Step 1: Add failing parity and concurrency tests**

Cover cloth in/out, duplicate submission protection, inventory warning invalidation, image recognition validation, quality attachment lifecycle, installation status transitions, and attachment authorization.

- [ ] **Step 2: Run focused tests**

Run: `./mvnw -Dtest='*Inventory*Test,*Quality*Test,*Installation*Test' test`

Expected: FAIL while duplicate services and divergent transaction paths remain.

- [ ] **Step 3: Merge one domain at a time and delete its duplicate implementation**

Complete inventory first, then quality, then installation. After each domain, run its focused test and update the four living documents before proceeding.

- [ ] **Step 4: Verify exact permissions and mapping uniqueness**

Run: `./mvnw -Dtest='*Inventory*Test,*Quality*Test,*Installation*Test,*Permission*Test,UniqueRuntimeComponentTest' test`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add management/src docs
git commit -m "refactor: merge inventory quality and installation domains"
```

---

### Task 7: Merge Customer, Document, Equipment, Label, and Printing

**Files:**
- Converge into: `management/src/main/java/my/hive/domain/{customer,document,equipment,label,print}`
- Create API adapters under: `management/src/main/java/my/hive/api/{customer,document,equipment,label,print}`
- Test: `management/src/test/java/my/hive/architecture/PrintEndpointUniquenessTest.java`
- Test: focused domain tests under corresponding target packages

**Interfaces:**
- Produces one Service per listed domain.
- Produces exactly one `PrintTaskController` and one print-task persistence implementation.

- [ ] **Step 1: Write failing duplicate and behavior tests**

Assert a single print controller bean and unique print mappings. Cover customer custom fields, document move/rename rules, equipment inspection, label default selection, print task claim/report/cancel, and raw-command authorization.

- [ ] **Step 2: Run focused tests**

Run: `./mvnw -Dtest='*Customer*Test,*Document*Test,*Equipment*Test,*Label*Test,*Print*Test' test`

Expected: FAIL until common-library, management, and mini printing paths converge.

- [ ] **Step 3: Merge models, services, and APIs**

Treat `PrintTaskController` collision as a release blocker. Keep one print-task schema and one task state machine.

- [ ] **Step 4: Verify and commit**

Run: `./mvnw -Dtest='*Customer*Test,*Document*Test,*Equipment*Test,*Label*Test,*Print*Test,UniqueRuntimeComponentTest' test`

Expected: PASS.

```powershell
git add management/src docs
git commit -m "refactor: merge customer document equipment and printing domains"
```

---

### Task 8: Merge Notifications, WeChat, Attendance, Statistics, and Maintenance Jobs

**Files:**
- Converge into: `management/src/main/java/my/hive/domain/{notification,attendance}`
- Create channel adapters: `management/src/main/java/my/hive/infrastructure/{wechat,sms}`
- Create scheduler package: `management/src/main/java/my/hive/infrastructure/scheduler`
- Test: `management/src/test/java/my/hive/architecture/UniqueScheduledWorkTest.java`
- Test: `management/src/test/java/my/hive/domain/notification/UnifiedNotificationServiceTest.java`

**Interfaces:**
- Produces one notification service with in-app, SMS, and WeChat channel adapters.
- Produces one XXL-JOB executor configuration and unique handler names.
- Produces one RabbitMQ listener per queue.

- [ ] **Step 1: Write failing scheduler/listener registry tests**

Reflect over `@XxlJob`, `@Scheduled`, and `@RabbitListener`; group by handler or queue declaration and fail on duplicates. Assert the required handler set includes attendance statistics, inventory statistics, notification closed loop, runtime audit, capacity report, and cleanup exactly once.

- [ ] **Step 2: Write notification and attendance behavior tests**

Cover idempotent notification creation, read/close transitions, announcement visibility, WeChat subscription registration/send limits, attendance punch rules, and daily-stat recomputation.

- [ ] **Step 3: Merge implementations and configure one executor**

Use one app name `hive-backend`, one executor port, one log path, and one enable flag. Retain RabbitMQ as an optional transport mode without duplicate consumers.

- [ ] **Step 4: Run and commit**

Run: `./mvnw -Dtest='*Notification*Test,*Attendance*Test,UniqueScheduledWorkTest,UniqueRuntimeComponentTest' test`

Expected: PASS.

```powershell
git add management/src docs
git commit -m "refactor: unify notifications attendance and scheduled work"
```

---

### Task 9: Remove Legacy Package Trees and Prove One Runtime Implementation

**Files:**
- Delete after all imports: `management/src/main/java/my/management/**`
- Ensure absent: any copied `management/src/main/java/my/hive_back/**`
- Update all tests to: `management/src/test/java/my/hive/**`
- Modify: `management/pom.xml`
- Test: `management/src/test/java/my/hive/architecture/UnifiedBackendSourceGuardTest.java`

**Interfaces:**
- Produces a single `my.hive` source root and one Spring Boot main class.

- [x] **Step 1: Run the source guard**

Run: `./mvnw -Dtest=UnifiedBackendSourceGuardTest test`

Expected: FAIL until every old package and reference is removed.

- [x] **Step 2: Remove legacy roots and dead dependencies**

Delete only after imports compile and focused tests pass. Remove dependencies used solely by retired duplicate implementations.

- [x] **Step 3: Add static cardinality assertions**

Assert exactly one `@SpringBootApplication`, no source import starts with `my.management` or `my.hive_back`, no permission constant contains `*`, and no `context-path: /web` exists.

- [x] **Step 4: Run the complete backend suite**

Run: `./mvnw clean test`

Expected: BUILD SUCCESS.

- [x] **Step 5: Update documents and commit**

```powershell
git add management docs
git commit -m "refactor: remove legacy backend implementations"
```

---

### Task 10: Import and Consolidate the Versioned Migration System

**Files:**
- Create from deployment source: `db-migrations/**`
- Create: `scripts/migrate-db.sh`
- Create only if schema changes require it: `db-migrations/migrations/V20260714_002_unified_backend_convergence.sql`
- Modify: `db-migrations/migration_manifest.txt`
- Create: `management-ui/tests/unified-backend-migration.test.js`

**Interfaces:**
- Produces the only migration command: `bash scripts/migrate-db.sh`.
- Produces an immutable manifest and historical checksum gate.

- [x] **Step 1: Copy the deployment migration tree without changing historical bytes**

Use binary-safe copy from `C:\Users\HUAWEI\Desktop\hive部署_全新配置\db-migrations` and `scripts/migrate-db.sh` into the main repository.

- [x] **Step 2: Write migration integrity tests**

The Node test compares every manifest entry with disk files, verifies protected SHA-256 values, rejects modified historical files, and ensures application resource SQL is not invoked as a migration runner.

- [x] **Step 3: Add only a new convergence migration when required**

The new file may update exact V3 permission bindings, unified scheduler executor metadata, or schema constraints discovered during domain convergence. It must not contain compatibility data transforms for retired tokens, caches, or interfaces.

- [x] **Step 4: Run migration verification against a shadow database**

Run:

```bash
node --test management-ui/tests/unified-backend-migration.test.js
bash db-migrations/scripts/verify-schema-only-baseline.sh
```

Expected: both pass and the manifest exactly matches `migrations/V*.sql`.

- [x] **Step 5: Update documents and commit**

```powershell
git add db-migrations scripts management-ui/tests docs
git commit -m "build: consolidate the database migration entry point"
```

---

### Task 11: Switch the Management UI to the Unified API Contract

**Files:**
- Modify: `management-ui/src/utils/request.js`
- Modify: `management-ui/src/api/auth.js`
- Modify: all API modules under `management-ui/src/api` and `management-ui/src/views/**/api`
- Modify: environment examples used by Vite
- Create: `management-ui/tests/unified-api-routes.test.js`

**Interfaces:**
- Consumes: unified API catalog and authentication paths.
- Produces: a management UI build with no `/web` request target.

- [x] **Step 1: Write the failing route-source test**

```javascript
test('management UI uses only the unified API prefix', () => {
  const source = readProjectSources()
  assert.doesNotMatch(source, /['"`]\/web(?:\/|['"`])/)
  assert.match(source, /VITE_API_BASE_URL\s*\|\|\s*['"]\/api['"]/)
  assert.match(source, /\/auth\/admin\/login/)
})
```

- [x] **Step 2: Run the test and confirm failure**

Run: `node --test tests/unified-api-routes.test.js`

Expected: FAIL because `request.js` defaults to `/web`.

- [x] **Step 3: Change the base URL and every renamed endpoint**

Set `baseURL` to `import.meta.env.VITE_API_BASE_URL || '/api'`. Update admin authentication and singular/plural domain path changes from the approved API catalog.

- [x] **Step 4: Run UI tests and build**

Run:

```powershell
npm test
npm run build
```

Expected: all Node tests pass and Vite produces `dist` successfully.

- [x] **Step 5: Update documents and commit**

```powershell
git add management-ui docs
git commit -m "refactor: switch management UI to unified API routes"
```

---

### Task 12: Build the Single-Container Deployment Source

**Files:**
- Create: `deploy/docker-compose.yml`
- Create: `deploy/backend/Dockerfile`
- Create: `deploy/nginx/conf.d/hive.conf`
- Create/modify: `deploy/.env.example`
- Import and adapt: `deploy/scripts/**`
- Create: `management-ui/tests/unified-deployment-topology.test.js`

**Interfaces:**
- Produces one service named `backend` with container name `hive-backend`.
- Produces nginx routing `/api/**` to `backend:8080`.

- [x] **Step 1: Write a failing Compose topology test**

Parse Compose YAML and assert exactly one service builds a Hive business JAR, no service or container name contains `mini-backend` or `management-backend`, and all scripts reference `hive-backend` only.

- [x] **Step 2: Run the test**

Run: `node --test tests/unified-deployment-topology.test.js`

Expected: FAIL against the imported dual-backend topology.

- [x] **Step 3: Create the one-backend Compose service**

The service builds `deploy/backend`, mounts `./logs/backend:/app/logs` and `./uploads:/app/uploads`, exposes only internal port 8080, uses one health check, one XXL-JOB executor configuration, and the union of required channel/maintenance variables without duplicates.

- [x] **Step 4: Rewrite nginx and operational scripts**

Update start, restart, health, smoke, low-cost verification, release-integrity, artifact inspection, snapshot, and rollback scripts. Remove all expectations for two JARs, two log directories, and two business containers.

- [ ] **Step 5: Validate configuration**

Local note (2026-07-15): topology tests, strict YAML parsing with duplicate-key rejection, complete application-variable mapping, retired-reference scans, and Bash syntax checks pass. Docker CLI is unavailable on this workstation, so the real `docker compose ... config` command remains open for the release host.

Run:

```bash
docker compose -f deploy/docker-compose.yml config
node --test management-ui/tests/unified-deployment-topology.test.js
```

Expected: valid Compose and passing topology tests.

- [x] **Step 6: Update documents and commit**

```powershell
git add deploy management-ui/tests docs
git commit -m "build: replace dual backends with one deployment service"
```

---

### Task 13: Build, Start, and Smoke-Test the Unique JAR

**Files:**
- Create: `deploy/scripts/smoke-unified-backend.sh`
- Create: `management/src/test/java/my/hive/api/UnifiedEndpointSmokeTest.java`
- Modify: `docs/api/unified-api-catalog.md`

**Interfaces:**
- Verifies admin and mini routes are served by the same process identity.

- [ ] **Step 1: Add an application-instance header or actuator info value**

Expose a non-secret build identifier from `BuildProperties` so smoke tests can prove admin and mini responses originate from the same artifact.

- [ ] **Step 2: Write endpoint smoke tests**

Cover health, admin login rejection/success fixture, mini login rejection/success fixture, current user, employee query, orders, approval summary, inventory summary, notification list, and print-task query. Assert the same build identifier on both client paths.

- [ ] **Step 3: Build the unique executable JAR**

Run: `./mvnw clean package`

Expected: BUILD SUCCESS and exactly one executable `management/target/hive-backend-0.0.1-SNAPSHOT.jar` excluding `original-*` files.

- [ ] **Step 4: Start the application and run smoke tests**

Run the JAR with an isolated test database/Redis configuration, wait for health, execute `smoke-unified-backend.sh`, then terminate the process cleanly.

Expected: every route responds from one process; logs contain no duplicate mapping, Bean, listener, or handler error.

- [ ] **Step 5: Commit**

```powershell
git add management deploy/scripts docs
git commit -m "test: verify the unified backend artifact and routes"
```

---

### Task 14: Synchronize the Release Package and Document Cutover/Rollback

**Files:**
- Create/modify: `RELEASE_BUILD_INFO.txt`
- Modify: `docs/deployment/unified-backend-deployment.md`
- Modify: `docs/migrations/unified-backend-migrations.md`
- Synchronize to: `C:\Users\HUAWEI\Desktop\hive部署_全新配置`

**Interfaces:**
- Produces the final deployable package with one JAR and reproducible release metadata.

- [ ] **Step 1: Generate release metadata**

Record Git commit, branch, UTC build time, JAR SHA-256, UI SHA-256, migration-manifest SHA-256, Java version, Maven version, and `BackendArtifactCount=1`.

- [ ] **Step 2: Create a release snapshot before replacing deployment files**

Archive the existing dual-backend Compose, both old JARs, nginx configuration, `.env.example`, scripts, migration manifest, and current `RELEASE_BUILD_INFO.txt`. Do not archive secret `.env` into the repository.

- [ ] **Step 3: Synchronize the approved deployment source**

Copy the unique JAR, UI `dist`, Compose, Dockerfile, nginx, migration tree, scripts, docs, and release metadata to the deployment directory. Remove the obsolete `backend`/`management-backend` dual layout only after the snapshot exists and paths are verified to remain within the deployment directory.

- [ ] **Step 4: Execute package integrity checks**

Run:

```bash
docker compose config
bash scripts/check-deploy-health.sh
bash scripts/verify-release-integrity.sh
```

Expected: all checks pass; exactly one Hive business container and one backend JAR are reported.

- [ ] **Step 5: Document release steps**

The deployment document must specify: backup, stop dual backends, clear business data/Redis namespace as approved, run the single migration entry, start the single backend, verify admin login, mini login, orders, approvals, inventory, notification, printing, scheduler registration, and logs.

- [ ] **Step 6: Document rollback steps**

Rollback must stop the unified backend, restore the deployment snapshot, restore the pre-release database backup when a new migration ran, clear the relevant Redis namespace, start the two old services, and execute the old smoke suite. No hand-written down migration is permitted.

- [ ] **Step 7: Final verification and commit**

Run backend tests, UI tests/build, unique JAR build, Compose config, migration verification, deployment health, and unified smoke tests once more from clean outputs.

```powershell
git add RELEASE_BUILD_INFO.txt docs deploy db-migrations scripts
git commit -m "release: package the unified Hive backend"
```

---

## Final Acceptance Checklist

- [ ] `git status --short` contains no accidental or unrelated files.
- [ ] `./mvnw clean test package` succeeds.
- [ ] `npm test` and `npm run build` succeed.
- [ ] One executable backend JAR is produced and recorded in `RELEASE_BUILD_INFO.txt`.
- [ ] One Spring Boot application class and one component-scan root remain.
- [ ] No `my.management`, `my.hive_back`, external common-library dependency, `/web`, wildcard permission, or legacy permission alias remains.
- [ ] One Service implementation exists for every shared domain.
- [ ] One print controller, scheduler handler per name, listener per queue, Web configuration, MyBatis configuration, and XXL-JOB executor exist.
- [ ] The migration manifest matches disk and all protected historical checksums pass.
- [ ] Compose contains one Hive business container named `hive-backend`.
- [ ] Admin and mini authentication plus representative business APIs respond from the same process.
- [ ] Deployment package, scripts, documentation, and `RELEASE_BUILD_INFO.txt` match the built artifacts.
