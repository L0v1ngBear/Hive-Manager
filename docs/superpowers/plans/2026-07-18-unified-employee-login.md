# Hive Unified Employee Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unify management-web and mini-program authentication around enterprise employee records, add controlled first activation, multi-tenant WeChat selection, invitation joining, stable Chinese failure reasons, and remove the obsolete standalone-WeChat-account behavior.

**Architecture:** The `user`/employee record remains the single identity source. Shared result envelopes gain a stable `reason` and optional safe detail data; `AuthenticationService` returns a discriminated WeChat flow and stores short-lived one-time selection/phone-proof tickets in Redis. The web client owns employee provisioning and activation, while the mini program matches verified WeChat phones and uses administrator-issued organization invitations as the only self-join path.

**Tech Stack:** Java 21, Spring Boot 3.1, MyBatis-Plus, MySQL 8, Redis, JUnit 5, Mockito, Vue 3, Element Plus, Node test runner, WeChat Mini Program JavaScript.

## Global Constraints

- The approved design is `docs/superpowers/specs/2026-07-18-unified-employee-login-design.md`.
- Web and mini-program authentication must use the same enterprise employee account; do not create a login-capable tenant-less WeChat account.
- An unknown WeChat phone must not receive a login token or business permissions.
- Customer-visible authentication failures must contain Chinese `msg` text and a stable machine-readable `reason`.
- Frontends must branch on `reason` or `flowStatus`, never on Chinese or English message matching.
- A 401 is reserved for a missing or expired authenticated session; provisioning, disabled, resigned, and tenant availability failures use explicit 403/409 business codes.
- Plain phone numbers, phone codes, SMS codes, tokens, response keys, and Redis ticket payloads must not be logged.
- Phone hashes are unique only inside one non-null tenant; the same natural person may belong to multiple tenants.
- Runtime-owned `.env`, Nginx certificates, MySQL/Redis data, uploads, and backups must never be copied into the local delivery directory.
- The only local release delivery directory is `C:\Users\HUAWEI\Desktop\hive全新部署`; deliver uncompressed files there and do not deploy remotely without explicit user authorization.

## File Structure

### Shared authentication contract

- Modify `management/src/main/java/my/hive/shared/dto/Result.java`: add `reason` and a failure factory that preserves safe detail data.
- Modify `management/src/main/java/my/hive/shared/exception/BusinessException.java`: carry `reason` and safe response data without breaking existing constructors.
- Modify `management/src/main/java/my/hive/shared/exception/GlobalExceptionHandler.java`: serialize the new fields.
- Create `management/src/main/java/my/hive/domain/auth/model/AuthReason.java`: canonical reason constants.

### WeChat login and invite proof

- Create `management/src/main/java/my/hive/domain/auth/model/vo/MiniWechatLoginVO.java`.
- Create `management/src/main/java/my/hive/domain/auth/model/vo/WechatTenantOptionVO.java`.
- Create `management/src/main/java/my/hive/domain/auth/model/dto/WechatTenantSelectRequest.java`.
- Create `management/src/main/java/my/hive/domain/auth/service/WechatTenantSelectionPayload.java`.
- Create `management/src/main/java/my/hive/domain/auth/service/WechatPhoneVerificationPayload.java`.
- Modify `management/src/main/java/my/hive/domain/auth/model/WechatLoginRequest.java`.
- Modify `management/src/main/java/my/hive/domain/auth/model/dto/OrganizationJoinRequest.java`.
- Modify `management/src/main/java/my/hive/domain/auth/mapper/AuthMapper.java`.
- Modify `management/src/main/java/my/hive/domain/auth/service/AuthenticationService.java`.
- Modify `management/src/main/java/my/hive/api/auth/MiniAuthController.java`.

### Employee provisioning and web activation

- Create `management/src/main/java/my/hive/domain/employee/model/vo/EmployeeCreateVO.java`.
- Modify `management/src/main/java/my/hive/domain/employee/service/EmployeeService.java`.
- Modify `management/src/main/java/my/hive/api/employee/EmployeeController.java`.
- Modify `management-ui/src/views/Login.vue`.
- Modify `management-ui/src/views/function/employee/employeeCreate.vue`.

### Mini program

- Modify `client/utils/request.js`: preserve `reason`/detail and allow login pages to own expected-flow messages.
- Modify `client/utils/auth.js`: remove creation of a tenant-less authenticated session.
- Modify `client/pages/login/login.js` and `client/pages/login/login.wxml`: handle logged-in, tenant-selection, and employee-not-found flows.
- Modify `client/pages/joinOrganization/joinOrganization.js`: accept a WeChat phone verification ticket as an alternative to SMS proof.

### Data, docs, and release

