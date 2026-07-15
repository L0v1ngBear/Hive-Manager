-- V20260702_001_unified_order_status_permissions.sql
-- Purpose:
-- 1. Replace sales/production order permission concepts with one order permission tree.
-- 2. Split order maintenance permissions by order status.
-- 3. Hide legacy sales:order and production:order permission nodes from role configuration.

SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS tmp_hive_unified_order_perm_seed;
CREATE TEMPORARY TABLE tmp_hive_unified_order_perm_seed (
    parent_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    perm_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    perm_type INT NOT NULL,
    sort_no INT NOT NULL,
    perm_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (perm_code)
) ENGINE=MEMORY;

INSERT INTO tmp_hive_unified_order_perm_seed (parent_code, perm_code, perm_type, sort_no, perm_name) VALUES
('', 'order', 1, 200, '订单管理'),
('order', 'order:*', 2, 201, '订单全部权限'),
('order', 'order:list', 3, 202, '查看订单'),
('order', 'order:detail', 3, 203, '查看订单详情'),
('order', 'order:create', 3, 204, '新建订单'),
('order', 'order:status:*', 2, 205, '维护全部订单状态'),
('order:status:*', 'order:status:budgeting', 3, 211, '维护预算中订单'),
('order:status:*', 'order:status:budget-completed', 3, 212, '维护预算完成订单'),
('order:status:*', 'order:status:pending-confirm', 3, 213, '维护待确认订单'),
('order:status:*', 'order:status:pending-pay', 3, 214, '维护待收款订单'),
('order:status:*', 'order:status:pending-material', 3, 215, '维护备料中订单'),
('order:status:*', 'order:status:producing', 3, 216, '维护生产中订单'),
('order:status:*', 'order:status:pending-ship', 3, 217, '维护待发货订单'),
('order:status:*', 'order:status:shipped', 3, 218, '维护已发货订单'),
('order:status:*', 'order:status:completed', 3, 219, '维护已完成订单'),
('order:status:*', 'order:status:pending-cancel', 3, 220, '维护取消审核中订单'),
('order:status:*', 'order:status:cancelled', 3, 221, '维护已取消订单');

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, create_time, update_time, is_deleted)
SELECT COALESCE(parent.id, 0), seed.perm_code, seed.perm_type, seed.sort_no, seed.perm_name, NOW(), NOW(), 0
FROM tmp_hive_unified_order_perm_seed seed
LEFT JOIN sys_permission parent
    ON BINARY parent.perm_code = BINARY seed.parent_code
   AND IFNULL(parent.is_deleted, 0) = 0
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_permission existed
    WHERE BINARY existed.perm_code = BINARY seed.perm_code
);

UPDATE sys_permission permission
INNER JOIN tmp_hive_unified_order_perm_seed seed
    ON BINARY seed.perm_code = BINARY permission.perm_code
LEFT JOIN sys_permission parent
    ON BINARY parent.perm_code = BINARY seed.parent_code
   AND IFNULL(parent.is_deleted, 0) = 0
SET permission.parent_id = COALESCE(parent.id, 0),
    permission.perm_type = seed.perm_type,
    permission.sort = seed.sort_no,
    permission.perm_name = seed.perm_name,
    permission.is_deleted = 0,
    permission.update_time = NOW();

DROP TEMPORARY TABLE IF EXISTS tmp_hive_unified_order_role_perm;
CREATE TEMPORARY TABLE tmp_hive_unified_order_role_perm (
    role_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    perm_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (role_code, perm_code)
) ENGINE=MEMORY;

