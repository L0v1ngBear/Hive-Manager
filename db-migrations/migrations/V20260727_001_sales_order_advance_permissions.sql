-- Keep custom roles user-managed. Only repair the system built-in sales roles.
-- Their existing self/department data scopes continue to restrict which orders
-- they can advance, so this does not broaden order visibility.

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT r.id, p.id, NOW(), 0
FROM sys_role r
JOIN sys_permission p
  ON BINARY p.perm_code IN (
    BINARY 'order:status:budgeting:advance',
    BINARY 'order:status:pending-confirm:advance',
    BINARY 'order:status:pending-pay:advance',
    BINARY 'order:status:pending-material:advance',
    BINARY 'order:status:producing:advance',
    BINARY 'order:status:pending-ship:advance',
    BINARY 'order:status:shipped:advance'
  )
WHERE r.is_deleted = 0
  AND BINARY r.tenant_code <> BINARY 'super'
  AND r.role_code IN ('SALES_STAFF', 'SALES_MANAGER')
  AND p.is_deleted = 0
  AND p.status = 1
  AND p.assignable = 1
ON DUPLICATE KEY UPDATE is_deleted = 0;

-- Existing access tokens carry a permission-version snapshot. Incrementing the
-- tenant users' version makes the repaired built-in permissions take effect
-- without changing any order, custom role, role assignment, or business data.
UPDATE `user`
SET permission_version = permission_version + 1
WHERE BINARY tenant_code <> BINARY 'super';
