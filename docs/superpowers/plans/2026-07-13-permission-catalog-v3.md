# Hive Permission Catalog V3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild Hive tenant permissions into one exact-code catalog with role inheritance, per-user overrides, status-aware order actions, row-level order scopes, immediate session invalidation, and consistent management/mini-program behavior.

**Architecture:** `HiveCommon` owns exact permission evaluation. `HiveManager` owns the catalog, built-in role matrix, employee permission profile, management endpoints, and management UI. `HiveBackend` consumes the same evaluator for mini-program APIs and session checks. The deployment package introduces a new immutable migration and validates that both JARs, management dist, mini-program source, and database catalog come from the same release.

**Tech Stack:** Java 21, Spring Boot 3.1, MyBatis/MyBatis-Plus, MySQL 8, Redis 7, Vue 3, Element Plus, WeChat Mini Program, Maven, Node.js test runner, Docker Compose.

## Global Constraints

- Do not create a git worktree. Use branch `codex/fix-permission-catalog-v3` in each affected repository after the current order branches are integrated and each checkout is clean.
- Do not modify or delete any historical migration. Reserve `V20260713_002` for uninvoiced warnings; use `V20260713_003_permission_catalog_v3.sql` unless `_003` is already committed before integration, in which case use the next unused number and update the manifest in the same commit.
- Enterprise tenant permission matching is exact. Do not support `*`, `*:*`, `module:*`, or implicit order-entry inference.
- Directory/group nodes are never assignable. Only enabled leaves may be written to role or user relation tables.
- A personal `DENY` wins over role grants and personal grants. User overrides must never update role permission rows.
- Order access requires entry permission, current-state action permission, and row-level data scope.
- Platform administrators remain outside the tenant permission tree.
- Disabled or resigned users must fail on the next request; do not wait for the 30-minute permission cache TTL.
- Unauthorized UI entries remain visible but disabled, do not navigate, and do not load business content. Direct routes render the no-permission page. Backend APIs return `403`.
- Write a failing test before each production change. Commit only the files owned by the task; do not include unrelated order work or `.superpowers/` artifacts.

---

### Task 1: Exact Permission Evaluator

**Files:**
- Modify: `D:/HiveCommon/hive-backend-common/src/main/java/my/hive/common/context/TenantPermissionContext.java`
- Modify: `D:/HiveCommon/hive-backend-common/src/test/java/my/hive/common/context/TenantPermissionContextTest.java`
- Test: `D:/HiveCommon/hive-backend-common/src/test/java/my/hive/common/aop/PermissionAspectTest.java`

**Interfaces:**
- Consumes: `TenantPermissionContext.init(String tenantCode, Long userId, Set<String> permCodes)` where denials are represented as an exact code prefixed with `!`, for example `!order:list`.
- Produces: exact-code `TenantPermissionContext.hasPermission(String code)` and unchanged OR semantics for `@RequirePermission({"a", "b"})`.

- [ ] **Step 1: Replace inference expectations with exact-code failing tests**

```java
@Test
void orderStateDoesNotOpenOrderEntry() {
    TenantPermissionContext.init("TENANT-TEST", 1L,
            Set.of("order:status:producing:view"));
    assertFalse(TenantPermissionContext.hasPermission("order:list"));
    assertTrue(TenantPermissionContext.hasPermission("order:status:producing:view"));
}

@Test
void denyWinsOverExactGrant() {
    TenantPermissionContext.init("TENANT-TEST", 1L,
            Set.of("order:list", "!order:list"));
    assertFalse(TenantPermissionContext.hasPermission("order:list"));
}

@Test
void tenantWildcardDoesNotGrantLeafPermission() {
    TenantPermissionContext.init("TENANT-TEST", 1L, Set.of("order:*"));
    assertFalse(TenantPermissionContext.hasPermission("order:list"));
}
```

- [ ] **Step 2: Run the common tests and confirm the old inference fails**

Run: `mvn -f D:/HiveCommon/hive-backend-common/pom.xml -Dtest=TenantPermissionContextTest,PermissionAspectTest test`

Expected: FAIL because the existing matcher accepts wildcards and derives order entry from status permissions.

- [ ] **Step 3: Implement exact matching**

```java
public static boolean hasPermission(String permCode) {
    if (permCode == null || permCode.isBlank()) {
        return false;
    }
    Set<String> codes = getPermCodes();
    String normalized = permCode.trim();
    return !codes.contains("!" + normalized) && codes.contains(normalized);
}
```

Delete `matches`, `hasOrderStatusPermission`, `ORDER_ENTRY_PERMISSIONS`, and wildcard constants. Keep tenant, user, and ignore-tenant context behavior unchanged.

- [ ] **Step 4: Run all common-module tests**

Run: `mvn -f D:/HiveCommon/hive-backend-common/pom.xml test`

Expected: BUILD SUCCESS with no order-entry inference test remaining.

- [ ] **Step 5: Commit**

```powershell
git -C D:/HiveCommon add hive-backend-common/src/main/java/my/hive/common/context/TenantPermissionContext.java hive-backend-common/src/test/java/my/hive/common/context/TenantPermissionContextTest.java hive-backend-common/src/test/java/my/hive/common/aop/PermissionAspectTest.java
git -C D:/HiveCommon commit -m "fix: enforce exact tenant permissions"
```

---

### Task 2: Permission V3 Database Migration

