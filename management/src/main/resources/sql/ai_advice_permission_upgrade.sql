-- 经营建议权限升级脚本。
-- 说明：经营、客户、员工等高维分析只应授权给老板、管理层或指定角色。

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, is_deleted)
SELECT 0, 'dashboard', 1, 100, '总览大盘', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'dashboard');

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, is_deleted)
SELECT parent.id, 'dashboard:ai:*', 2, 10, '经营建议-全部权限', 0
FROM sys_permission parent
WHERE parent.perm_code = 'dashboard'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'dashboard:ai:*');

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, is_deleted)
SELECT parent.id, 'dashboard:ai:view', 3, 1, '查看经营建议', 0
FROM sys_permission parent
WHERE parent.perm_code = 'dashboard:ai:*'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'dashboard:ai:view');

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, is_deleted)
SELECT parent.id, item.perm_code, 3, item.sort_no, item.perm_name, 0
FROM sys_permission parent
INNER JOIN (
    SELECT 'dashboard:ai:inventory' AS perm_code, 11 AS sort_no, '经营建议-库存水位与周转' AS perm_name
    UNION ALL SELECT 'dashboard:ai:order', 12, '经营建议-订单履约与交付'
    UNION ALL SELECT 'dashboard:ai:customer', 13, '经营建议-客户客情与风控'
    UNION ALL SELECT 'dashboard:ai:quality', 14, '经营建议-质量管控与溯源'
    UNION ALL SELECT 'dashboard:ai:finance', 15, '经营建议-财务健康与成本'
    UNION ALL SELECT 'dashboard:ai:employee', 16, '经营建议-员工组织与效率'
    UNION ALL SELECT 'dashboard:ai:operation', 17, '经营建议-生产运营节奏'
) item
WHERE parent.perm_code = 'dashboard:ai:*'
  AND NOT EXISTS (SELECT 1 FROM sys_permission exists_permission WHERE exists_permission.perm_code = item.perm_code);

-- 已有租户负责人默认保留最高业务权限，避免升级后老板账号看不到经营建议。
INSERT INTO sys_role_permission (role_id, permission_id, is_deleted)
SELECT role.id, permission.id, 0
FROM sys_role role
INNER JOIN sys_permission permission
  ON permission.perm_code IN (
    'dashboard', 'dashboard:ai:*', 'dashboard:ai:view',
    'dashboard:ai:inventory', 'dashboard:ai:order', 'dashboard:ai:customer',
    'dashboard:ai:quality', 'dashboard:ai:finance', 'dashboard:ai:employee',
    'dashboard:ai:operation'
  )
WHERE role.role_code = 'TENANT_OWNER'
  AND IFNULL(role.is_deleted, 0) = 0
  AND IFNULL(permission.is_deleted, 0) = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission relation
    WHERE relation.role_id = role.id
      AND relation.permission_id = permission.id
      AND IFNULL(relation.is_deleted, 0) = 0
  );