- Create `db-migrations/migrations/V20260718_001_unified_employee_login.sql`.
- Create `db-migrations/scripts/audit-unified-employee-login.sh`.
- Modify `db-migrations/migration_manifest.txt` and `db-migrations/migration_checksums.sha256`.
- Modify `management-ui/src/views/manual/UserManual.vue` and stale Java comments.
- Synchronize verified output to `C:\Users\HUAWEI\Desktop\hive全新部署`.

---

### Task 1: Add stable authentication failure reasons

**Files:**
- Create: `management/src/main/java/my/hive/domain/auth/model/AuthReason.java`
- Modify: `management/src/main/java/my/hive/shared/dto/Result.java`
- Modify: `management/src/main/java/my/hive/shared/exception/BusinessException.java`
- Modify: `management/src/main/java/my/hive/shared/exception/GlobalExceptionHandler.java`
- Test: `management/src/test/java/my/hive/shared/exception/GlobalExceptionHandlerTest.java`
- Test: `management/src/test/java/my/hive/domain/auth/service/AuthenticationServiceTest.java`

**Interfaces:**
- Produces: `BusinessException(int code, String reason, String msg)` and `BusinessException(int code, String reason, String msg, Object data)`.
- Produces: `Result.fail(Integer code, String reason, String msg, T data)`.
- Produces: canonical string constants such as `AuthReason.ACCOUNT_DISABLED`.
- Consumed by: Tasks 2-4 and both frontend tasks.

- [ ] **Step 1: Write failing envelope and exception-handler tests**

Add assertions that a reason and safe detail object survive the handler:

```java
@Test
void businessExceptionPreservesReasonAndSafeData() {
    BusinessException exception = new BusinessException(
            403,
            AuthReason.EMPLOYEE_NOT_FOUND,
            "管理员尚未添加该手机号",
            Map.of("phoneVerificationTicket", "ticket-1")
    );

    ResponseEntity<Result<Object>> response = handler.handleBusinessException(exception);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getReason()).isEqualTo(AuthReason.EMPLOYEE_NOT_FOUND);
    assertThat(response.getBody().getMsg()).isEqualTo("管理员尚未添加该手机号");
    assertThat(response.getBody().getData())
            .isEqualTo(Map.of("phoneVerificationTicket", "ticket-1"));
}
```

Also add a compatibility test proving `new BusinessException(400, "中文提示")` still produces `reason == null`.

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```powershell
cd D:\HiveManager\management
& .\mvnw.cmd -q '-Dtest=GlobalExceptionHandlerTest,AuthenticationServiceTest' test
```

Expected: compilation fails because `AuthReason`, the new constructors, and `Result.reason` do not exist.

- [ ] **Step 3: Implement the shared contract**

Create `AuthReason` as a non-instantiable constants class:

```java
package my.hive.domain.auth.model;

public final class AuthReason {
    public static final String EMPLOYEE_NOT_FOUND = "EMPLOYEE_NOT_FOUND";
    public static final String TENANT_SELECTION_REQUIRED = "TENANT_SELECTION_REQUIRED";
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String EMPLOYEE_RESIGNED = "EMPLOYEE_RESIGNED";
    public static final String TENANT_UNAVAILABLE = "TENANT_UNAVAILABLE";
    public static final String TENANT_LICENSE_UNAVAILABLE = "TENANT_LICENSE_UNAVAILABLE";
    public static final String ACCOUNT_ACTIVATION_REQUIRED = "ACCOUNT_ACTIVATION_REQUIRED";
    public static final String PHONE_ACCOUNT_AMBIGUOUS = "PHONE_ACCOUNT_AMBIGUOUS";
    public static final String INVITATION_INVALID_OR_EXPIRED = "INVITATION_INVALID_OR_EXPIRED";

    private AuthReason() {}
}
```

Extend `Result<T>` with `private String reason;` and this factory while preserving existing factories:

```java
public static <T> Result<T> fail(Integer code, String reason, String msg, T data) {
    Result<T> result = new Result<>();
    result.setCode(code);
    result.setReason(reason);
    result.setMsg(msg);
    result.setData(data);
    result.setEncrypted(false);
    return result;
}

public static <T> Result<T> fail(Integer code, String reason, String msg) {
    return fail(code, reason, msg, null);
}
```

Extend `BusinessException` with `String reason` and `Object data`; make old constructors delegate to the new four-argument constructor. Change `handleBusinessException` to return `ResponseEntity<Result<Object>>` and call:

```java
return new ResponseEntity<>(
        Result.fail(e.getCode(), e.getReason(), e.getMsg(), e.getData()),
        HttpStatus.OK
);
```

Keep database-constraint sanitization data-free so database details never leak.

- [ ] **Step 4: Run focused and contract tests**

Run the Step 2 command plus:

```powershell
& .\mvnw.cmd -q '-Dtest=CustomerFacingChineseMessageContractTest' test
```

Expected: all selected tests pass and no pure-English customer exception is reported.

- [ ] **Step 5: Commit Task 1**

