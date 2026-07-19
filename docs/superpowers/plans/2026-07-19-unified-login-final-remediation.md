# Unified Employee Login Final Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the validated security, data-integrity, stable-reason, audit, and mini-program validation gaps before merging unified employee login.

**Architecture:** Public authentication endpoints resolve a trusted normalized client IP centrally and delegate fixed-window atomic Redis limits to a dedicated service. Organization invitations move to an atomic Redis hash contract consumed by both organization issuance and authentication join, while tenant/license reasons and compatibility backfill remain at their owning service/mapper boundaries.

**Tech Stack:** Java 17, Spring Boot, Spring Data Redis/Lua, MyBatis Plus, JUnit 5/Mockito/AssertJ, Node.js built-in test runner, WeChat mini-program JavaScript.

## Global Constraints

- Preserve existing `.superpowers/sdd` edits and do not merge, push, or remotely deploy.
- Never log or persist invitation code, phone proof, WeChat phone code, SMS code, password, plaintext phone, or selection ticket in audit payloads.
- Tests precede production changes and each focused concern records an observed RED and GREEN command.
- Invitation codes default to one use, expire after fifteen minutes, and legacy string values are consumed once.
- Cross-tenant membership creates a distinct target-tenant employee; only a target-tenant duplicate blocks the join and only one tenant-less legacy row may be reused.
- The release audit is read-only and blocks duplicate real-tenant phone hashes while explicitly reporting every other remediation category.
- Refresh the release JAR, metadata, and desktop delivery tree only after all code tests and builds pass; remove any repository-local staged JAR afterward.

---

### Task 1: Trusted client IP and atomic multidimensional public-flow limits

**Files:**
- Create: `management/src/main/java/my/hive/shared/web/TrustedClientIpProperties.java`
- Create: `management/src/main/java/my/hive/shared/web/TrustedClientIpResolver.java`
- Create: `management/src/main/java/my/hive/domain/auth/service/PublicAuthRateLimiter.java`
- Modify: `management/src/main/java/my/hive/api/auth/AdminAuthController.java`
- Modify: `management/src/main/java/my/hive/api/auth/MiniAuthController.java`
- Modify: `management/src/main/java/my/hive/domain/auth/service/AuthenticationService.java`
- Modify: `management/src/main/resources/application.yaml`
- Test: `management/src/test/java/my/hive/shared/web/TrustedClientIpResolverTest.java`
- Test: `management/src/test/java/my/hive/domain/auth/service/PublicAuthRateLimiterTest.java`
- Test: `management/src/test/java/my/hive/api/auth/UnifiedAuthenticationIntegrationTest.java`

**Interfaces:**
- Produces: `String TrustedClientIpResolver.resolve(HttpServletRequest request)`.
- Produces: `void PublicAuthRateLimiter.check(String flow, String dimension, String safeSubject, int limit, Duration window)` backed by one `INCR`/first-hit `EXPIRE` Lua call.
- Controllers pass the resolved IP to password-reset SMS, organization-join SMS, WeChat login, and tenant-selection methods; services add independent IP and phone-hash/account/ticket-fingerprint dimensions.

- [ ] **Step 1: Write failing resolver and limiter tests**

```java
assertThat(resolver.resolve(requestFrom("198.51.100.8", "203.0.113.4")))
        .isEqualTo("198.51.100.8");
assertThat(resolver.resolve(requestFrom("127.0.0.1", "203.0.113.4, 127.0.0.1")))
        .isEqualTo("203.0.113.4");
verify(redis).execute(any(DefaultRedisScript.class), eq(List.of(expectedKey)), eq("300"));
```

- [ ] **Step 2: Run RED**

Run: `cd management && .\mvnw.cmd -q '-Dtest=TrustedClientIpResolverTest,PublicAuthRateLimiterTest,UnifiedAuthenticationIntegrationTest' test`

Expected: FAIL because the resolver/limiter classes and IP-plumbed method signatures do not exist.

- [ ] **Step 3: Implement trusted-proxy normalization and atomic limits**

```java
Long current = redis.execute(RATE_LIMIT_SCRIPT, List.of(key), Long.toString(window.toSeconds()));
if (current != null && current > limit) {
    throw new BusinessException(429, "请求过于频繁，请稍后再试");
}
```

Trust forwarded headers only when the socket peer matches configured CIDRs (loopback by default), walk the chain right-to-left, canonicalize IPv4/IPv6, fingerprint secret tickets/codes, and never log raw subjects.

- [ ] **Step 4: Run GREEN and commit**

Run the Step 2 command; expected PASS. Commit only Task 1 files with `fix: rate limit public authentication flows`.

### Task 2: Atomic structured invitations and audited successful joins

