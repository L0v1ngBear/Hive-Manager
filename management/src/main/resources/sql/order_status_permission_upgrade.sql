-- Unified order permissions and status-scoped order visibility.
-- Run this once after upgrading from the old sales/production order permission model.

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, is_deleted)
SELECT 0, 'order', 1, 300, '订单管理', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'order');

UPDATE sys_permission
SET parent_id = 0,
    perm_type = 1,
    sort = 300,
    perm_name = '订单管理',
    is_deleted = 0
WHERE perm_code = 'order';

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, is_deleted)
SELECT parent.id, item.perm_code, item.perm_type, item.sort_no, item.perm_name, 0
FROM sys_permission parent
INNER JOIN (
    SELECT 'order:*' AS perm_code, 2 AS perm_type, 0 AS sort_no, '订单管理-全部权限' AS perm_name
    UNION ALL SELECT 'order:list', 3, 1, '订单列表页面'
    UNION ALL SELECT 'order:detail', 3, 2, '查看订单详情'
    UNION ALL SELECT 'order:create', 3, 3, '创建订单'
    UNION ALL SELECT 'order:warning:setting', 3, 4, '订单预警设置'
    UNION ALL SELECT 'order:status:*', 2, 20, '订单状态-全部状态'
) item
WHERE parent.perm_code = 'order'
  AND NOT EXISTS (SELECT 1 FROM sys_permission existed WHERE existed.perm_code = item.perm_code);

UPDATE sys_permission permission
INNER JOIN (
    SELECT 'order:*' AS perm_code, 2 AS perm_type, 0 AS sort_no, '订单管理-全部权限' AS perm_name
    UNION ALL SELECT 'order:list', 3, 1, '订单列表页面'
    UNION ALL SELECT 'order:detail', 3, 2, '查看订单详情'
    UNION ALL SELECT 'order:create', 3, 3, '创建订单'
    UNION ALL SELECT 'order:warning:setting', 3, 4, '订单预警设置'
    UNION ALL SELECT 'order:status:*', 2, 20, '订单状态-全部状态'
) item ON item.perm_code = permission.perm_code
INNER JOIN sys_permission parent ON parent.perm_code = 'order'
SET permission.parent_id = parent.id,
    permission.perm_type = item.perm_type,
    permission.sort = item.sort_no,
    permission.perm_name = item.perm_name,
    permission.is_deleted = 0;

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, is_deleted)
SELECT parent.id, item.perm_code, 3, item.sort_no, item.perm_name, 0
FROM sys_permission parent
INNER JOIN (
    SELECT 'order:status:pending-confirm' AS perm_code, 1 AS sort_no, '订单状态-待确认' AS perm_name
    UNION ALL SELECT 'order:status:pending-pay', 2, '订单状态-待收款'
    UNION ALL SELECT 'order:status:pending-material', 3, '订单状态-备料中'
    UNION ALL SELECT 'order:status:budgeting', 4, '订单状态-预算中'
    UNION ALL SELECT 'order:status:budget-completed', 5, '订单状态-预算完成'
    UNION ALL SELECT 'order:status:producing', 6, '订单状态-生产中'
    UNION ALL SELECT 'order:status:pending-ship', 7, '订单状态-待发货'
    UNION ALL SELECT 'order:status:shipped', 8, '订单状态-已发货'
    UNION ALL SELECT 'order:status:completed', 9, '订单状态-已完成'
    UNION ALL SELECT 'order:status:pending-cancel', 10, '订单状态-取消审核中'
    UNION ALL SELECT 'order:status:cancelled', 11, '订单状态-已取消'
) item
WHERE parent.perm_code = 'order:status:*'
  AND NOT EXISTS (SELECT 1 FROM sys_permission existed WHERE existed.perm_code = item.perm_code);

UPDATE sys_permission permission
INNER JOIN (
    SELECT 'order:status:pending-confirm' AS perm_code, 1 AS sort_no, '订单状态-待确认' AS perm_name
    UNION ALL SELECT 'order:status:pending-pay', 2, '订单状态-待收款'
    UNION ALL SELECT 'order:status:pending-material', 3, '订单状态-备料中'
    UNION ALL SELECT 'order:status:budgeting', 4, '订单状态-预算中'
    UNION ALL SELECT 'order:status:budget-completed', 5, '订单状态-预算完成'
    UNION ALL SELECT 'order:status:producing', 6, '订单状态-生产中'
    UNION ALL SELECT 'order:status:pending-ship', 7, '订单状态-待发货'
    UNION ALL SELECT 'order:status:shipped', 8, '订单状态-已发货'
    UNION ALL SELECT 'order:status:completed', 9, '订单状态-已完成'
    UNION ALL SELECT 'order:status:pending-cancel', 10, '订单状态-取消审核中'
    UNION ALL SELECT 'order:status:cancelled', 11, '订单状态-已取消'
) item ON item.perm_code = permission.perm_code
INNER JOIN sys_permission parent ON parent.perm_code = 'order:status:*'
SET permission.parent_id = parent.id,
    permission.perm_type = 3,
    permission.sort = item.sort_no,
    permission.perm_name = item.perm_name,
    permission.is_deleted = 0;

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, is_deleted)
SELECT parent.id, 'approval:order:audit', 3, 40, '订单审批', 0
FROM sys_permission parent
WHERE parent.perm_code = 'approval'
  AND NOT EXISTS (SELECT 1 FROM sys_permission existed WHERE existed.perm_code = 'approval:order:audit');

