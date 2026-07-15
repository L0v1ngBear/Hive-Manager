-- Keep the unified order page while separating sales and production visibility by status.
-- Only the four retained built-in roles are adjusted; custom roles and user overrides remain intact.

SET NAMES utf8mb4;

UPDATE sys_role_permission role_permission
INNER JOIN sys_role role_item
  ON role_item.id = role_permission.role_id
INNER JOIN sys_permission permission
  ON permission.id = role_permission.permission_id
SET role_permission.is_deleted = 1
WHERE IFNULL(role_permission.is_deleted, 0) = 0
  AND IFNULL(role_item.is_deleted, 0) = 0
  AND BINARY role_item.role_code IN (BINARY 'SALES_STAFF', BINARY 'SALES_MANAGER')
  AND BINARY permission.perm_code LIKE BINARY 'order:status:%';

UPDATE sys_role_permission role_permission
INNER JOIN sys_role role_item
  ON role_item.id = role_permission.role_id
INNER JOIN sys_permission permission
  ON permission.id = role_permission.permission_id
SET role_permission.is_deleted = 1
WHERE IFNULL(role_permission.is_deleted, 0) = 0
  AND IFNULL(role_item.is_deleted, 0) = 0
  AND BINARY role_item.role_code IN (BINARY 'PRODUCTION_STAFF', BINARY 'PRODUCTION_MANAGER')
  AND BINARY permission.perm_code LIKE BINARY 'order:status:%';

DROP TEMPORARY TABLE IF EXISTS tmp_hive_order_role_status_scope;
CREATE TEMPORARY TABLE tmp_hive_order_role_status_scope (
    role_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    perm_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (role_code, perm_code)
) ENGINE=MEMORY;

INSERT INTO tmp_hive_order_role_status_scope (role_code, perm_code) VALUES
('SALES_STAFF', 'order:status:budgeting'),
('SALES_STAFF', 'order:status:budget-completed'),
('SALES_STAFF', 'order:status:pending-confirm'),
('SALES_MANAGER', 'order:status:budgeting'),
('SALES_MANAGER', 'order:status:budget-completed'),
('SALES_MANAGER', 'order:status:pending-confirm'),
('SALES_MANAGER', 'order:status:pending-cancel'),
('SALES_MANAGER', 'order:status:cancelled'),
('PRODUCTION_STAFF', 'order:status:pending-material'),
('PRODUCTION_STAFF', 'order:status:producing'),
('PRODUCTION_STAFF', 'order:status:pending-ship'),
('PRODUCTION_MANAGER', 'order:status:pending-material'),
('PRODUCTION_MANAGER', 'order:status:producing'),
('PRODUCTION_MANAGER', 'order:status:pending-ship');

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT role_item.id, permission.id, NOW(), 0
FROM sys_role role_item
INNER JOIN tmp_hive_order_role_status_scope role_scope
  ON BINARY role_scope.role_code = BINARY role_item.role_code
INNER JOIN sys_permission permission
  ON BINARY permission.perm_code = BINARY role_scope.perm_code
 AND IFNULL(permission.is_deleted, 0) = 0
WHERE IFNULL(role_item.is_deleted, 0) = 0
ON DUPLICATE KEY UPDATE is_deleted = 0;

DROP TEMPORARY TABLE IF EXISTS tmp_hive_order_role_status_scope;