**Files:**
- Create: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/db-migrations/migrations/V20260713_003_permission_catalog_v3.sql`
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/db-migrations/migration_manifest.txt`
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/db-migrations/scripts/verify-online-schema.sh`
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/db-migrations/scripts/rebuild-mysql-from-baseline.sh`
- Create: `D:/HiveManager/management-ui/tests/deploy-permission-catalog-v3.test.js`

**Interfaces:**
- Produces: permission columns `module_code`, `assignable`, `status`; user columns `permission_version`, `auth_version`; table `sys_permission_catalog`; table `order_responsibility`.
- Produces: exact permission codes defined in `docs/superpowers/specs/2026-07-13-permission-catalog-v3-design.md` sections 5 and 7.

- [ ] **Step 1: Write a failing deployment contract test**

```javascript
test('permission v3 migration is immutable and complete', () => {
  const sql = readMigration('V20260713_003_permission_catalog_v3.sql')
  for (const token of [
    'ADD COLUMN `module_code`',
    'ADD COLUMN `assignable`',
    'ADD COLUMN `permission_version`',
    'ADD COLUMN `auth_version`',
    'CREATE TABLE IF NOT EXISTS `sys_permission_catalog`',
    'CREATE TABLE IF NOT EXISTS `order_responsibility`',
    "'order:status:pending-confirm:view'",
    "'order:scope:sales:self'",
    "'employee:permission:manage'"
  ]) assert.match(sql, new RegExp(escapeRegExp(token)))
  assert.doesNotMatch(sql, /VALUES\s*\([^\n]*'order:\*'/)
})
```

- [ ] **Step 2: Run the test and confirm the migration is missing**

Run: `node --test D:/HiveManager/management-ui/tests/deploy-permission-catalog-v3.test.js`

Expected: FAIL because the V3 migration does not exist.

- [ ] **Step 3: Add schema changes and deterministic permission mapping**

The migration must use guarded `information_schema` checks for additive DDL and a temporary mapping table with explicit conflict precedence:

```sql
CREATE TEMPORARY TABLE permission_code_map (
  old_code VARCHAR(128) NOT NULL,
  new_code VARCHAR(128) NOT NULL,
  PRIMARY KEY (old_code, new_code)
);

INSERT INTO permission_code_map (old_code, new_code) VALUES
('dashboard:*', 'dashboard:view'),
('customer:page', 'customer:list'),
('customer:add', 'customer:create'),
('inventory:record:recent', 'inventory:record:list'),
('approval:order:audit', 'order:audit:shipment');

INSERT INTO sys_user_permission
  (tenant_code, user_id, permission_id, effect, create_time, update_time, is_deleted)
SELECT source.tenant_code,
       source.user_id,
       target.id,
       CASE WHEN SUM(source.effect = 'DENY') > 0 THEN 'DENY' ELSE 'GRANT' END,
       NOW(), NOW(), 0
FROM sys_user_permission source
JOIN sys_permission old_perm ON old_perm.id = source.permission_id
JOIN permission_code_map map ON map.old_code = old_perm.perm_code
JOIN sys_permission target ON target.perm_code = map.new_code AND target.is_deleted = 0
WHERE source.is_deleted = 0
GROUP BY source.tenant_code, source.user_id, target.id
ON DUPLICATE KEY UPDATE
  effect = IF(effect = 'DENY' OR VALUES(effect) = 'DENY', 'DENY', 'GRANT'),
  is_deleted = 0,
  update_time = NOW();
```

Insert every V3 leaf with stable `perm_code`, set all group nodes `assignable=0`, rebuild built-in role relations exactly, and soft-delete old wildcards only after validation queries succeed.

- [ ] **Step 4: Add online schema assertions**

`verify-online-schema.sh` must fail when any of these are true:

```sql
SELECT COUNT(*) FROM sys_permission
WHERE is_deleted = 0 AND assignable = 1
  AND (perm_code IN ('*', '*:*') OR perm_code LIKE '%:*');

SELECT COUNT(*) FROM sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
WHERE rp.is_deleted = 0 AND (p.assignable <> 1 OR p.status <> 1 OR p.is_deleted <> 0);

SELECT COUNT(*) FROM sys_user_permission up
LEFT JOIN sys_user u ON u.id = up.user_id AND BINARY u.tenant_code = BINARY up.tenant_code
WHERE up.is_deleted = 0 AND u.id IS NULL;
```

Expected value for each query: `0`.

- [ ] **Step 5: Run migration static tests and manifest verification**

Run: `node --test D:/HiveManager/management-ui/tests/deploy-permission-catalog-v3.test.js D:/HiveManager/management-ui/tests/deploy-migration-immutability.test.js`

Expected: all tests PASS and historical migration checksums remain unchanged.

- [ ] **Step 6: Commit deployment changes in the deployment repository or release branch**

Stage only the new migration, manifest, schema scripts, and the deployment contract test. Commit message: `feat: add permission catalog v3 migration`.

---

### Task 3: Management Permission Catalog and Built-In Roles

**Files:**
- Create: `D:/HiveManager/management/src/main/java/my/management/module/sys/service/PermissionCatalogV3.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/module/sys/service/BuiltInRoleCatalog.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/module/sys/service/BuiltInRoleProvisionService.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/module/sys/model/entity/SysPermission.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/module/sys/model/vo/SysPermissionTreeVO.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/module/sys/model/enums/PermissionCodeEnum.java`
- Test: `D:/HiveManager/management/src/test/java/my/management/module/sys/service/PermissionCatalogV3Test.java`
- Test: `D:/HiveManager/management/src/test/java/my/management/module/sys/service/BuiltInRoleCatalogTest.java`

