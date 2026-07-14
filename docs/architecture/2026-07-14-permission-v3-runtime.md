# Hive Permission V3 Runtime

## Scope

This document is the current source of truth for permission evaluation, session invalidation, and release ordering. The production cutover uses a fresh database. No permission-ID API, wildcard permission, sales/production order alias, or pre-V3 cache compatibility is retained.

## Runtime Contract

- Both backends use `hive-backend-common:0.2.0`.
- `TenantPermissionContext` evaluates exact permission codes only. A personal `!code` denies the same exact code.
- Only enabled, assignable permission leaves (`status = 1`, `assignable = 1`) enter a user permission set.
- Order entry permissions and order status permissions are independent. A status permission does not imply `order:list` or `order:detail`.
- Approver lookup uses the exact audit permission and applies personal DENY after role and personal GRANT sources are combined.
- Management UI route, menu, button, and action checks use exact codes only.

## Session And Cache Versions

The `user` table owns two independent versions:

| Change | `permission_version` | `auth_version` |
| --- | ---: | ---: |
| Personal permission override | +1 | unchanged |
| User role assignment | +1 | unchanged |
| Role permission update | +1 for every bound user | unchanged |
| Password reset or password change | unchanged | +1 |
| Disable, resignation, or account deletion | +1 when roles are revoked | +1 |
| Transparent BCrypt hash upgrade | unchanged | unchanged |

Every token contains `auth_version`. Both interceptors reject missing, invalid, disabled, resigned, or stale account state before loading business permissions. The management backend reads the lightweight account row on every request. The mini backend uses the shared account-state cache and evicts it after committed account or permission changes.

Permission keys are immutable and versioned:

```text
hive:{env}:cache:management:perm-v3:{tenant}:{user}:{permissionVersion}:3
hive:{env}:cache:mini:perm-v3:{tenant}:{user}:{permissionVersion}:3
hive:{env}:cache:auth:account-v3:{tenant}:{user}
```

Old permission keys are not scanned or reused. They become unreachable after a version change and expire by TTL. Shared account-state eviction is registered for `afterCommit`, so a concurrent request cannot repopulate Redis from an uncommitted database snapshot.

## Main Code Paths

- Management authentication: `AuthService`, `AuthMapper`, `AuthTokenInterceptor`, `PermissionCacheUtil`.
- Management permission mutation: `EmployeePermissionProfileService`, `EmployeeService`, `RoleService`, `TenantManageService`.
- Mini authentication: `AuthService`, `TenantInterceptor`, `UserService`, `UserMapper`.
- Exact approvers: management `EmployeeMapper` and mini `UserMapper`.
- Exact UI checks: `management-ui/src/utils/permission.js` plus router, sidebar, navbar, attendance, and order views.
- Fresh schema: deployment migration `V20260713_003_permission_catalog_v3.sql`.

## Release Order

1. Apply the fresh baseline and the manifest migrations, including Permission Catalog V3.
2. Build and install `hive-backend-common:0.2.0`.
3. Build the management backend and mini backend from the same permission branch.
4. Build the management UI and package the mini-program source.
5. Replace both backend JARs and mirror the UI `dist` directory before restarting containers.
6. Run release integrity, secret, schema, permission-catalog, and online smoke gates.

The `0.2.0` token format intentionally forces users with older tokens to log in again once after deployment.