```powershell
git add management/src/main/java/my/hive/domain/auth/model/AuthReason.java management/src/main/java/my/hive/shared/dto/Result.java management/src/main/java/my/hive/shared/exception/BusinessException.java management/src/main/java/my/hive/shared/exception/GlobalExceptionHandler.java management/src/test/java/my/hive/shared/exception/GlobalExceptionHandlerTest.java management/src/test/java/my/hive/domain/auth/service/AuthenticationServiceTest.java
git commit -m "feat: add stable authentication failure reasons"
```

### Task 2: Implement single- and multi-tenant WeChat employee login

**Files:**
- Create: `management/src/main/java/my/hive/domain/auth/model/vo/MiniWechatLoginVO.java`
- Create: `management/src/main/java/my/hive/domain/auth/model/vo/WechatTenantOptionVO.java`
- Create: `management/src/main/java/my/hive/domain/auth/model/dto/WechatTenantSelectRequest.java`
- Create: `management/src/main/java/my/hive/domain/auth/service/WechatTenantSelectionPayload.java`
- Modify: `management/src/main/java/my/hive/domain/auth/mapper/AuthMapper.java`
- Modify: `management/src/main/java/my/hive/domain/auth/service/AuthenticationService.java`
- Modify: `management/src/main/java/my/hive/api/auth/MiniAuthController.java`
- Test: `management/src/test/java/my/hive/domain/auth/service/AuthenticationServiceTest.java`
- Test: `management/src/test/java/my/hive/api/auth/UnifiedAuthenticationIntegrationTest.java`

**Interfaces:**
- Produces: `MiniWechatLoginVO AuthenticationService.wechatLogin(WechatLoginRequest)`.
- Produces: `LoginVO AuthenticationService.selectWechatTenant(WechatTenantSelectRequest)`.
- Produces: `POST /auth/mini/wechat-login/select`.
- Consumes: `AuthReason` and reason-aware exceptions from Task 1.

- [ ] **Step 1: Add failing service tests for all three match counts**

Cover:

```java
@Test
void returnsLoggedInFlowForOneEligibleEmployee() {
    when(mapper.selectLoginUsersByPhoneInTenants(PHONE, HASH, null, List.of("a", "b")))
            .thenReturn(List.of(user(1L, "a", 1)));

    MiniWechatLoginVO result = service.wechatLogin(wechatRequest("code"));

    assertThat(result.getFlowStatus()).isEqualTo("LOGGED_IN");
    assertThat(result.getLoginInfo().getTenantCode()).isEqualTo("a");
}

@Test
void returnsOneTimeTenantSelectionForMultipleEmployees() {
    when(mapper.selectLoginUsersByPhoneInTenants(PHONE, HASH, null, List.of("a", "b")))
            .thenReturn(List.of(user(1L, "a", 1), user(2L, "b", 1)));

    MiniWechatLoginVO result = service.wechatLogin(wechatRequest("code"));

    assertThat(result.getFlowStatus()).isEqualTo(AuthReason.TENANT_SELECTION_REQUIRED);
    assertThat(result.getSelectionTicket()).isNotBlank();
    assertThat(result.getTenants()).extracting(WechatTenantOptionVO::getTenantCode)
            .containsExactly("a", "b");
    assertThat(result.getLoginInfo()).isNull();
}
```

Add tests proving the tenant-selection ticket is rejected when expired, tampered, used twice, or submitted with a tenant outside its candidate set.

- [ ] **Step 2: Run service tests and verify RED**

```powershell
cd D:\HiveManager\management
& .\mvnw.cmd -q '-Dtest=AuthenticationServiceTest' test
```

Expected: compilation fails for the new VO, request, and service methods.

- [ ] **Step 3: Add DTOs and Redis payload**

Use these exact fields:

```java
@Data
public class MiniWechatLoginVO {
    private String flowStatus;
    private LoginVO loginInfo;
    private String selectionTicket;
    private List<WechatTenantOptionVO> tenants = new ArrayList<>();
}

@Data
public class WechatTenantOptionVO {
    private String tenantCode;
    private String tenantName;
    private String tenantLogoUrl;
}

@Data
public class WechatTenantSelectRequest {
    @NotBlank(message = "请选择企业")
    private String tenantCode;
    @NotBlank(message = "企业选择凭证不能为空")
    private String selectionTicket;
}

@Data
public class WechatTenantSelectionPayload {
    private String phoneHash;
    private List<String> tenantCodes = new ArrayList<>();
    private Long expireAt;
}
```

- [ ] **Step 4: Add a tenant-specific phone-hash mapper query**

Add to `AuthMapper`:

