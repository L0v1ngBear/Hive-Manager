# Provider-Routed Logistics And Storage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Kuaidi100 with one provider-routed logistics gateway backed by APISpace, and formalize configurable local/OSS file-storage providers without changing current attachment storage behavior.

**Architecture:** Order code calls only `LogisticsTrackingGateway`, which selects a `LogisticsTrackingProvider` using `LOGISTICS_PROVIDER`; APISpace owns all provider-specific HTTP and JSON details. File storage uses the same interface/router pattern with `local` and `aliyun-oss` implementations, while existing attachment consumers remain on local storage until private-download routing is implemented.

**Tech Stack:** Java 21, Spring Boot 3.1, JDK HttpClient, Fastjson2, JUnit 5, Mockito, AssertJ, Node test runner, Docker Compose.

## Global Constraints

- Delete Kuaidi100 code and `KUAIDI100_*` configuration; do not retain fallback or compatibility logic.
- Never write an APISpace Token into Git, frontend assets, tests, logs, release metadata, or deployment examples.
- Keep the management frontend endpoint and hover-only query behavior unchanged.
- Keep successful tracking cache TTL at 30 minutes and failure cooldown at 30 seconds.
- `LOGISTICS_PROVIDER=apispace` is the only supported logistics provider in this release; unknown values fail explicitly.
- `FILE_STORAGE_PROVIDER=local` remains the default; OSS remains disabled until all credentials are supplied.
- Do not silently fall back between external providers.
- Do not touch `.superpowers/sdd/progress.md`.

---

### Task 1: Unified Logistics Gateway

**Files:**
- Create: `management/src/main/java/my/hive/infrastructure/logistics/LogisticsTrackingQuery.java`
- Create: `management/src/main/java/my/hive/infrastructure/logistics/LogisticsTrackingProvider.java`
- Create: `management/src/main/java/my/hive/infrastructure/logistics/LogisticsTrackingGateway.java`
- Test: `management/src/test/java/my/hive/infrastructure/logistics/LogisticsTrackingGatewayTest.java`

**Interfaces:**
- Produces: `record LogisticsTrackingQuery(String companyCode, String trackingNo, String phoneSuffix)`.
- Produces: `LogisticsTrackingProvider.providerCode()` and `query(LogisticsTrackingQuery)`.
- Produces: `LogisticsTrackingGateway.query(LogisticsTrackingQuery)` and `providerCode()`.

- [ ] **Step 1: Write the failing gateway selection tests**

Create two mock providers with codes `apispace` and `stub`. Assert that a gateway configured with `apispace` calls only that provider, trims/lowercases the configured code, exposes `providerCode()`, and rejects `missing` without invoking any provider.

- [ ] **Step 2: Run the gateway test and verify RED**

Run: `management\mvnw.cmd -f management\pom.xml -Dtest=LogisticsTrackingGatewayTest test`

Expected: test compilation fails because the gateway contract does not exist.

- [ ] **Step 3: Implement the minimal gateway contract**

The gateway constructor receives `${logistics.provider:apispace}` and `List<LogisticsTrackingProvider>`, builds an immutable lowercase provider map, rejects duplicate codes, and throws `BusinessException(503, "物流查询供应商未配置或不受支持")` for an unknown code.

- [ ] **Step 4: Run the gateway test and verify GREEN**

Run the command from Step 2. Expected: all gateway tests pass.

- [ ] **Step 5: Commit the gateway**

```bash
git add management/src/main/java/my/hive/infrastructure/logistics management/src/test/java/my/hive/infrastructure/logistics/LogisticsTrackingGatewayTest.java
git commit -m "feat: add logistics provider gateway"
```

### Task 2: APISpace Logistics Provider

**Files:**
- Delete: `management/src/main/java/my/hive/infrastructure/logistics/Kuaidi100Client.java`
- Delete: `management/src/main/java/my/hive/infrastructure/logistics/Kuaidi100Properties.java`
- Delete: `management/src/test/java/my/hive/infrastructure/logistics/Kuaidi100ClientTest.java`
- Create: `management/src/main/java/my/hive/infrastructure/logistics/ApispaceLogisticsProperties.java`
- Create: `management/src/main/java/my/hive/infrastructure/logistics/ApispaceLogisticsTrackingProvider.java`
- Test: `management/src/test/java/my/hive/infrastructure/logistics/ApispaceLogisticsTrackingProviderTest.java`