**Interfaces:**
- Produces: `PermissionCatalogV3.leaves(): Set<String>` and `PermissionCatalogV3.isAssignable(String): boolean`.
- Produces: `BuiltInRoleCatalog.require(String roleCode).permissions()` containing only active leaf codes.

- [ ] **Step 1: Write failing catalog invariants**

```java
@Test
void catalogContainsOnlyExactAssignableLeaves() {
    assertTrue(catalog.leaves().contains("employee:permission:manage"));
    assertTrue(catalog.leaves().contains("order:status:pending-confirm:advance"));
    assertFalse(catalog.leaves().stream().anyMatch(code -> code.endsWith(":*")));
    assertFalse(catalog.isAssignable("order"));
    assertFalse(catalog.isAssignable("order:status:pending-confirm"));
}

@Test
void everyBuiltInRoleUsesCatalogLeaves() {
    builtInRoles.all().forEach(role ->
        assertTrue(catalog.leaves().containsAll(role.permissions()), role.code()));
}
```

- [ ] **Step 2: Run tests and confirm old role wildcards fail**

Run: `mvn -f D:/HiveManager/management/pom.xml -Dtest=PermissionCatalogV3Test,BuiltInRoleCatalogTest test`

Expected: FAIL because roles contain `inventory:*`, `installation:*`, or old status codes.

- [ ] **Step 3: Implement immutable catalog records**

```java
public record PermissionDefinition(
        String code,
        String moduleCode,
        PermissionNodeType type,
        boolean assignable,
        String name,
        int sort) {}

public Set<String> leaves() {
    return definitions.stream()
            .filter(PermissionDefinition::assignable)
            .map(PermissionDefinition::code)
            .collect(Collectors.toUnmodifiableSet());
}
```

Represent every code from design section 5 once. `BuiltInRoleCatalog` composes immutable baseline, staff, and manager sets from those constants.

- [ ] **Step 4: Make provisioning reconcile existing system roles**

Change provisioning from “skip existing role” to: create missing system roles; normalize name and `is_system`; replace permission relations only for retained built-in roles; never modify custom roles.

- [ ] **Step 5: Run management role tests**

Run: `mvn -f D:/HiveManager/management/pom.xml -Dtest=PermissionCatalogV3Test,BuiltInRoleCatalogTest test`

Expected: BUILD SUCCESS; all role permissions are exact V3 leaves.

- [ ] **Step 6: Commit**

Commit message: `feat: define permission catalog v3 roles`.

---

### Task 4: Employee Effective Permission Profile API

**Files:**
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/EmployeeController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/module/employee/service/EmployeeService.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/module/employee/mapper/EmployeeMapper.java`
- Replace: `D:/HiveManager/management/src/main/java/my/management/module/employee/model/dto/EmployeePermissionOverrideRequest.java`
- Replace: `D:/HiveManager/management/src/main/java/my/management/module/employee/model/vo/EmployeePermissionOverrideVO.java`
- Create: `D:/HiveManager/management/src/main/java/my/management/module/employee/model/vo/EmployeePermissionNodeVO.java`
- Create: `D:/HiveManager/management/src/main/java/my/management/module/employee/model/vo/PermissionRoleSourceVO.java`
- Test: `D:/HiveManager/management/src/test/java/my/management/module/employee/service/EmployeePermissionProfileServiceTest.java`
- Test: `D:/HiveManager/management/src/test/java/my/management/controller/EmployeePermissionControllerTest.java`

**Interfaces:**
- Produces: `GET /employee/{userId}/permission-profile`.
- Produces: `PUT /employee/{userId}/permission-overrides` with `{permissionVersion, grants, denies}`.
- Requires caller permission: `employee:permission:manage`.

- [ ] **Step 1: Write failing service tests**

```java
@Test
void denyOverridesRoleWithoutChangingRoleRelations() {
    var before = rolePermissionMapper.selectByRoleId(roleId);
    service.replacePermissionOverrides(userId,
        new EmployeePermissionOverrideRequest(7L, Set.of(), Set.of("order:print")));
    var profile = service.getPermissionProfile(userId);
    assertFalse(profile.node("order:print").effective());
    assertEquals("PERSONAL_DENY", profile.node("order:print").reason());
    assertEquals(before, rolePermissionMapper.selectByRoleId(roleId));
}

@Test
void stalePermissionVersionIsRejected() {
    assertThrows(OptimisticLockException.class, () ->
        service.replacePermissionOverrides(userId,
            new EmployeePermissionOverrideRequest(6L, Set.of("document:file:upload"), Set.of())));
}
```

- [ ] **Step 2: Run tests and confirm the legacy ID-set response fails**

Run: `mvn -f D:/HiveManager/management/pom.xml -Dtest=EmployeePermissionProfileServiceTest,EmployeePermissionControllerTest test`

Expected: FAIL because current APIs return role/grant/deny ID sets and do not check a version.

- [ ] **Step 3: Implement code-based request and profile response**

```java
public record EmployeePermissionOverrideRequest(
        long permissionVersion,
        Set<String> grants,
        Set<String> denies) {}