```java
@Select({
        "SELECT u.id AS userId, u.tenant_code AS tenantCode, COALESCE(t.tenant_name, u.tenant_code) AS tenantName, t.logo_url AS tenantLogoUrl, u.name AS userName, u.login_name AS loginName, ",
        "COALESCE(u.phone_mask, u.phone) AS phone, u.password AS password, COALESCE(u.must_change_password, 0) AS mustChangePassword, u.status AS userStatus, COALESCE(u.permission_version, 1) AS permissionVersion, COALESCE(u.auth_version, 1) AS authVersion ",
        "FROM user u LEFT JOIN tenant t ON t.tenant_code = u.tenant_code AND IFNULL(t.deleted, 0) = 0 ",
        "WHERE u.phone_hash = #{phoneHash} AND u.tenant_code = #{tenantCode} ORDER BY u.id ASC LIMIT 2"
})
List<LoginUserRow> selectLoginUsersByPhoneHashAndTenant(@Param("phoneHash") String phoneHash,
                                                        @Param("tenantCode") String tenantCode);
```

- [ ] **Step 5: Implement the discriminated flow and one-time selection**

Use a random UUID ticket, Redis key namespace `auth:mini-wechat:tenant-selection`, a five-minute TTL, JSON payload with only phone hash/candidate tenant codes/expiry, and `getAndDelete` when selecting. Do not store or log the raw phone.

For zero matches, throw:

```java
throw new BusinessException(
        403,
        AuthReason.EMPLOYEE_NOT_FOUND,
        "管理员尚未添加该手机号，请联系企业负责人或使用组织邀请码加入"
);
```

For one match, validate eligibility and return `flowStatus=LOGGED_IN`. For multiple matches, return `flowStatus=TENANT_SELECTION_REQUIRED` and no login token. The selection endpoint must re-query by phone hash and tenant, require exactly one row, validate eligibility, then call `buildLoginVO`.

- [ ] **Step 6: Update controller and integration tests**

Use:

```java
@PostMapping("/wechat-login")
public Result<MiniWechatLoginVO> wechat(@Valid @RequestBody WechatLoginRequest request) {
    return Result.success(authentication.wechatLogin(request));
}

@PostMapping("/wechat-login/select")
public Result<LoginVO> selectWechatTenant(@Valid @RequestBody WechatTenantSelectRequest request) {
    return Result.success(authentication.selectWechatTenant(request));
}
```

Update `UnifiedAuthenticationIntegrationTest` to mock and assert both routes.

- [ ] **Step 7: Run backend auth tests**

```powershell
& .\mvnw.cmd -q '-Dtest=AuthenticationServiceTest,UnifiedAuthenticationIntegrationTest' test
```

Expected: all selected tests pass.

- [ ] **Step 8: Commit Task 2**

Stage only the Task 2 files and commit:

```powershell
git commit -m "feat: support tenant-aware WeChat employee login"
```

### Task 3: Add controlled WeChat phone proof to organization joining

**Files:**
- Create: `management/src/main/java/my/hive/domain/auth/service/WechatPhoneVerificationPayload.java`
- Modify: `management/src/main/java/my/hive/domain/auth/model/dto/OrganizationJoinRequest.java`
- Modify: `management/src/main/java/my/hive/domain/auth/service/AuthenticationService.java`
- Test: `management/src/test/java/my/hive/domain/auth/service/AuthenticationServiceTest.java`

**Interfaces:**
- Produces: `phoneVerificationTicket` in `BusinessException.data` for `EMPLOYEE_NOT_FOUND`.
- Produces: optional `OrganizationJoinRequest.phoneVerificationTicket` and optional `smsCode`, with service-level exactly-one-proof validation.
- Consumed by: Task 7 mini-program join flow.

- [ ] **Step 1: Write failing phone-proof tests**

Test that an unknown verified WeChat phone returns a five-minute proof ticket, that joining with a matching typed phone consumes it once, and that a different phone, expired ticket, or reused ticket fails with Chinese guidance. Keep the existing SMS join test passing.

```java
assertThatThrownBy(() -> service.wechatLogin(request))
        .isInstanceOfSatisfying(BusinessException.class, ex -> {
            assertThat(ex.getReason()).isEqualTo(AuthReason.EMPLOYEE_NOT_FOUND);
            assertThat(ex.getData()).asInstanceOf(InstanceOfAssertFactories.MAP)
                    .containsKey("phoneVerificationTicket");
        });
```

- [ ] **Step 2: Run the auth test and verify RED**

```powershell
& .\mvnw.cmd -q '-Dtest=AuthenticationServiceTest' test
```

Expected: the ticket assertion fails because no proof ticket exists.

- [ ] **Step 3: Implement one-time phone verification**

Use:

```java
@Data
public class WechatPhoneVerificationPayload {
    private String phoneHash;
    private Long expireAt;
}
```

Store it under `auth:mini-wechat:phone-proof:<ticket>` with a five-minute TTL. Add `phoneVerificationTicket` to `OrganizationJoinRequest`; remove `@NotBlank` from `smsCode` but retain the field. In `joinOrganization`, require either:

```java
if (StringUtils.hasText(request.getPhoneVerificationTicket())) {
    validateAndConsumeWechatPhoneProof(request.getPhoneVerificationTicket(), phoneHash);
} else {
    validateOrganizationJoinSmsCode(phoneHash, request.getSmsCode());
}
```

Consume Redis proof data atomically with `getAndDelete`. Compare only phone hashes.

- [ ] **Step 4: Run focused auth tests**

Run the Step 2 command. Expected: WeChat proof and SMS fallback tests pass.

- [ ] **Step 5: Commit Task 3**

```powershell
git commit -m "feat: verify invitation joins with WeChat phone proof"
```

### Task 4: Make web employee provisioning activation-first

**Files:**
- Create: `management/src/main/java/my/hive/domain/employee/model/vo/EmployeeCreateVO.java`
- Modify: `management/src/main/java/my/hive/domain/employee/service/EmployeeService.java`
- Modify: `management/src/main/java/my/hive/api/employee/EmployeeController.java`
- Create: `management/src/test/java/my/hive/domain/employee/service/EmployeeServiceTest.java`
- Modify: `management/src/test/java/my/hive/api/UnifiedEndpointSmokeTest.java`

**Interfaces:**
- Produces: `EmployeeCreateVO EmployeeService.create(EmployeeCreateRequest)`.
- Produces: `Result<EmployeeCreateVO> POST /emp/employee/create`.
- Keeps: existing password-reset endpoints as the implementation of “首次登录 / 忘记密码”.
- Consumed by: Task 6 employee creation UI.

- [ ] **Step 1: Write failing employee-provisioning tests**

Assert that a created employee uses the generated employee number as `loginName`, stores the phone hash/mask, sets `mustChangePassword=1`, stores a BCrypt credential that does not match the configured shared default password, and returns:

```java
assertThat(result)
        .extracting(EmployeeCreateVO::getEmployeeId,
                    EmployeeCreateVO::getEmpNo,
                    EmployeeCreateVO::getPhoneMask,
                    EmployeeCreateVO::getActivationRequired)
        .containsExactly(100L, "EMP0001", "138****0000", true);
```

Add a duplicate-phone test that returns a Chinese `PHONE_ACCOUNT_AMBIGUOUS` or existing duplicate business error without creating a second row.

- [ ] **Step 2: Run the new test and verify RED**

```powershell
& .\mvnw.cmd -q '-Dtest=EmployeeServiceTest,UnifiedEndpointSmokeTest' test
```

Expected: compilation fails because `EmployeeCreateVO` does not exist and `create` returns `Long`.

- [ ] **Step 3: Implement the response and non-shared initial credential**

Create:

```java
@Data
public class EmployeeCreateVO {
    private Long employeeId;
    private String empNo;
    private String phoneMask;
    private Boolean activationRequired;
}
```

Generate 32 random bytes with `SecureRandom`, encode with `Base64.getUrlEncoder().withoutPadding()`, BCrypt-hash that unshared value, and never return it. Remove `EmployeeService`'s use of `app.default-password.employee`; keep the deployment variable for one compatibility release so existing environment validation does not break again.

Return `EmployeeCreateVO` from service/controller. Import code may ignore the returned object but must still compile and preserve import counts.

- [ ] **Step 4: Run employee and endpoint tests**

Run the Step 2 command. Expected: all selected tests pass.

- [ ] **Step 5: Commit Task 4**

```powershell
git commit -m "feat: provision employees for SMS account activation"
```

### Task 5: Enforce tenant phone uniqueness with a guarded migration

**Files:**
- Create: `db-migrations/migrations/V20260718_001_unified_employee_login.sql`
- Create: `db-migrations/scripts/audit-unified-employee-login.sh`
- Modify: `db-migrations/migration_manifest.txt`
- Modify: `db-migrations/migration_checksums.sha256`
- Create: `management-ui/tests/unified-employee-login-migration.test.js`

**Interfaces:**
- Produces: unique index `uk_user_tenant_phone_hash (tenant_code, phone_hash)`.
- Produces: a read-only audit script whose non-zero exit blocks migration when duplicates exist.
- Consumed by: all later provisioning/login behavior.

- [ ] **Step 1: Write a failing static migration contract test**

The Node test must assert that the new migration is append-only in the manifest, contains a duplicate guard before its `ALTER TABLE`, creates the exact index name, and never deletes or auto-merges users.

```javascript
assert.match(sql, /GROUP BY\s+tenant_code\s*,\s*phone_hash[\s\S]*HAVING COUNT\(\*\) > 1/i)
assert.match(sql, /SIGNAL SQLSTATE '45000'/)
assert.match(sql, /uk_user_tenant_phone_hash/)
assert.doesNotMatch(sql, /DELETE\s+FROM\s+`?user`?/i)
```

- [ ] **Step 2: Run the test and verify RED**