**Interfaces:**
- Consumes: `LogisticsTrackingProvider` and `LogisticsTrackingQuery` from Task 1.
- Produces: provider code `apispace` and normalized `OrderLogisticsTrackingVO`.

- [ ] **Step 1: Write failing APISpace request and response tests**

Use a mocked `HttpClient`. Capture the request and assert POST to `https://eolink.o.apispace.com/wlgj1/paidtobuy_api/trace_search`, JSON body fields `cpCode`, `mailNo`, `tel`, `orderType=asc`, `Content-Type: application/json`, and `X-APISpace-Token`. Use a success body containing `logisticsTraceDetailList` with out-of-order millisecond timestamps; assert newest-first normalized traces, Beijing timestamps, latest message/time, company, status and tracking number.

- [ ] **Step 2: Write failing configuration and failure tests**

Assert disabled/blank-token configuration returns 503; HTTP 401, 403, 413, 416 and 504 map to sanitized business errors; `success=false`, absent `logisticsTrace`, and invalid JSON never expose raw provider bodies or Token values.

- [ ] **Step 3: Run provider tests and verify RED**

Run: `management\mvnw.cmd -f management\pom.xml -Dtest=ApispaceLogisticsTrackingProviderTest test`

Expected: compilation fails because the APISpace provider does not exist.

- [ ] **Step 4: Implement APISpace properties and provider**

Bind `logistics.apispace.enabled`, `token`, `connect-timeout`, and `request-timeout`. Build requests with JDK `HttpClient`; parse only the documented `success` and `logisticsTrace` structure. Convert epoch milliseconds with `Asia/Shanghai` and `yyyy-MM-dd HH:mm:ss`. Map `SIGN`, `DELIVERING`, `TRANSPORT`, `ACCEPT`, `FAILED` and unknown values to stable Chinese state labels.

- [ ] **Step 5: Run provider and gateway tests**

Run: `management\mvnw.cmd -f management\pom.xml '-Dtest=LogisticsTrackingGatewayTest,ApispaceLogisticsTrackingProviderTest' test`

Expected: all tests pass.

- [ ] **Step 6: Commit the APISpace provider replacement**

```bash
git add management/src/main/java/my/hive/infrastructure/logistics management/src/test/java/my/hive/infrastructure/logistics
git commit -m "feat: replace logistics provider with APISpace"
```

### Task 3: Route Order Tracking Through The Gateway

**Files:**
- Modify: `management/src/main/java/my/hive/domain/order/service/OrderLogisticsTrackingService.java`
- Modify: `management/src/test/java/my/hive/domain/order/service/OrderLogisticsTrackingServiceTest.java`
- Modify: `docs/management-ui/modules/order.md`
- Modify: `docs/architecture/2026-07-13-hive-system-logic-chain-map.md`

**Interfaces:**
- Consumes: `LogisticsTrackingGateway.query(LogisticsTrackingQuery)`.
- Preserves: `GET /orders/{orderId}/shipments/{shipmentId}/logistics-tracking` and `OrderLogisticsTrackingVO`.

- [ ] **Step 1: Rewrite the service tests against the gateway**

Replace Kuaidi100 mocks with a gateway mock. Assert Chinese company names map to APISpace codes (`SF`, `ZTO`, `YTO`, `YUNDA`, `STO`, `EMS`, `JD`, `DBKD`, `JTSD`), direct uppercase codes are accepted, customer phone `13800000000` becomes suffix `0000`, and provider calls receive `LogisticsTrackingQuery`.

- [ ] **Step 2: Add cache namespace and privacy assertions**

Assert cache and call-event provider names use `apispace-logistics`; the cache key remains tenant/order/shipment/company/tracking based; the full phone and full tracking number never appear in call-event metadata.

- [ ] **Step 3: Run order service tests and verify RED**

Run: `management\mvnw.cmd -f management\pom.xml -Dtest=OrderLogisticsTrackingServiceTest test`

Expected: compilation or verification fails because the service still depends on Kuaidi100.

- [ ] **Step 4: Refactor the order service**