public record EmployeePermissionNodeVO(
        String code,
        String type,
        boolean assignable,
        boolean enabled,
        List<PermissionRoleSourceVO> roleSources,
        String personalEffect,
        boolean effective,
        String reason,
        List<EmployeePermissionNodeVO> children) {}
```

Validate that grants and denies are disjoint exact leaves. Lock the employee row, compare `permission_version`, replace only that user’s overrides, increment the version, clear both permission caches, and write an employee change log.

- [ ] **Step 4: Add controller authorization and HTTP contracts**

```java
@GetMapping("/{userId}/permission-profile")
@RequirePermission("employee:permission:manage")
public Result<EmployeePermissionOverrideVO> permissionProfile(@PathVariable Long userId) {
    return Result.success(employeeService.permissionProfile(userId));
}

@PutMapping("/{userId}/permission-overrides")
@RequirePermission("employee:permission:manage")
public Result<Void> replaceOverrides(@PathVariable Long userId,
        @Valid @RequestBody EmployeePermissionOverrideRequest request) {
    employeeService.replacePermissionOverrides(userId, request);
    return Result.success(null);
}
```

- [ ] **Step 5: Run employee tests and full management tests**

Run: `mvn -f D:/HiveManager/management/pom.xml -Dtest=EmployeePermissionProfileServiceTest,EmployeePermissionControllerTest test`

Then: `mvn -f D:/HiveManager/management/pom.xml test`

Expected: both commands BUILD SUCCESS.

- [ ] **Step 6: Commit**

Commit message: `feat: expose effective employee permissions`.

---

### Task 5: Unified Employee Permission Tree UI

**Files:**
- Replace: `D:/HiveManager/management-ui/src/views/function/employee/EmployeePermissionDrawer.vue`
- Modify: `D:/HiveManager/management-ui/src/views/function/employee/api/employee.js`
- Test: `D:/HiveManager/management-ui/tests/employee-permission-tree.test.js`

**Interfaces:**
- Consumes: permission profile and code-based override API from Task 4.
- Produces: one tree whose checkboxes represent final effective permissions and whose reset action clears personal overrides.

- [ ] **Step 1: Write failing component contract tests**

```javascript
test('drawer uses one final permission tree', () => {
  const source = read('src/views/function/employee/EmployeePermissionDrawer.vue')
  assert.equal((source.match(/<el-tree/g) || []).length, 1)
  assert.match(source, /恢复角色默认/)
  assert.match(source, /角色来源/)
  assert.match(source, /个人增加|个人禁用/)
  assert.doesNotMatch(source, /追加权限树|禁止权限树/)
})
```

- [ ] **Step 2: Run the test and confirm the three-tree UI fails**

Run: `node --test D:/HiveManager/management-ui/tests/employee-permission-tree.test.js`

Expected: FAIL because the existing drawer renders separate role, grant, and deny controls.

- [ ] **Step 3: Implement deterministic checkbox-to-override conversion**

```javascript
function applyEffectiveToggle(node, checked) {
  if (!node.assignable) return
  if (checked) {
    denyCodes.delete(node.code)
    if (!node.inherited) grantCodes.add(node.code)
  } else {
    grantCodes.delete(node.code)
    if (node.inherited) denyCodes.add(node.code)
  }
}

function restoreRoleDefault(node) {
  for (const leaf of collectAssignableLeaves(node)) {
    grantCodes.delete(leaf.code)
    denyCodes.delete(leaf.code)
  }
}
```

Render role-source badges, personal-effect badges, final check state, parent half-check state, search, “only personal changes,” change counts, and `409` reload behavior.

- [ ] **Step 4: Run UI contract test and build**

Run: `node --test D:/HiveManager/management-ui/tests/employee-permission-tree.test.js`

Then: `npm --prefix D:/HiveManager/management-ui run build`

Expected: test PASS and Vite build succeeds.

- [ ] **Step 5: Commit**

Commit message: `feat: unify employee permission overrides`.

---

### Task 6: Immediate Account and Permission Invalidation

**Files:**
- Modify: `D:/HiveManager/management/src/main/java/my/management/common/interceptor/AuthTokenInterceptor.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/common/utils/PermissionCacheUtil.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/module/auth/mapper/AuthMapper.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/module/employee/service/EmployeeService.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/common/interceptor/TenantInterceptor.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/auth/service/AuthService.java`
- Modify: `D:/HiveBackend/server/src/main/resources/xml/SysUserRoleMapper.xml`
- Test: `D:/HiveManager/management/src/test/java/my/management/common/interceptor/AuthTokenInterceptorTest.java`
- Test: `D:/HiveBackend/server/src/test/java/my/hive_back/common/interceptor/TenantInterceptorTest.java`

**Interfaces:**
- Produces: cached account state `{enabled, permissionVersion, authVersion, catalogVersion}`.
- Requires: Token auth version equals current auth version; cache key contains permission and catalog versions.

- [ ] **Step 1: Write failing old-token and stale-cache tests**

```java
@Test
void disabledUserCannotUsePreviouslyIssuedToken() {
    givenTokenAuthVersion(5L);
    givenCurrentAccountState(false, 9L, 6L, 3L);
    assertThrows(UnauthorizedException.class, () -> interceptor.preHandle(request, response, handler));
}