```powershell
cd D:\HiveManager\management-ui
node --test tests\unified-employee-login-migration.test.js
```

Expected: failure because the migration and manifest entry do not exist.

- [ ] **Step 3: Add the read-only audit script**

The script must source the existing database helpers, query and report:

```sql
SELECT tenant_code, phone_hash, COUNT(*) AS duplicate_count
FROM user
WHERE tenant_code IS NOT NULL AND tenant_code <> ''
  AND phone_hash IS NOT NULL AND phone_hash <> ''
GROUP BY tenant_code, phone_hash
HAVING COUNT(*) > 1;

SELECT id, phone_mask, status
FROM user
WHERE tenant_code IS NULL OR tenant_code = '';
```

It exits non-zero only for duplicate tenant phone hashes; tenant-less users are reported for manual reconciliation but are not deleted.

- [ ] **Step 4: Add the guarded migration**

Use a stored procedure that counts duplicates and raises:

```sql
SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Duplicate tenant phone hashes must be resolved before unified employee login migration';
```

Only after the count is zero, add `UNIQUE KEY uk_user_tenant_phone_hash (tenant_code, phone_hash)` if the index does not already exist. Do not edit historical migration files.

- [ ] **Step 5: Append manifest and checksum**

Append exactly:

```text
migrations/V20260718_001_unified_employee_login.sql
```

Calculate the lowercase SHA-256 using PowerShell and append the generated line to `migration_checksums.sha256`:

```powershell
$relative='migrations/V20260718_001_unified_employee_login.sql'
$hash=(Get-FileHash "D:\HiveManager\db-migrations\$relative" -Algorithm SHA256).Hash.ToLowerInvariant()
Add-Content -Encoding UTF8 D:\HiveManager\db-migrations\migration_checksums.sha256 "$hash  $relative"
```

- [ ] **Step 6: Run migration contracts**

```powershell
node --test tests\unified-employee-login-migration.test.js tests\deploy-migration-immutability.test.js tests\deploy-schema-verifier.test.js
```

Expected: all selected Node tests pass.

- [ ] **Step 7: Commit Task 5**

```powershell
git commit -m "feat: guard tenant phone uniqueness migration"
```

### Task 6: Align the management web login and employee-creation UI

**Files:**
- Modify: `management-ui/src/views/Login.vue`
- Modify: `management-ui/src/views/function/employee/employeeCreate.vue`
- Modify: `management-ui/src/views/function/employee/api/employee.js` only if its response unwrapping assumes a numeric ID
- Create: `management-ui/tests/unified-employee-login-ui.test.js`

**Interfaces:**
- Consumes: `EmployeeCreateVO` from Task 4.
- Uses: existing `/auth/admin/password-reset/code` and `/auth/admin/password-reset` endpoints for both first activation and forgotten passwords.

- [ ] **Step 1: Write a failing UI contract test**

Assert that `Login.vue` contains “工号、手机号或登录账号”, does not claim email login, labels the reset flow “首次登录 / 忘记密码”, and that employee creation reads `empNo`, `phoneMask`, and `activationRequired` from the response.

- [ ] **Step 2: Run the contract and verify RED**

```powershell
cd D:\HiveManager\management-ui
node --test tests\unified-employee-login-ui.test.js
```

Expected: failure on the old “员工编号或邮箱” placeholder and missing activation result UI.

- [ ] **Step 3: Update login wording and activation entry**

Change the account placeholder to:

```vue
placeholder="请输入工号、手机号或登录账号"
```

Rename the password-reset entry/dialog to “首次登录 / 忘记密码”. Keep the existing SMS validation and password-setting API calls; after success, prefill the phone in the login form.

- [ ] **Step 4: Show employee activation information once**

Capture the create response and show an Element Plus alert/dialog containing:

```text
员工档案创建成功
登录工号：{empNo}
绑定手机号：{phoneMask}
请员工在登录页选择“首次登录 / 忘记密码”，验证手机号后设置密码。
```

Do not show or generate a shared default password in the UI.

- [ ] **Step 5: Run UI tests and build**

```powershell
node --test tests\unified-employee-login-ui.test.js
npm run build
```

Expected: test and Vite build exit zero.

- [ ] **Step 6: Commit Task 6**

```powershell
git commit -m "feat: align web employee login activation flow"
```

### Task 7: Implement mini-program reason-driven WeChat login

**Files:**
- Modify: `client/utils/request.js`
- Modify: `client/pages/login/login.js`
- Modify: `client/pages/login/login.wxml`
- Modify: `client/pages/joinOrganization/joinOrganization.js`
- Create: `client/tests/wechat-employee-login-flow.test.js`
- Modify: `client/tests/public-login-error.test.js`

**Interfaces:**
- Consumes: `MiniWechatLoginVO`, `POST /auth/mini/wechat-login/select`, `reason`, and optional `phoneVerificationTicket`.
- Produces: explicit mini-program states for logged in, employee not found, and tenant selection.