Inject only `LogisticsTrackingGateway`; resolve APISpace codes; build a phone suffix from the last four digits when available; keep lock coalescing, 30-minute success cache and 30-second failure cache. Use `gateway.providerCode() + "-logistics"` as the external guard provider namespace.

- [ ] **Step 5: Update current architecture documentation**

Replace current Kuaidi100 cache keys and provider descriptions with the Gateway/APISpace contract. Keep the historical Kuaidi100 implementation plan unchanged as historical evidence.

- [ ] **Step 6: Run focused backend tests**

Run: `management\mvnw.cmd -f management\pom.xml '-Dtest=LogisticsTrackingGatewayTest,ApispaceLogisticsTrackingProviderTest,OrderLogisticsTrackingServiceTest' test`

Expected: all tests pass.

- [ ] **Step 7: Commit the domain integration**

```bash
git add management/src/main/java/my/hive/domain/order/service/OrderLogisticsTrackingService.java management/src/test/java/my/hive/domain/order/service/OrderLogisticsTrackingServiceTest.java docs/management-ui/modules/order.md docs/architecture/2026-07-13-hive-system-logic-chain-map.md
git commit -m "refactor: route order tracking through provider gateway"
```

### Task 4: Configurable File Storage Providers

**Files:**
- Create: `management/src/main/java/my/hive/infrastructure/storage/FileStorageProvider.java`
- Create: `management/src/main/java/my/hive/infrastructure/storage/FileStorageProviderRouter.java`
- Modify: `management/src/main/java/my/hive/infrastructure/storage/LocalFileStorageService.java`
- Modify: `management/src/main/java/my/hive/infrastructure/storage/OssStorageService.java`
- Test: `management/src/test/java/my/hive/infrastructure/storage/FileStorageProviderRouterTest.java`
- Test: `management/src/test/java/my/hive/infrastructure/storage/OssStorageServiceContractTest.java`

**Interfaces:**
- Produces: `FileStorageProvider.providerCode()`, `upload(MultipartFile, String tenantCode, String module)`, and `deleteQuietly(String objectKey)`.
- Produces: router methods with the same upload/delete contract selected by `FILE_STORAGE_PROVIDER`.

- [ ] **Step 1: Write failing storage router tests**

Assert `local` selects only the local child, `aliyun-oss` selects only OSS, codes are normalized, duplicate codes are rejected, and unknown values fail without fallback.

- [ ] **Step 2: Write failing OSS object-key contract test**

Assert an OSS upload object key includes sanitized path prefix, tenant, business module and date. Assert disabled or incomplete OSS configuration fails before constructing a client and no access secret appears in errors.

- [ ] **Step 3: Run storage tests and verify RED**

Run: `management\mvnw.cmd -f management\pom.xml '-Dtest=FileStorageProviderRouterTest,OssStorageServiceContractTest' test`

Expected: compilation fails because the provider contract/router does not exist and OSS upload lacks a module argument.

- [ ] **Step 4: Implement the storage contract and router**

Make `LocalFileStorageService` provider code `local` and `OssStorageService` provider code `aliyun-oss`. Change OSS upload to accept `module` and include it in the object key. Add a router configured by `${storage.provider:local}`. Do not change existing attachment consumers in this release.

- [ ] **Step 5: Run storage tests and existing document tests**

Run: `management\mvnw.cmd -f management\pom.xml '-Dtest=FileStorageProviderRouterTest,OssStorageServiceContractTest,UnifiedDocumentServiceTest' test`

Expected: all tests pass and document uploads remain local.

- [ ] **Step 6: Commit the storage abstraction**

```bash
git add management/src/main/java/my/hive/infrastructure/storage management/src/test/java/my/hive/infrastructure/storage
git commit -m "feat: add configurable file storage providers"
```

### Task 5: Deployment Configuration And Secret Gates

**Files:**
- Modify: `management/src/main/resources/application.yaml`
- Modify: `deploy/.env.example`
- Modify: `deploy/docker-compose.yml`
- Modify: `deploy/scripts/check-deploy-health.sh`
- Modify: `management-ui/tests/deploy-secret-hardening.test.js`
- Modify: `docs/deployment/unified-backend-deployment.md`