@Test
void permissionVersionChangesCacheKey() {
    assertNotEquals(cache.key("T1", 10L, 7L, 3L), cache.key("T1", 10L, 8L, 3L));
}
```

- [ ] **Step 2: Run both interceptor test suites and confirm failure**

Run management and mini backend targeted Maven tests. Expected: FAIL because interceptors currently trust the old token and fixed cache key.

- [ ] **Step 3: Implement versioned account-state checks**

Use a five-minute lightweight account-state cache, but invalidate it explicitly on employee status, role, password, and personal-permission changes. Build permission keys as:

```java
String.format("perm-v3:%s:%d:%d:%d", tenantCode, userId, permissionVersion, catalogVersion)
```

On disable, resignation, password reset, or forced logout, increment `auth_version`. On role or override changes, increment `permission_version`. Reject deleted, disabled, or resigned users before initializing `TenantPermissionContext`.

- [ ] **Step 4: Run both backend test suites**

Run: `mvn -f D:/HiveManager/management/pom.xml test`

Run: `mvn -f D:/HiveBackend/server/pom.xml test`

Expected: both BUILD SUCCESS.

- [ ] **Step 5: Commit in each affected repository**

Use commit message `fix: invalidate changed user permissions immediately`.

---

### Task 7: Management Backend Endpoint Permission Matrix

**Files:**
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/ApprovalController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/AttendanceManageController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/BadProductController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/CustomerController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/DocumentController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/EmployeeController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/EquipmentController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/InstallationTaskController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/InventoryController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/LabelTemplateController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/NotificationController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/OrderController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/OrganizationController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/PriceController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/ReceiptPrintController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/RoleController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/TableExportController.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/TenantManualController.java`
- Test: `D:/HiveManager/management/src/test/java/my/management/architecture/PermissionEndpointMatrixTest.java`

**Interfaces:**
- Consumes: exact codes from `PermissionCatalogV3`.
- Produces: every tenant business endpoint declares an exact leaf permission; compound row/state checks remain in service policy.

- [ ] **Step 1: Write a failing reflection-based endpoint matrix test**

```java
@Test
void everyBusinessHandlerHasAnAssignablePermission() {
    endpointScanner.businessHandlers().forEach(handler -> {
        RequirePermission rule = handler.getAnnotation(RequirePermission.class);
        assertNotNull(rule, handler.toString());
        assertTrue(Arrays.stream(rule.value()).allMatch(catalog::isAssignable), handler.toString());
    });
}
```

Add explicit assertions that installation uses `installation:list`/`update`, approval entry uses `approval:list`, auditor settings use `approval:auditor:setting`, and announcement reads use `notification:announcement:list`.

- [ ] **Step 2: Run the matrix test and inventory every failure**

Run: `mvn -f D:/HiveManager/management/pom.xml -Dtest=PermissionEndpointMatrixTest test`

Expected: FAIL on missing annotations, `order:list` write endpoints, installation borrowing order access, and old wildcard codes.

- [ ] **Step 3: Replace endpoint annotations module by module**

Use exact leaf permissions. For example:

```java
@GetMapping("/page")
@RequirePermission("installation:list")

@PutMapping("/{id}")
@RequirePermission("installation:update")

@GetMapping("/auditors")
@RequirePermission("approval:auditor:list")

@PutMapping("/auditors/default")
@RequirePermission("approval:auditor:setting")
```

Do not annotate write endpoints with list/detail permissions. Split combined `save` endpoints only where the current request clearly distinguishes create and update; otherwise the service must choose and check the exact action before writing.

- [ ] **Step 4: Run endpoint matrix and full management tests**

Expected: matrix PASS and full Maven BUILD SUCCESS.

- [ ] **Step 5: Commit**

Commit message: `fix: enforce management permission matrix`.

---

### Task 8: Management UI Global Disabled Permission State

**Files:**
- Modify: `D:/HiveManager/management-ui/src/utils/permission.js`
- Modify: `D:/HiveManager/management-ui/src/directives/permission.js`
- Modify: `D:/HiveManager/management-ui/src/stores/user.js`
- Modify: `D:/HiveManager/management-ui/src/router/index.js`
- Modify: `D:/HiveManager/management-ui/src/layout/components/Sidebar.vue`
- Modify: `D:/HiveManager/management-ui/src/layout/components/Navbar.vue`
- Modify: `D:/HiveManager/management-ui/src/views/NoPermission.vue`
- Modify: `D:/HiveManager/management-ui/src/views/function/announcement/announcement.vue`
- Modify: `D:/HiveManager/management-ui/src/views/function/approval/approvalCenter.vue`
- Modify: `D:/HiveManager/management-ui/src/views/function/attendance/attendanceManagement.vue`
- Modify: `D:/HiveManager/management-ui/src/views/function/badProduct/badProduct.vue`
- Modify: `D:/HiveManager/management-ui/src/views/function/employee/employee.vue`
- Modify: `D:/HiveManager/management-ui/src/views/function/installationTask/installationTask.vue`
- Modify: `D:/HiveManager/management-ui/src/views/function/inventory/inventory.vue`
- Modify: `D:/HiveManager/management-ui/src/views/function/inventory/InventoryModelDetail.vue`
- Modify: `D:/HiveManager/management-ui/src/views/function/order/order.vue`
- Modify: `D:/HiveManager/management-ui/src/views/function/role/role.vue`
- Modify: `D:/HiveManager/management-ui/src/views/function/role/permissionDrawer.vue`
- Test: `D:/HiveManager/management-ui/tests/permission-disabled-state.test.js`