- [ ] **Step 1: Write failing mini-program flow tests**

Use the existing VM-based test style to prove:

- `LOGGED_IN` calls `afterLogin` exactly once.
- `TENANT_SELECTION_REQUIRED` stores the selection ticket/options and does not save a session.
- `EMPLOYEE_NOT_FOUND` does not redirect to login, preserves `phoneVerificationTicket`, and offers invitation joining.
- `ACCOUNT_DISABLED` and `EMPLOYEE_RESIGNED` display the backend Chinese message once.

- [ ] **Step 2: Run mini tests and verify RED**

```powershell
cd D:\productHiveFrontend\client
node --test tests\wechat-employee-login-flow.test.js tests\public-login-error.test.js
```

Expected: failure because current login code assumes `res.data` is directly a `LoginVO` and has no tenant-selection state.

- [ ] **Step 3: Let expected login flows own their messages**

Add `showBusinessError = true` to request options. When false, `request.js` must still reject with the full result object but skip `showBusinessTip`. Keep public-login 401 behavior unchanged: never clear an unrelated session and never redirect automatically.

Use `showBusinessError: false` only for `/auth/mini/wechat-login` and handle `error.reason` in the login page.

- [ ] **Step 4: Add tenant-selection state and actions**

Add page data:

```javascript
tenantOptions: [],
selectionTicket: '',
selectingTenant: false,
phoneVerificationTicket: ''
```

Handle success:

```javascript
if (data.flowStatus === 'LOGGED_IN') {
  await this.afterLogin(data.loginInfo || {} , subscribeAuthorization)
  return
}
if (data.flowStatus === 'TENANT_SELECTION_REQUIRED') {
  this.setData({
    tenantOptions: data.tenants || [],
    selectionTicket: data.selectionTicket || ''
  })
}
```

Render a modal/card list in `login.wxml`; selecting one calls `/auth/mini/wechat-login/select` with `selectionTicket` and `tenantCode`, then passes its `LoginVO` to `afterLogin`.

- [ ] **Step 5: Handle unknown employees and invite proof**

In the login catch branch, when `error.reason === 'EMPLOYEE_NOT_FOUND'`, retain `error.data.phoneVerificationTicket` and show a modal with “联系企业负责人” and “使用组织邀请码加入”. Pass the ticket as a URL query parameter to the join page. In `joinOrganization.js`, include it in the request and permit submission without SMS only when a ticket is present; the backend remains authoritative.

- [ ] **Step 6: Run focused and existing login tests**

```powershell
node --test tests\wechat-employee-login-flow.test.js tests\public-login-error.test.js tests\auth-login-redirect.test.js tests\request-security.test.js
```

Expected: all selected tests pass.

- [ ] **Step 7: Commit Task 7 in the mini-program repository**

```powershell
git add utils/request.js pages/login/login.js pages/login/login.wxml pages/joinOrganization/joinOrganization.js tests/wechat-employee-login-flow.test.js tests/public-login-error.test.js
git commit -m "feat: add employee-aware WeChat login flow"
```

### Task 8: Remove obsolete standalone-account behavior and documentation

**Files:**
- Modify: `client/utils/auth.js`
- Modify: `client/pages/login/login.js`
- Modify: `client/pages/index/index.js`
- Modify: `client/pages/salesOrder/salesOrder.js`
- Modify: `management/src/main/java/my/hive/domain/employee/service/EmployeeService.java`
- Modify: `management-ui/src/views/manual/UserManual.vue`
- Create: `client/tests/no-standalone-login-state.test.js`
- Create: `management-ui/tests/unified-login-manual.test.js`

**Interfaces:**
- Consumes: all joined sessions now contain nonblank `tenantCode`.
- Produces: no login-capable `needsOrganization` session state.

- [ ] **Step 1: Write failing stale-contract tests**

Assert that source/manual content no longer includes these claims:

```text
新手机号可先登录
微信一键登录会查找或创建用户信息
小程序一键登录会先产生一条没有 emp_employee_ext 档案的 user
```

Assert `saveLoginSession` rejects or clears a login response with no `tenantCode` instead of persisting a token.

- [ ] **Step 2: Run both tests and verify RED**

```powershell
cd D:\productHiveFrontend\client
node --test tests\no-standalone-login-state.test.js
cd D:\HiveManager\management-ui
node --test tests\unified-login-manual.test.js
```

Expected: both fail on current standalone-state branches/text.

- [ ] **Step 3: Remove obsolete authenticated-without-tenant branches**

Change `saveLoginSession` so a missing tenant code does not persist token/response key and returns `false`; successful joined sessions return `true`. Update login callers to require `true`. Remove `needsOrganization` routing from login, index, and sales-order paths while retaining the explicit public invitation page.

- [ ] **Step 4: Update docs and stale Java comments**

