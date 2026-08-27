-- 售后管理权限目录与内置角色授权。
-- 仅追加售后模块，不影响既有业务模块和自定义角色。

SET NAMES utf8mb4;

INSERT INTO sys_permission
  (parent_id, perm_code, module_code, perm_type, assignable, status, sort, perm_name, create_time, update_time, is_deleted)
VALUES
  (0, 'after_sales', 'after_sales', 1, 0, 1, 780, '售后管理', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), module_code = VALUES(module_code), perm_type = VALUES(perm_type),
  assignable = VALUES(assignable), status = VALUES(status), sort = VALUES(sort),
  perm_name = VALUES(perm_name), is_deleted = 0, update_time = NOW();

INSERT INTO sys_permission
  (parent_id, perm_code, module_code, perm_type, assignable, status, sort, perm_name, create_time, update_time, is_deleted)
SELECT parent.id, seed.perm_code, 'after_sales', seed.perm_type, 1, 1, seed.sort_no, seed.perm_name, NOW(), NOW(), 0
FROM sys_permission parent
JOIN (
  SELECT 'after_sales:list' AS perm_code, 2 AS perm_type, 781 AS sort_no, '查看售后工单' AS perm_name
  UNION ALL SELECT 'after_sales:detail', 2, 782, '查看售后工单详情'
  UNION ALL SELECT 'after_sales:create', 3, 783, '新建售后工单'
  UNION ALL SELECT 'after_sales:update', 3, 784, '编辑售后工单'
  UNION ALL SELECT 'after_sales:process', 3, 785, '处理售后工单'
  UNION ALL SELECT 'after_sales:part:list', 2, 786, '查看售后配件库'
  UNION ALL SELECT 'after_sales:part:create', 3, 787, '新建售后配件'
  UNION ALL SELECT 'after_sales:part:update', 3, 788, '编辑售后配件'
  UNION ALL SELECT 'after_sales:part:stock-in', 3, 789, '售后配件入库'
  UNION ALL SELECT 'after_sales:part:outbound', 3, 790, '售后配件出库'
) seed ON 1 = 1
WHERE parent.perm_code = 'after_sales'
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), module_code = VALUES(module_code), perm_type = VALUES(perm_type),
  assignable = VALUES(assignable), status = VALUES(status), sort = VALUES(sort),
  perm_name = VALUES(perm_name), is_deleted = 0, update_time = NOW();

DROP TEMPORARY TABLE IF EXISTS after_sales_role_permission_seed;
CREATE TEMPORARY TABLE after_sales_role_permission_seed (
  role_code varchar(50) NOT NULL,
  perm_code varchar(100) NOT NULL,
  PRIMARY KEY (role_code, perm_code)
) ENGINE=MEMORY;

-- 企业负责人拥有全部售后权限。
INSERT INTO after_sales_role_permission_seed (role_code, perm_code)
SELECT 'ADMIN', perm_code
FROM sys_permission
WHERE module_code = 'after_sales'
  AND perm_code <> 'after_sales'
  AND is_deleted = 0;

-- 销售登记并跟进工单；仓储维护/出库配件；安装人员处理上门服务。
INSERT IGNORE INTO after_sales_role_permission_seed (role_code, perm_code) VALUES
  ('SALES_STAFF', 'after_sales:list'),
  ('SALES_STAFF', 'after_sales:detail'),
  ('SALES_STAFF', 'after_sales:create'),
  ('SALES_STAFF', 'after_sales:update'),
  ('SALES_MANAGER', 'after_sales:list'),
  ('SALES_MANAGER', 'after_sales:detail'),
  ('SALES_MANAGER', 'after_sales:create'),
  ('SALES_MANAGER', 'after_sales:update'),
  ('SALES_MANAGER', 'after_sales:process'),
  ('WAREHOUSE_STAFF', 'after_sales:list'),
  ('WAREHOUSE_STAFF', 'after_sales:detail'),
  ('WAREHOUSE_STAFF', 'after_sales:part:list'),
  ('WAREHOUSE_STAFF', 'after_sales:part:outbound'),
  ('WAREHOUSE_MANAGER', 'after_sales:list'),
  ('WAREHOUSE_MANAGER', 'after_sales:detail'),
  ('WAREHOUSE_MANAGER', 'after_sales:part:list'),
  ('WAREHOUSE_MANAGER', 'after_sales:part:create'),
  ('WAREHOUSE_MANAGER', 'after_sales:part:update'),
  ('WAREHOUSE_MANAGER', 'after_sales:part:stock-in'),
  ('WAREHOUSE_MANAGER', 'after_sales:part:outbound'),
  ('INSTALLATION_STAFF', 'after_sales:list'),
  ('INSTALLATION_STAFF', 'after_sales:detail'),
  ('INSTALLATION_STAFF', 'after_sales:process'),
  ('INSTALLATION_MANAGER', 'after_sales:list'),
  ('INSTALLATION_MANAGER', 'after_sales:detail'),
  ('INSTALLATION_MANAGER', 'after_sales:process');

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT role_item.id, permission.id, NOW(), 0
FROM sys_role role_item
JOIN after_sales_role_permission_seed seed ON seed.role_code = role_item.role_code
JOIN sys_permission permission ON permission.perm_code = seed.perm_code AND permission.is_deleted = 0
WHERE role_item.is_deleted = 0
ON DUPLICATE KEY UPDATE is_deleted = 0;

UPDATE user AS user_item
JOIN sys_user_role user_role ON user_role.user_id = user_item.id AND user_role.is_deleted = 0
JOIN sys_role role_item ON role_item.id = user_role.role_id AND role_item.is_deleted = 0
JOIN after_sales_role_permission_seed seed ON seed.role_code = role_item.role_code
SET user_item.permission_version = COALESCE(user_item.permission_version, 1) + 1,
    user_item.auth_version = COALESCE(user_item.auth_version, 1) + 1
WHERE user_item.status = 1;

DROP TEMPORARY TABLE IF EXISTS after_sales_role_permission_seed;