**Interfaces:**
- Consumes: exact effective permission codes from the logged-in user store.
- Produces: `permissionState(code): 'allowed' | 'denied'`, disabled navigation, disabled commands, and no content request on denied pages.

- [ ] **Step 1: Write failing static and behavior contracts**

```javascript
test('legacy permission aliases and wildcards are removed', () => {
  const source = read('src/utils/permission.js')
  assert.doesNotMatch(source, /LEGACY_ORDER_PERMISSION_MAP|order:status:\*/)
})

test('installation and approval routes use independent entry permissions', () => {
  const source = read('src/router/index.js')
  assert.match(source, /installation:list/)
  assert.match(source, /approval:list/)
})
```

- [ ] **Step 2: Run the contract and confirm current OR/alias behavior fails**

Run: `node --test D:/HiveManager/management-ui/tests/permission-disabled-state.test.js`

Expected: FAIL because legacy aliases, order status inference, and borrowed route permissions remain.

- [ ] **Step 3: Implement exact UI permission helpers**

```javascript
export function hasPermission(granted, code) {
  return Array.isArray(granted) && granted.includes(code)
}

export function disabledPermissionProps(granted, code) {
  const allowed = hasPermission(granted, code)
  return { allowed, disabled: !allowed, ariaDisabled: String(!allowed) }
}
```

Sidebar and Navbar entries remain rendered. Denied entries use disabled styling, `aria-disabled="true"`, a permission tooltip, and a click handler that prevents navigation. Route guards render `NoPermission` without mounting the business component. Buttons use the exact action permission and keep content hidden only when entry permission is absent.

- [ ] **Step 4: Run all management UI tests and build**

Run: `node --test D:/HiveManager/management-ui/tests/*.test.js`

Run: `npm --prefix D:/HiveManager/management-ui run build`

Expected: tests PASS and build succeeds without permission alias references.

- [ ] **Step 5: Commit**

Commit message: `feat: show disabled unauthorized actions`.

---

### Task 9: Order Responsibility and State-Action Policy

**Files:**
- Create: `D:/HiveManager/management/src/main/java/my/management/module/order/model/entity/OrderResponsibility.java`
- Create: `D:/HiveManager/management/src/main/java/my/management/module/order/mapper/OrderResponsibilityMapper.java`
- Create: `D:/HiveManager/management/src/main/java/my/management/module/order/service/OrderAccessPolicy.java`
- Modify: `D:/HiveManager/management/src/main/java/my/management/controller/OrderController.java`
- Modify management order services and mappers used by `OrderController`
- Create: `D:/HiveManager/management/src/test/java/my/management/module/order/service/OrderAccessPolicyTest.java`
- Create: `D:/HiveManager/management/src/test/java/my/management/controller/OrderPermissionIntegrationTest.java`

**Interfaces:**
- Produces: `OrderAccessPolicy.requireAccess(OrderRef order, String actionCode)`.
- Produces: `OrderAccessPolicy.applyScope(OrderQuery query)` for list, summary, warning, and export.
- Requires: `order:list`, current `order:status:<status>:<action>`, and at least one matching `order:scope:*` leaf.

- [ ] **Step 1: Write failing scope and action tests**

```java
@Test
void salesStaffCannotReadAnotherSalespersonsOrder() {
    givenPermissions("order:list", "order:status:producing:view", "order:scope:sales:self");
    givenSalesResponsibility(orderId, anotherUserId, salesDepartmentId);
    assertThrows(PermissionDeniedException.class,
        () -> policy.requireAccess(orderRef(orderId, "producing"), "view"));
}

@Test
void viewDoesNotAllowAdvance() {
    givenPermissions("order:list", "order:status:pending-confirm:view", "order:scope:sales:self");
    assertThrows(PermissionDeniedException.class,
        () -> policy.requireAccess(orderRef(orderId, "pending-confirm"), "advance"));
}
```

- [ ] **Step 2: Run targeted tests and confirm current order:list checks fail**

Run: `mvn -f D:/HiveManager/management/pom.xml -Dtest=OrderAccessPolicyTest,OrderPermissionIntegrationTest test`

Expected: FAIL because no responsibility table or compound policy exists.

- [ ] **Step 3: Implement policy and mapper filters**

```java
public void requireAccess(OrderRef order, String action) {
    require("order:list");
    require("order:status:" + order.status() + ":" + action);
    if (!scopeMatcher.matches(order, TenantPermissionContext.getUserId(),
            TenantPermissionContext.getPermCodes())) {
        throw new PermissionDeniedException("当前账号无权访问该订单");
    }
}
```

All list, total, status-summary, warning, export, detail, update, print, advance, rollback, cancel, and audit-candidate paths must call the policy or receive its SQL scope predicate. Create sales responsibility at order creation. Create or replace production responsibility when production is explicitly assigned or first accepted by a production actor.

- [ ] **Step 4: Map every valid state action**

Create only action leaves supported by the state machine. A transition checks the current state’s `:advance`; rollback checks `:rollback`; cancellation checks `:cancel`; reads check `:view`. Shipment and cancellation approvals additionally require `order:audit:shipment` or `order:audit:cancel` and candidate membership.

- [ ] **Step 5: Run targeted and full management tests**