**Files:**
- Create: `management/src/main/java/my/hive/domain/organization/model/OrganizationInvitationPayload.java`
- Create: `management/src/main/java/my/hive/domain/organization/service/OrganizationInvitationService.java`
- Modify: `management/src/main/java/my/hive/domain/organization/service/OrganizationService.java`
- Modify: `management/src/main/java/my/hive/domain/auth/service/AuthenticationService.java`
- Modify: `management/src/main/java/my/hive/shared/log/OperationLogProperties.java`
- Modify: `management/src/main/resources/application-prod.yaml`
- Modify: `management/src/main/resources/application-dev.yaml`
- Test: `management/src/test/java/my/hive/domain/organization/service/OrganizationInvitationServiceTest.java`
- Test: `management/src/test/java/my/hive/architecture/CommercialHardeningStaticTest.java`

**Interfaces:**
- Produces: `OrganizationJoinCodeVO OrganizationInvitationService.issue(String tenantCode, Long issuerUserId)`.
- Produces: `OrganizationInvitationPayload OrganizationInvitationService.consume(String code)` using one Lua command that validates expiry, decrements remaining uses, and deletes at zero; legacy Redis strings are read and deleted atomically.

- [ ] **Step 1: Write failing payload, TTL, legacy, and concurrency tests**

```java
OrganizationInvitationPayload payload = service.consume("JOIN1234");
assertThat(payload.getTenantCode()).isEqualTo("tenant-a");
assertThat(successesFromConcurrentConsume()).isEqualTo(1);
assertThat(capturedIssueScript).contains("HSET", "issuerUserId", "remainingUses", "EXPIRE");
assertThat(capturedConsumeScript).contains("TYPE", "HINCRBY", "DEL");
```

- [ ] **Step 2: Run RED**

Run: `cd management && .\mvnw.cmd -q '-Dtest=OrganizationInvitationServiceTest,CommercialHardeningStaticTest' test`

Expected: FAIL because the structured invitation service is absent and production audit modules are only `order`.

- [ ] **Step 3: Implement atomic invitation issue/consume and audit scope**

```lua
local remaining = tonumber(redis.call('HGET', KEYS[1], 'remainingUses'))
if not remaining or remaining <= 1 then redis.call('DEL', KEYS[1])
else redis.call('HINCRBY', KEYS[1], 'remainingUses', -1) end
```

Set production defaults to `order,auth,organization`; keep `recordArgs=false` and `recordResult=false` for credential and invitation endpoints and add code/proof field names to sensitive-key configuration.

- [ ] **Step 4: Run GREEN and commit**

Run the Step 2 command; expected PASS. Commit Task 2 files with `fix: consume organization invitations atomically`.

### Task 3: Stable tenant reasons and cross-tenant join semantics

**Files:**
- Modify: `management/src/main/java/my/hive/domain/auth/model/AuthReason.java`
- Modify: `management/src/main/java/my/hive/domain/auth/service/AuthenticationService.java`
- Modify: `management/src/main/java/my/hive/domain/tenant/service/TenantLicenseService.java`
- Modify: `management/src/main/java/my/hive/domain/employee/mapper/EmployeeMapper.java`
- Test: `management/src/test/java/my/hive/domain/auth/service/AuthenticationServiceTest.java`
- Create: `management/src/test/java/my/hive/domain/tenant/service/TenantLicenseServiceTest.java`

**Interfaces:**
- Adds `TENANT_SELECTION_INVALID_OR_EXPIRED`.
- `TenantLicenseService.ensureTenantUsable` throws reason-aware `TENANT_UNAVAILABLE` for missing/deleted/disabled tenants and `TENANT_LICENSE_UNAVAILABLE` only for subscription/package/end-date failures.
- `EmployeeMapper.selectOrganizationJoinCandidates(tenantCode, phoneHash, phone)` returns target-tenant and tenant-less rows only.

- [ ] **Step 1: Write failing real service and join tests**

```java
assertThatThrownBy(() -> license.ensureTenantUsable("disabled"))
        .extracting("reason").isEqualTo(AuthReason.TENANT_UNAVAILABLE);
assertThatThrownBy(() -> service.selectWechatTenant(expired))
        .extracting("reason").isEqualTo(AuthReason.TENANT_SELECTION_INVALID_OR_EXPIRED);
assertThat(crossTenantJoin.getTenantCode()).isEqualTo("tenant-b");
```

- [ ] **Step 2: Run RED**

Run: `cd management && .\mvnw.cmd -q '-Dtest=AuthenticationServiceTest,TenantLicenseServiceTest' test`

Expected: FAIL for the absent canonical selection reason, collapsed license reason, and rejection of an employee who belongs to another tenant.

- [ ] **Step 3: Implement typed reasons and target-only duplicate lookup**

Preserve reason-aware `BusinessException` from `TenantLicenseService`; do not remap all failures in `AuthenticationService`. Filter lookup SQL to `(tenant_code = #{tenantCode} OR tenant_code IS NULL OR tenant_code = '')`, reject a target tenant row, allow unrelated tenants, and reuse exactly one tenant-less row.

- [ ] **Step 4: Run GREEN and commit**

Run the Step 2 command; expected PASS. Commit with `fix: preserve tenant reasons across invitation joins`.

### Task 4: Cleartext-safe compatibility backfill and complete release audit