Use the canonical statement:

```text
微信手机号用于匹配企业员工账号；未建档用户需由管理员添加，或使用管理员生成的有效组织邀请码加入。
```

Keep historical migration file `V20260506_001_mini_wechat_standalone_login.sql` immutable; do not edit it. Explain the retirement in the new migration and current documentation instead.

- [ ] **Step 5: Run cleanup contracts**

Run the Step 2 commands. Expected: both pass.

- [ ] **Step 6: Commit cleanup in each repository**

Use one focused commit in `D:\HiveManager` and one in `D:\productHiveFrontend\client`, both with message:

```text
refactor: retire standalone mini-program login state
```

### Task 9: Full verification, release metadata, and fixed-directory delivery

**Files:**
- Modify generated release metadata only as required: `RELEASE_BUILD_INFO.txt`
- Update delivery tree: `C:\Users\HUAWEI\Desktop\hive全新部署`

**Interfaces:**
- Consumes: all prior tasks.
- Produces: one verified, uncompressed release tree for the user's existing overwrite-and-publish workflow.

- [ ] **Step 1: Run the complete backend suite and package**

```powershell
cd D:\HiveManager\management
& .\mvnw.cmd -q test
& .\mvnw.cmd -q -DskipTests package
```

Expected: both commands exit zero; Surefire reports zero failures/errors.

- [ ] **Step 2: Run all management UI tests and build**

Use the repository's package scripts:

```powershell
cd D:\HiveManager\management-ui
npm test
npm run build
```

Expected: all tests pass and the production build exits zero.

- [ ] **Step 3: Run all mini-program tests**

```powershell
cd D:\productHiveFrontend\client
node --test tests\*.test.js
```

Expected: zero failures. If unrelated pre-existing static contracts fail, investigate and resolve or explicitly stop; do not declare the release ready with known failures.

- [ ] **Step 4: Verify migrations and customer-visible Chinese messages**

```powershell
cd D:\HiveManager\management-ui
node --test tests\deploy-migration-immutability.test.js tests\deploy-schema-verifier.test.js tests\unified-employee-login-migration.test.js
cd D:\HiveManager\management
& .\mvnw.cmd -q '-Dtest=CustomerFacingChineseMessageContractTest' test
```

Expected: all commands pass.

- [ ] **Step 5: Refresh release metadata**

Copy the packaged JAR to `D:\HiveManager\deploy\backend\hive-backend.jar` only in a temporary/staging release tree, calculate its SHA-256/byte count, recalculate the management UI manifest, migration manifest hashes, counts, and build time, and update `RELEASE_BUILD_INFO.txt`. Never add `.env` or certificates.

Verify metadata/JAR equality:

```powershell
$jar='D:\HiveManager\management\target\hive-backend-0.0.1-SNAPSHOT.jar'
$hash=(Get-FileHash $jar -Algorithm SHA256).Hash.ToLowerInvariant()
$bytes=(Get-Item $jar).Length
"BackendJarSha256=$hash"
"BackendJarBytes=$bytes"
```

- [ ] **Step 6: Synchronize only release-owned files to the fixed desktop directory**

Copy the current `deploy` release files, `db-migrations`, management UI `dist`, verified backend JAR, and `RELEASE_BUILD_INFO.txt` into:

```text
C:\Users\HUAWEI\Desktop\hive全新部署
```

Preserve `交付约定.md`, `docs`, and `SMOKE_TEST.md`. Do not copy `.env`, `nginx/certs`, `mysql/data`, `redis/data`, `uploads`, or `backups`.

- [ ] **Step 7: Verify the delivered tree**

Check required files, forbidden paths, JAR hash equality, exactly one backend JAR, UI manifest file hashes, migration checksums, and compiled authentication constants. The old English literal `Account is disabled or unavailable` must be absent; the new Chinese/reason contract must be present.

- [ ] **Step 8: Report handoff without remote deployment**

Tell the user to overwrite `/root/hive` with the contents of the fixed directory and run:

```bash
cd /root/hive
bash publish.sh
```

Do not upload or execute the command remotely unless the user explicitly authorizes it.

---

## Plan Self-Review

- Spec coverage: identity model, web activation, WeChat zero/one/multiple matches, invitation joining, reason codes, data migration, security, docs, testing, and fixed-directory delivery all map to Tasks 1-9.
- Scope: all tasks contribute to one unified authentication outcome; there is no independent product subsystem that warrants a separate spec.
- Type consistency: `AuthReason`, `MiniWechatLoginVO`, `WechatTenantSelectRequest`, `EmployeeCreateVO`, `selectionTicket`, and `phoneVerificationTicket` use the same names across backend and frontend tasks.
- Migration safety: historical migrations remain immutable; duplicate data blocks the new unique index; no automatic deletion or merge occurs.
- Completeness scan: every implementation step names its files, code shape, command, and expected result.
