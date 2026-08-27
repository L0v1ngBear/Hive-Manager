-- Sales staff need read-only access to the parts catalog when creating a
-- replacement-part or motor-replacement ticket.  Inventory permissions are
-- intentionally not granted here.
INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT role_item.id, permission.id, NOW(), 0
FROM sys_role role_item
JOIN sys_permission permission
  ON permission.perm_code = 'after_sales:part:list'
 AND permission.is_deleted = 0
WHERE role_item.role_code IN ('SALES_STAFF', 'SALES_MANAGER')
  AND role_item.is_deleted = 0
ON DUPLICATE KEY UPDATE is_deleted = 0;

-- Refresh only the users whose effective sales-role permissions changed.
UPDATE user AS user_item
JOIN sys_user_role user_role
  ON user_role.user_id = user_item.id
 AND user_role.is_deleted = 0
JOIN sys_role role_item
  ON role_item.id = user_role.role_id
 AND role_item.is_deleted = 0
SET user_item.permission_version = COALESCE(user_item.permission_version, 1) + 1,
    user_item.auth_version = COALESCE(user_item.auth_version, 1) + 1
WHERE user_item.status = 1
  AND role_item.role_code IN ('SALES_STAFF', 'SALES_MANAGER');