**Interfaces:**
- Produces: `LOGISTICS_PROVIDER`, `APISPACE_LOGISTICS_ENABLED`, `APISPACE_LOGISTICS_TOKEN`, timeout variables, and `FILE_STORAGE_PROVIDER`.
- Preserves: all existing `ALIYUN_OSS_*` variables with empty secrets and disabled default.

- [ ] **Step 1: Rewrite deployment contract tests first**

Assert Compose maps all APISpace and provider variables; `.env.example` contains blank Token and no Kuaidi variables; source and deployment files contain no supplied Token; OSS defaults remain `FILE_STORAGE_PROVIDER=local` and `ALIYUN_OSS_ENABLED=false`.

- [ ] **Step 2: Run deployment test and verify RED**

Run: `node --test management-ui/tests/deploy-secret-hardening.test.js`

Expected: failure because current files still contain Kuaidi variables and lack APISpace/provider mappings.

- [ ] **Step 3: Replace runtime and deployment configuration**

Use these defaults:

```text
LOGISTICS_PROVIDER=apispace
APISPACE_LOGISTICS_ENABLED=false
APISPACE_LOGISTICS_TOKEN=
APISPACE_LOGISTICS_CONNECT_TIMEOUT=5s
APISPACE_LOGISTICS_REQUEST_TIMEOUT=10s
FILE_STORAGE_PROVIDER=local
ALIYUN_OSS_ENABLED=false
```

When APISpace is enabled, health checks require a nonblank Token. When `FILE_STORAGE_PROVIDER=aliyun-oss`, health checks require OSS enabled plus endpoint, bucket, access key ID and secret.

- [ ] **Step 4: Run deployment and frontend contract tests**

Run: `node --test management-ui/tests/deploy-secret-hardening.test.js management-ui/tests/order-logistics-tracking.test.js`

Expected: all tests pass; the order query remains hover-only.

- [ ] **Step 5: Commit deployment configuration**

```bash
git add management/src/main/resources/application.yaml deploy/.env.example deploy/docker-compose.yml deploy/scripts/check-deploy-health.sh management-ui/tests/deploy-secret-hardening.test.js docs/deployment/unified-backend-deployment.md
git commit -m "build: configure APISpace and storage providers"
```

### Task 6: Verification And Release Artifact

**Files:**
- Modify: `RELEASE_BUILD_INFO.txt`
- Update artifact: `C:/Users/HUAWEI/Desktop/hive全新部署/backend/hive-backend.jar`
- Update artifact: `C:/Users/HUAWEI/Desktop/hive全新部署/RELEASE_BUILD_INFO.txt`

**Interfaces:**
- Consumes: all tasks above.
- Produces: one verified unified-backend deployment artifact without runtime secrets.

- [ ] **Step 1: Run focused tests**

Run all backend tests named in Tasks 1-5 and the two Node contract tests. Expected: zero failures.

- [ ] **Step 2: Scan retired provider and secrets**

Run a repository-scoped secret scan that flags APISpace credential patterns without embedding any credential values in the command, documentation, or test output.

Expected: no result. Historical plan/spec files may mention the retired provider only as historical context and are excluded from this scan.

- [ ] **Step 3: Build the unified backend**

Run: `management\mvnw.cmd -f management\pom.xml -DskipTests package`

Expected: `BUILD SUCCESS` and one Spring Boot JAR.

- [ ] **Step 4: Refresh release metadata and desktop package**

Copy the JAR to `C:/Users/HUAWEI/Desktop/hive全新部署/backend/hive-backend.jar`. Update only source/package commit, build time, backend SHA-256 and backend bytes; preserve management UI, mini-program and migration hashes.

- [ ] **Step 5: Verify package integrity**

Run from Git Bash:

```bash
cd /c/Users/HUAWEI/Desktop/hive全新部署
SOURCE_REPOSITORY_ROOT=/d/HiveManager bash scripts/verify-release-integrity.sh
bash scripts/verify-upload-package.sh
```

Expected: unified backend inspection, release integrity and upload cleanliness all pass.

- [ ] **Step 6: Commit metadata and push main**

```bash
git add RELEASE_BUILD_INFO.txt
git commit -m "build: refresh APISpace logistics release"
git push origin main
```