Expected: scope tests PASS and full Maven BUILD SUCCESS.

- [ ] **Step 6: Commit**

Commit message: `feat: enforce order responsibility scopes`.

---

### Task 10: Mini Backend Permission and Order Scope Convergence

**Files:**
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/api/order/OrderController.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/api/approval/ApprovalController.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/api/notification/NotificationController.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/service/OrderService.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/service/SalesOrderService.java`
- Modify: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/service/ProductionOrderService.java`
- Create: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/service/OrderAccessPolicy.java`
- Create: `D:/HiveBackend/server/src/main/java/my/hive_back/module/order/mapper/OrderResponsibilityMapper.java`
- Test: `D:/HiveBackend/server/src/test/java/my/hive_back/order/OrderPermissionScopeTest.java`
- Test: `D:/HiveBackend/server/src/test/java/my/hive_back/module/notification/NotificationPermissionTest.java`

**Interfaces:**
- Consumes: the same database responsibility records and exact permission codes as management backend.
- Produces: identical allow/deny decisions for mini-program list, summary, detail, update, flow, approval, and announcement APIs.

- [ ] **Step 1: Add failing mini-backend permission tests**

Test announcement read without `notification:announcement:list`, order status without entry, entry without state view, cross-owner sales access, cross-assignee production access, and shipment reviewer not in candidate list.

- [ ] **Step 2: Run mini backend tests and confirm failures**

Run: `mvn -f D:/HiveBackend/server/pom.xml -Dtest=OrderPermissionScopeTest,NotificationPermissionTest test`

Expected: FAIL on missing announcement annotation and absent row-scope enforcement.

- [ ] **Step 3: Implement the same compound access contract**

Use exact annotations for entry/action checks and the mini `OrderAccessPolicy` for row/state checks. Do not duplicate a different scope definition; share the table, lane names, permission code constants, and denial semantics from the design.

- [ ] **Step 4: Run mini backend full tests**

Run: `mvn -f D:/HiveBackend/server/pom.xml test`

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

Commit message: `fix: align mini backend permissions`.

---

### Task 11: Mini Program Disabled Entries and Action Gating

**Files:**
- Modify: `D:/productHiveFrontend/client/app.js`
- Create: `D:/productHiveFrontend/client/utils/permission.js`
- Modify: `D:/productHiveFrontend/client/pages/index/index.js`
- Modify: `D:/productHiveFrontend/client/pages/index/index.wxml`
- Modify: `D:/productHiveFrontend/client/pages/salesOrder/salesOrder.js`
- Modify: `D:/productHiveFrontend/client/pages/salesOrder/salesOrder.wxml`
- Modify: `D:/productHiveFrontend/client/pages/orderDetail/orderDetail.js`
- Modify: `D:/productHiveFrontend/client/pages/orderDetail/orderDetail.wxml`
- Modify: `D:/productHiveFrontend/client/pages/approval/approval.js`
- Modify: `D:/productHiveFrontend/client/pages/approval/approval.wxml`
- Modify: `D:/productHiveFrontend/client/pages/announcement/announcement.js`
- Modify: `D:/productHiveFrontend/client/pages/announcement/announcement.wxml`
- Modify: `D:/productHiveFrontend/client/pages/attendance/attendance.js`
- Modify: `D:/productHiveFrontend/client/pages/attendance/attendance.wxml`
- Modify: `D:/productHiveFrontend/client/pages/inventory/inventory.js`
- Modify: `D:/productHiveFrontend/client/pages/inventory/inventory.wxml`
- Modify: `D:/productHiveFrontend/client/pages/badProduct/badProduct.js`
- Modify: `D:/productHiveFrontend/client/pages/badProduct/badProduct.wxml`
- Modify: `D:/productHiveFrontend/client/pages/equipmentInspection/equipmentInspection.js`
- Modify: `D:/productHiveFrontend/client/pages/equipmentInspection/equipmentInspection.wxml`
- Modify: `D:/productHiveFrontend/client/pages/customer/customer.js`
- Modify: `D:/productHiveFrontend/client/pages/customer/customer.wxml`
- Modify: `D:/productHiveFrontend/client/pages/document/document.js`
- Modify: `D:/productHiveFrontend/client/pages/document/document.wxml`
- Modify: `D:/productHiveFrontend/client/pages/labelTemplate/labelTemplate.js`
- Modify: `D:/productHiveFrontend/client/pages/labelTemplate/labelTemplate.wxml`
- Create: `D:/HiveManager/management-ui/tests/mini-permission-disabled-state.test.js`

**Interfaces:**
- Consumes: exact effective permission list from mini authentication response.
- Produces: `can(code)`, disabled feature entries, no denied API requests, and exact order action controls.

- [ ] **Step 1: Assert the mini-program root**

Run:

```powershell
Get-Item D:/productHiveFrontend/client/app.json
Get-Content D:/productHiveFrontend/project.config.json | Select-String 'miniprogramRoot'
```

Expected: `app.json` exists and `project.config.json` resolves the mini-program root to `client/`. The deployment package must copy this same root.

- [ ] **Step 2: Write failing package-level permission contracts**

The test must assert that pages use exact permission codes, do not contain `order:status:*` or legacy sales/production aliases, and call no business request when the page entry is denied.

- [ ] **Step 3: Implement a shared exact permission helper**

```javascript
function can(code, permissions = getApp().globalData.permissions || []) {
  return permissions.indexOf(code) >= 0
}

