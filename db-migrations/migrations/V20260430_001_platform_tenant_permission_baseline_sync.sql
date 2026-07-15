INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, is_deleted)
SELECT parent.id, 'platform:tenant:create', 3, 1112, '创建租户', 0
FROM sys_permission parent
WHERE parent.perm_code = 'platform:tenant'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_permission existed
    WHERE existed.perm_code = 'platform:tenant:create'
  );

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, is_deleted)
SELECT parent.id, 'platform:tenant:*', 3, 1119, '租户管理-全部权限', 0
FROM sys_permission parent
WHERE parent.perm_code = 'platform:tenant'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_permission existed
    WHERE existed.perm_code = 'platform:tenant:*'
  );