**Files:**
- Modify: `management/src/main/java/my/hive/domain/auth/mapper/AuthMapper.java`
- Modify: `db-migrations/scripts/audit-unified-employee-login.sh`
- Modify: `management-ui/tests/unified-employee-login-migration.test.js`
- Modify: `management/src/test/java/my/hive/domain/auth/service/AuthenticationServiceTest.java`

**Interfaces:**
- Backfill update writes HMAC hash and mask only when the original phone still matches, then clears `user.phone`; authentication continues only after the hash-only requery succeeds.
- Audit reports duplicate real-tenant hashes, tenant-less rows, missing employee extension, missing roles, invalid statuses, and blank hashes for login-capable tenant employees; only duplicates return failure.

- [ ] **Step 1: Write failing SQL/static tests**

```java
assertThat(backfillSql).contains("phone = NULL", "WHERE id = #{userId}", "phone = #{phone}");
assert.match(script, /missing employee extension/i);
assert.doesNotMatch(script, /(?:DELETE|UPDATE|INSERT|ALTER|DROP|TRUNCATE)\b/i);
```

- [ ] **Step 2: Run RED**

Run: `cd management && .\mvnw.cmd -q '-Dtest=AuthenticationServiceTest' test`; then `cd management-ui && npm test -- --test-name-pattern='unified employee login audit'`.

Expected: FAIL because plaintext is retained and four audit categories are absent.

- [ ] **Step 3: Harden guarded backfill and expand read-only audit**

```sql
UPDATE user
SET phone_hash = #{phoneHash}, phone_mask = #{phoneMask}, phone = NULL
WHERE id = #{userId} AND tenant_code = #{tenantCode} AND phone = #{phone};
```

Keep the unique key as the collision/concurrency backstop and leave every non-duplicate category report-only.

- [ ] **Step 4: Run GREEN and commit**

Run the Step 2 commands; expected PASS. Commit with `fix: audit unresolved employee login data`.

### Task 5: Mini-program safe logging and exact join validation

**Files:**
- Modify: `D:/productHiveFrontend/client/pages/joinOrganization/joinOrganization.js`
- Modify: `D:/productHiveFrontend/client/tests/wechat-employee-login-flow.test.js`

**Interfaces:**
- SMS is exactly six digits; passwords are 8-64 characters and contain at least one ASCII letter and digit.
- Join failures log only a constant label plus safe numeric/status reason and never the caught error object or embedded proof ticket.

- [ ] **Step 1: Write failing boundary and leak tests**

```js
assert.equal(await submit({ smsCode: '12345' }), false);
assert.equal(await submit({ password: 'abcdefgh' }), false);
assert.doesNotMatch(JSON.stringify(consoleCalls), /phone-proof-secret/);
```

- [ ] **Step 2: Run RED**

Run: `cd D:\productHiveFrontend\client && node --test tests/wechat-employee-login-flow.test.js`

Expected: FAIL because 4-8 digit SMS and 6-32 character password values are accepted and the error object is logged.

- [ ] **Step 3: Implement validation and safe logging**

```js
if (!/^\d{6}$/.test(smsCode)) return showError('请输入6位短信验证码');
if (password.length < 8 || password.length > 64 || !/[A-Za-z]/.test(password) || !/\d/.test(password)) return showError('密码需为8-64位且包含字母和数字');
console.error('加入组织失败', { code: Number(error?.code || 0), reason: String(error?.reason || '') });
```

- [ ] **Step 4: Run GREEN and commit**

Run the Step 2 command; expected PASS. Commit in the mini repository with `fix: align organization join validation`.

### Task 6: Full verification, report, and fixed-directory release refresh

**Files:**
- Create/modify: `.superpowers/sdd/final-remediation-report.md`
- Refresh only after verification: repository release metadata and `C:/Users/HUAWEI/Desktop/hive全新部署`

- [ ] **Step 1: Run focused suites and all backend/UI/mini tests/builds**

```powershell
cd D:\HiveManager\management; .\mvnw.cmd test; .\mvnw.cmd -DskipTests package
cd D:\HiveManager\management-ui; npm test; npm run build
cd D:\productHiveFrontend\client; npm test
```

- [ ] **Step 2: Refresh and verify release assets**

Use the repository release script/process documented in `AGENTS.md`, verify desktop JAR SHA-256 equals `RELEASE_BUILD_INFO.txt`, verify required tree entries, scan the compiled JAR for old English login strings, and remove any repository-local staged JAR.

- [ ] **Step 3: Record evidence and final status**

Write every RED/GREEN command, focused commit, security decision, test/build output summary, JAR hash, desktop tree check, and any remaining concern to `.superpowers/sdd/final-remediation-report.md`; do not merge or deploy remotely.

## Self-Review

- Spec coverage: design sections 8-10 and review findings map to Tasks 1-5; release requirements map to Task 6.
- Placeholder scan: no deferred implementation placeholders remain.
- Type consistency: invitation issue/consume, trusted IP, public limiter, tenant reasons, and target-tenant candidate lookup are defined once and consumed by later tasks with matching names.