function denyWithoutRequest(message = '权限不足') {
  wx.showToast({ title: message, icon: 'none' })
  return false
}
```

Render unauthorized entries disabled. Order buttons check the exact current-state action code. Approval tabs and new buttons load only when their entry/submit/audit permissions are present.

- [ ] **Step 4: Validate package structure and source size**

Run existing mini-package static tests, then the WeChat developer-tool CLI compile command configured by the project. Expected: `app.json` found at the project root, lazy loading remains valid, component on-demand injection passes, and the main package stays below 2 MB.

- [ ] **Step 5: Commit**

Commit message: `feat: align mini permission states`.

---

### Task 12: Role and Employee Permission Security Regression Suite

**Files:**
- Create: `D:/HiveManager/management/src/test/java/my/management/security/PermissionV3SecurityTest.java`
- Create: `D:/HiveBackend/server/src/test/java/my/hive_back/security/PermissionV3SecurityTest.java`
- Modify: `D:/HiveManager/management/src/test/java/my/management/architecture/CommercialHardeningStaticTest.java`
- Modify: `D:/HiveManager/management-ui/tests/builtin-role-matrix.test.js`

**Interfaces:**
- Consumes: Tasks 1-11.
- Produces: one executable security contract covering both backends, role matrix, employee overrides, and tenant isolation.

- [ ] **Step 1: Add parameterized least-privilege tests**

```java
@ParameterizedTest
@CsvSource({
  "order:list,order:update,false",
  "installation:list,order:list,false",
  "approval:list,approval:auditor:setting,false",
  "order:status:pending-ship:view,order:status:pending-ship:advance,false"
})
void onePermissionNeverImpliesAnother(String granted, String requested, boolean expected) {
    TenantPermissionContext.init("T1", 1L, Set.of(granted));
    assertEquals(expected, TenantPermissionContext.hasPermission(requested));
}
```

Also assert that every system role uses active leaves, every non-admin role excludes role/employee permission management, and `ADMIN` excludes platform permissions.

- [ ] **Step 2: Run both security suites and UI role contracts**

Expected: all PASS.

- [ ] **Step 3: Commit tests**

Commit message: `test: lock permission v3 boundaries`.

---

### Task 13: Migration Rehearsal, Release Packaging, and Documentation

**Files:**
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/docs/default-role-permission-matrix.md`
- Modify: `D:/HiveManager/docs/architecture/2026-07-13-hive-system-logic-chain-map.md`
- Modify deployment smoke/release scripts that verify JAR, dist, mini-program source, and schema compatibility.
- Create a release verification report under `C:/Users/HUAWEI/Desktop/hive部署_全新配置/smoke-reports/` by running the scripts; do not commit generated runtime logs unless the repository policy already tracks them.

**Interfaces:**
- Consumes: all implementation commits and the immutable V3 migration.
- Produces: one deployable release in which database catalog, management JAR, mini backend JAR, management dist, and mini source agree on exact permission codes.

- [ ] **Step 1: Run local verification**

```powershell
mvn -f D:/HiveCommon/hive-backend-common/pom.xml test
mvn -f D:/HiveManager/management/pom.xml test
mvn -f D:/HiveBackend/server/pom.xml test
node --test D:/HiveManager/management-ui/tests/*.test.js
npm --prefix D:/HiveManager/management-ui run build
```

Expected: every command succeeds.

- [ ] **Step 2: Rehearse an empty database and an upgrade database**

Run the deployment package’s baseline rebuild against an empty MySQL 8 instance. Then restore a pre-V3 snapshot and run versioned migrations. Expected in both databases: no failed migration history, no assignable wildcards, no orphan role/user relations, built-in role checksum match, and permission catalog version exactly `3`.

- [ ] **Step 3: Build and copy release artifacts**

Build both JARs and management dist from their committed permission branches. Copy the actual mini-program project root, excluding developer caches, logs, screenshots, `node_modules`, tests, and source maps not required by WeChat. Record SHA-256 for source artifacts and container `/app/app.jar` targets.

- [ ] **Step 4: Run release gate and online smoke checks**

Expected endpoints:

- management scan-login API returns one of `200,400,401,403`, never `502`;
- mini `/api/auth/me` returns one of `200,400,401,403`, never `502`;
- unauthorized permission probes return `403`;
- disabled user’s old Token returns `401`;
- authorized order list returns only rows allowed by responsibility scope.

- [ ] **Step 5: Update authoritative docs**

Document the exact role matrix, employee override semantics, endpoint permission matrix, order scope rules, migration version, cache version, deployment order, rollback method, and test commands. Update the logic-chain map’s permission and order sections to match the implemented code rather than the pre-change audit.

- [ ] **Step 6: Final commit and review**

Commit message: `docs: finalize permission v3 release contract`. Request a code review focused on privilege escalation, stale sessions, tenant isolation, migration safety, and disabled UI requests before merging.

## Execution Order

Execute Tasks 1-6 sequentially because they establish the catalog and API contract. Tasks 7 and 8 may run in parallel after Task 5. Tasks 9 and 10 may run in parallel after Tasks 1-6 and the current order-flow branches are integrated. Task 11 starts after Task 10. Tasks 12 and 13 are final integration gates.

Do not package or deploy an intermediate state where a frontend uses V3 codes but its backend or database still uses old codes.