INSERT INTO tmp_hive_unified_order_role_perm (role_code, perm_code) VALUES
('ADMIN', 'order:*'),
('TENANT_OWNER', 'order:*'),
('SUPER_ADMIN', 'order:*'),
('PLATFORM_ADMIN', 'order:*'),
('APPROVAL_MANAGER', 'order:list'),
('APPROVAL_MANAGER', 'order:detail'),
('APPROVAL_MANAGER', 'order:status:*'),
('SALES_STAFF', 'order:list'),
('SALES_STAFF', 'order:detail'),
('SALES_STAFF', 'order:create'),
('SALES_STAFF', 'order:status:budgeting'),
('SALES_STAFF', 'order:status:budget-completed'),
('SALES_STAFF', 'order:status:pending-confirm'),
('SALES_MANAGER', 'order:list'),
('SALES_MANAGER', 'order:detail'),
('SALES_MANAGER', 'order:create'),
('SALES_MANAGER', 'order:status:budgeting'),
('SALES_MANAGER', 'order:status:budget-completed'),
('SALES_MANAGER', 'order:status:pending-confirm'),
('WAREHOUSE_STAFF', 'order:list'),
('WAREHOUSE_STAFF', 'order:detail'),
('WAREHOUSE_STAFF', 'order:status:pending-material'),
('WAREHOUSE_STAFF', 'order:status:pending-ship'),
('WAREHOUSE_STAFF', 'order:status:shipped'),
('WAREHOUSE_STAFF', 'order:status:completed'),
('WAREHOUSE_MANAGER', 'order:list'),
('WAREHOUSE_MANAGER', 'order:detail'),
('WAREHOUSE_MANAGER', 'order:status:pending-material'),
('WAREHOUSE_MANAGER', 'order:status:pending-ship'),
('WAREHOUSE_MANAGER', 'order:status:shipped'),
('WAREHOUSE_MANAGER', 'order:status:completed'),
('PRODUCTION_STAFF', 'order:list'),
('PRODUCTION_STAFF', 'order:detail'),
('PRODUCTION_STAFF', 'order:status:pending-material'),
('PRODUCTION_STAFF', 'order:status:producing'),
('PRODUCTION_STAFF', 'order:status:pending-ship'),
('PRODUCTION_MANAGER', 'order:list'),
('PRODUCTION_MANAGER', 'order:detail'),
('PRODUCTION_MANAGER', 'order:status:pending-material'),
('PRODUCTION_MANAGER', 'order:status:producing'),
('PRODUCTION_MANAGER', 'order:status:pending-ship');

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT role_item.id, permission.id, NOW(), 0
FROM sys_role role_item
INNER JOIN tmp_hive_unified_order_role_perm allow_perm
    ON BINARY allow_perm.role_code = BINARY role_item.role_code
INNER JOIN sys_permission permission
    ON BINARY permission.perm_code = BINARY allow_perm.perm_code
   AND IFNULL(permission.is_deleted, 0) = 0
WHERE IFNULL(role_item.is_deleted, 0) = 0
ON DUPLICATE KEY UPDATE is_deleted = 0;

UPDATE sys_role_permission role_perm
INNER JOIN sys_permission permission
    ON permission.id = role_perm.permission_id
SET role_perm.is_deleted = 1
WHERE IFNULL(role_perm.is_deleted, 0) = 0
  AND BINARY permission.perm_code IN (
      BINARY 'sales:order:*',
      BINARY 'sales:order:list',
      BINARY 'sales:order:detail',
      BINARY 'sales:order:status',
      BINARY 'sales:order:pre-confirm',
      BINARY 'sales:order:fulfillment',
      BINARY 'production:order:*',
      BINARY 'production:order:list',
      BINARY 'production:order:detail',
      BINARY 'production:order:log',
      BINARY 'production:order:status',
      BINARY 'production:order:pre-production',
      BINARY 'production:order:fulfillment'
  );

UPDATE sys_permission
SET is_deleted = 1,
    update_time = NOW()
WHERE BINARY perm_code IN (
    BINARY 'sales:order',
    BINARY 'sales:order:*',
    BINARY 'sales:order:list',
    BINARY 'sales:order:detail',
    BINARY 'sales:order:status',
    BINARY 'sales:order:pre-confirm',
    BINARY 'sales:order:fulfillment',
    BINARY 'production:order',
    BINARY 'production:order:*',
    BINARY 'production:order:list',
    BINARY 'production:order:detail',
    BINARY 'production:order:log',
    BINARY 'production:order:status',
    BINARY 'production:order:pre-production',
    BINARY 'production:order:fulfillment'
);

DROP TEMPORARY TABLE IF EXISTS tmp_hive_unified_order_role_perm;
DROP TEMPORARY TABLE IF EXISTS tmp_hive_unified_order_perm_seed;
