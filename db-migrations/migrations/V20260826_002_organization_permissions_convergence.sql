-- The original V3 catalogue migration predates the organization module.
-- Existing databases that already ran it therefore need this forward-only
-- convergence migration; new databases receive the same rows idempotently.

INSERT INTO sys_permission (
  parent_id, perm_code, module_code, perm_type, assignable, status, sort, perm_name, create_time, update_time, is_deleted
) VALUES (
  0, 'organization', 'organization', 1, 0, 1, 810, '组织管理', NOW(), NOW(), 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), module_code = VALUES(module_code), perm_type = VALUES(perm_type),
  assignable = VALUES(assignable), status = VALUES(status), sort = VALUES(sort), perm_name = VALUES(perm_name),
  is_deleted = 0, update_time = NOW();

SET @organization_parent_id := (
  SELECT id FROM sys_permission WHERE perm_code = 'organization' LIMIT 1
);

INSERT INTO sys_permission (
  parent_id, perm_code, module_code, perm_type, assignable, status, sort, perm_name, create_time, update_time, is_deleted
) VALUES
  (@organization_parent_id, 'organization:view', 'organization', 2, 1, 1, 811, '查看部门、职位和成员', NOW(), NOW(), 0),
  (@organization_parent_id, 'organization:department:manage', 'organization', 3, 1, 1, 812, '新增和编辑部门', NOW(), NOW(), 0),
  (@organization_parent_id, 'organization:department:delete', 'organization', 3, 1, 1, 813, '删除空部门', NOW(), NOW(), 0),
  (@organization_parent_id, 'organization:position:manage', 'organization', 3, 1, 1, 814, '新增和编辑职位', NOW(), NOW(), 0),
  (@organization_parent_id, 'organization:position:delete', 'organization', 3, 1, 1, 815, '删除空职位', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), module_code = VALUES(module_code), perm_type = VALUES(perm_type),
  assignable = VALUES(assignable), status = VALUES(status), sort = VALUES(sort), perm_name = VALUES(perm_name),
  is_deleted = 0, update_time = NOW();

-- Enterprise owners have all organization permissions.  HR staff may view;
-- HR managers may maintain the department and position hierarchy.
INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT role_item.id, permission_item.id, NOW(), 0
FROM sys_role role_item
JOIN (
  SELECT 'ADMIN' AS role_code, 'organization:view' AS perm_code
  UNION ALL SELECT 'ADMIN', 'organization:department:manage'
  UNION ALL SELECT 'ADMIN', 'organization:department:delete'
  UNION ALL SELECT 'ADMIN', 'organization:position:manage'
  UNION ALL SELECT 'ADMIN', 'organization:position:delete'
  UNION ALL SELECT 'HR_STAFF', 'organization:view'
  UNION ALL SELECT 'HR_MANAGER', 'organization:view'
  UNION ALL SELECT 'HR_MANAGER', 'organization:department:manage'
  UNION ALL SELECT 'HR_MANAGER', 'organization:department:delete'
  UNION ALL SELECT 'HR_MANAGER', 'organization:position:manage'
  UNION ALL SELECT 'HR_MANAGER', 'organization:position:delete'
) AS role_grant
  ON role_grant.role_code = role_item.role_code
JOIN sys_permission permission_item
  ON permission_item.perm_code = role_grant.perm_code
 AND permission_item.is_deleted = 0
 AND permission_item.status = 1
 AND permission_item.assignable = 1
WHERE role_item.tenant_code <> 'super'
  AND role_item.is_deleted = 0
ON DUPLICATE KEY UPDATE is_deleted = 0;

-- Force new permission snapshots for users affected by the role grants.
UPDATE user AS user_item
JOIN sys_user_role user_role
  ON user_role.user_id = user_item.id
 AND user_role.tenant_code = user_item.tenant_code
 AND user_role.is_deleted = 0
JOIN sys_role role_item
  ON role_item.id = user_role.role_id
 AND role_item.is_deleted = 0
SET user_item.permission_version = COALESCE(user_item.permission_version, 1) + 1,
    user_item.auth_version = COALESCE(user_item.auth_version, 1) + 1
WHERE user_item.status = 1
  AND user_item.tenant_code <> 'super'
  AND role_item.role_code IN ('ADMIN', 'HR_STAFF', 'HR_MANAGER');
