INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, is_deleted)
SELECT 0, 'dashboard', 1, 1800, '经营大盘', 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE perm_code = 'dashboard'
);

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, is_deleted)
SELECT parent.id, 'dashboard:ai', 2, 1810, 'AI建议中心', 0
FROM sys_permission parent
WHERE parent.perm_code = 'dashboard'
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE perm_code = 'dashboard:ai'
  );

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, is_deleted)
SELECT parent.id, item.perm_code, 3, item.sort, item.perm_name, 0
FROM sys_permission parent
JOIN (
  SELECT 'dashboard:*' AS perm_code, 1801 AS sort, '经营大盘-全部权限' AS perm_name
  UNION ALL SELECT 'dashboard:ai:view', 1811, '查看全部AI建议'
  UNION ALL SELECT 'dashboard:ai:*', 1812, 'AI建议-全部维度'
  UNION ALL SELECT 'dashboard:ai:inventory', 1813, 'AI建议-库存维度'
  UNION ALL SELECT 'dashboard:ai:order', 1814, 'AI建议-订单维度'
  UNION ALL SELECT 'dashboard:ai:customer', 1815, 'AI建议-客户维度'
  UNION ALL SELECT 'dashboard:ai:quality', 1816, 'AI建议-质量维度'
  UNION ALL SELECT 'dashboard:ai:finance', 1817, 'AI建议-财务维度'
  UNION ALL SELECT 'dashboard:ai:employee', 1818, 'AI建议-员工维度'
  UNION ALL SELECT 'dashboard:ai:operation', 1819, 'AI建议-运营维度'
) item ON 1 = 1
WHERE parent.perm_code = CASE
    WHEN item.perm_code = 'dashboard:*' THEN 'dashboard'
    ELSE 'dashboard:ai'
  END
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission existed WHERE existed.perm_code = item.perm_code
  );

INSERT INTO sys_role (tenant_code, role_code, role_name, is_system, is_deleted)
SELECT tenant.tenant_code, 'AUTO_DASHBOARD_AI', 'AI建议管理员', 1, 0
FROM tenant
WHERE tenant.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role role_item
    WHERE role_item.tenant_code = tenant.tenant_code
      AND role_item.role_code = 'AUTO_DASHBOARD_AI'
      AND role_item.is_deleted = 0
  );

INSERT INTO sys_role_permission (role_id, permission_id, is_deleted)
SELECT role_item.id, permission.id, 0
FROM sys_role role_item
JOIN sys_permission permission ON permission.perm_code LIKE 'dashboard:ai:%'
WHERE role_item.role_code = 'AUTO_DASHBOARD_AI'
  AND role_item.is_deleted = 0
  AND permission.is_deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission existed
    WHERE existed.role_id = role_item.id
      AND existed.permission_id = permission.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, is_deleted)
SELECT role_item.id, permission.id, 0
FROM sys_role role_item
JOIN sys_permission permission ON permission.perm_code IN (
  'dashboard:*',
  'dashboard:ai:view',
  'dashboard:ai:*',
  'dashboard:ai:inventory',
  'dashboard:ai:order',
  'dashboard:ai:customer',
  'dashboard:ai:quality',
  'dashboard:ai:finance',
  'dashboard:ai:employee',
  'dashboard:ai:operation'
)
WHERE role_item.role_code IN ('TENANT_OWNER', 'ADMIN')
  AND role_item.is_deleted = 0
  AND permission.is_deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission existed
    WHERE existed.role_id = role_item.id
      AND existed.permission_id = permission.id
  );