UPDATE sys_permission permission
INNER JOIN sys_permission parent ON parent.perm_code = 'approval'
SET permission.parent_id = parent.id,
    permission.perm_type = 3,
    permission.sort = 40,
    permission.perm_name = '订单审批',
    permission.is_deleted = 0
WHERE permission.perm_code = 'approval:order:audit';

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT DISTINCT old_relation.role_id, new_permission.id, NOW(), 0
FROM sys_role_permission old_relation
INNER JOIN sys_permission old_permission ON old_permission.id = old_relation.permission_id
INNER JOIN (
    SELECT 'sales:order:*' AS old_perm_code, 'order:*' AS new_perm_code
    UNION ALL SELECT 'production:order:*', 'order:*'
    UNION ALL SELECT 'sales:order:list', 'order:list'
    UNION ALL SELECT 'production:order:list', 'order:list'
    UNION ALL SELECT 'sales:order:detail', 'order:detail'
    UNION ALL SELECT 'production:order:detail', 'order:detail'
    UNION ALL SELECT 'production:order:log', 'order:detail'
    UNION ALL SELECT 'sales:order:add', 'order:create'
    UNION ALL SELECT 'production:order:add', 'order:create'
    UNION ALL SELECT 'sales:order:status', 'order:status:*'
    UNION ALL SELECT 'production:order:status', 'order:status:*'
    UNION ALL SELECT 'sales:order:pre-confirm', 'order:status:*'
    UNION ALL SELECT 'sales:order:fulfillment', 'order:status:*'
    UNION ALL SELECT 'production:order:pre-production', 'order:status:*'
    UNION ALL SELECT 'production:order:fulfillment', 'order:status:*'
) mapping ON mapping.old_perm_code = old_permission.perm_code
INNER JOIN sys_permission new_permission ON new_permission.perm_code = mapping.new_perm_code
WHERE IFNULL(old_relation.is_deleted, 0) = 0
  AND IFNULL(old_permission.is_deleted, 0) = 0
  AND IFNULL(new_permission.is_deleted, 0) = 0
ON DUPLICATE KEY UPDATE is_deleted = 0;

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT DISTINCT role.id, permission.id, NOW(), 0
FROM sys_role role
INNER JOIN sys_permission permission
  ON permission.perm_code IN (
    'order:*',
    'order:list',
    'order:detail',
    'order:create',
    'order:warning:setting',
    'order:status:*',
    'approval:order:audit'
  )
WHERE role.role_code = 'TENANT_OWNER'
  AND IFNULL(role.is_deleted, 0) = 0
  AND IFNULL(permission.is_deleted, 0) = 0
ON DUPLICATE KEY UPDATE is_deleted = 0;

INSERT INTO sys_user_permission (tenant_code, user_id, permission_id, effect, create_time, update_time, is_deleted)
SELECT DISTINCT old_override.tenant_code, old_override.user_id, new_permission.id, old_override.effect, NOW(), NOW(), 0
FROM sys_user_permission old_override
INNER JOIN sys_permission old_permission ON old_permission.id = old_override.permission_id
INNER JOIN (
    SELECT 'sales:order:*' AS old_perm_code, 'order:*' AS new_perm_code
    UNION ALL SELECT 'production:order:*', 'order:*'
    UNION ALL SELECT 'sales:order:list', 'order:list'
    UNION ALL SELECT 'production:order:list', 'order:list'
    UNION ALL SELECT 'sales:order:detail', 'order:detail'
    UNION ALL SELECT 'production:order:detail', 'order:detail'
    UNION ALL SELECT 'production:order:log', 'order:detail'
    UNION ALL SELECT 'sales:order:add', 'order:create'
    UNION ALL SELECT 'production:order:add', 'order:create'
    UNION ALL SELECT 'sales:order:status', 'order:status:*'
    UNION ALL SELECT 'production:order:status', 'order:status:*'
    UNION ALL SELECT 'sales:order:pre-confirm', 'order:status:*'
    UNION ALL SELECT 'sales:order:fulfillment', 'order:status:*'
    UNION ALL SELECT 'production:order:pre-production', 'order:status:*'
    UNION ALL SELECT 'production:order:fulfillment', 'order:status:*'
) mapping ON mapping.old_perm_code = old_permission.perm_code
INNER JOIN sys_permission new_permission ON new_permission.perm_code = mapping.new_perm_code
WHERE IFNULL(old_override.is_deleted, 0) = 0
  AND old_override.effect IN ('GRANT', 'DENY')
  AND IFNULL(old_permission.is_deleted, 0) = 0
  AND IFNULL(new_permission.is_deleted, 0) = 0
ON DUPLICATE KEY UPDATE effect = VALUES(effect), is_deleted = 0, update_time = NOW();
