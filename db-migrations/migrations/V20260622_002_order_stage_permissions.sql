-- V20260622_002_order_stage_permissions.sql
-- Purpose:
-- 1. Split broad order status permissions into stage-specific permissions.
-- 2. Keep tenant/platform administrators fully authorized.
-- 3. Remove broad status permissions from default business roles so duties can be separated.
-- 4. Use BINARY comparisons to avoid collation drift across historical tables.

SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS tmp_hive_order_stage_perm_seed;
CREATE TEMPORARY TABLE tmp_hive_order_stage_perm_seed (
    parent_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    perm_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    perm_type INT NOT NULL,
    sort_no INT NOT NULL,
    perm_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (perm_code)
) ENGINE=MEMORY;

INSERT INTO tmp_hive_order_stage_perm_seed (parent_code, perm_code, perm_type, sort_no, perm_name) VALUES
('approval', 'approval:order:audit', 3, 638, '审批订单'),
('sales:order', 'sales:order:pre-confirm', 3, 206, '维护确认前订单'),
('sales:order', 'sales:order:fulfillment', 3, 207, '维护履约阶段订单'),
('production:order', 'production:order:pre-production', 3, 227, '维护生产备料前订单'),
('production:order', 'production:order:fulfillment', 3, 228, '维护生产履约阶段订单');

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, create_time, update_time, is_deleted)
SELECT COALESCE(parent.id, 0), seed.perm_code, seed.perm_type, seed.sort_no, seed.perm_name, NOW(), NOW(), 0
FROM tmp_hive_order_stage_perm_seed seed
LEFT JOIN sys_permission parent
    ON BINARY parent.perm_code = BINARY seed.parent_code
   AND IFNULL(parent.is_deleted, 0) = 0
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_permission existed
    WHERE BINARY existed.perm_code = BINARY seed.perm_code
);

UPDATE sys_permission permission
INNER JOIN tmp_hive_order_stage_perm_seed seed
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

DROP TEMPORARY TABLE IF EXISTS tmp_hive_order_stage_role_perm;
CREATE TEMPORARY TABLE tmp_hive_order_stage_role_perm (
    role_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    perm_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (role_code, perm_code)
) ENGINE=MEMORY;

INSERT INTO tmp_hive_order_stage_role_perm (role_code, perm_code) VALUES
-- Administrators and tenant owners keep the full order maintenance surface.
('ADMIN', 'sales:order:pre-confirm'),
('ADMIN', 'sales:order:fulfillment'),
('ADMIN', 'production:order:pre-production'),
('ADMIN', 'production:order:fulfillment'),
('ADMIN', 'approval:order:audit'),
('TENANT_OWNER', 'sales:order:pre-confirm'),
('TENANT_OWNER', 'sales:order:fulfillment'),
('TENANT_OWNER', 'production:order:pre-production'),
('TENANT_OWNER', 'production:order:fulfillment'),
('TENANT_OWNER', 'approval:order:audit'),
('SUPER_ADMIN', 'sales:order:pre-confirm'),
('SUPER_ADMIN', 'sales:order:fulfillment'),
('SUPER_ADMIN', 'production:order:pre-production'),
('SUPER_ADMIN', 'production:order:fulfillment'),
('SUPER_ADMIN', 'approval:order:audit'),
('PLATFORM_ADMIN', 'sales:order:pre-confirm'),
('PLATFORM_ADMIN', 'sales:order:fulfillment'),
('PLATFORM_ADMIN', 'production:order:pre-production'),
('PLATFORM_ADMIN', 'production:order:fulfillment'),
('PLATFORM_ADMIN', 'approval:order:audit'),
('APPROVAL_MANAGER', 'approval:order:audit'),

-- Sales can maintain orders before customer/business confirmation.
('SALES_STAFF', 'sales:order:pre-confirm'),
('SALES_MANAGER', 'sales:order:pre-confirm'),

-- Warehouse handles fulfillment/shipping after confirmation.
('WAREHOUSE_STAFF', 'sales:order:fulfillment'),
('WAREHOUSE_MANAGER', 'sales:order:fulfillment'),
('WAREHOUSE_STAFF', 'production:order:fulfillment'),
('WAREHOUSE_MANAGER', 'production:order:fulfillment'),

-- Production owns production preparation and production execution.
('PRODUCTION_STAFF', 'production:order:pre-production'),
('PRODUCTION_MANAGER', 'production:order:pre-production'),
('PRODUCTION_STAFF', 'production:order:fulfillment'),
('PRODUCTION_MANAGER', 'production:order:fulfillment');

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT role_item.id, permission.id, NOW(), 0
FROM sys_role role_item
INNER JOIN tmp_hive_order_stage_role_perm allow_perm
    ON BINARY allow_perm.role_code = BINARY role_item.role_code
INNER JOIN sys_permission permission
    ON BINARY permission.perm_code = BINARY allow_perm.perm_code
   AND IFNULL(permission.is_deleted, 0) = 0
WHERE IFNULL(role_item.is_deleted, 0) = 0
ON DUPLICATE KEY UPDATE is_deleted = 0;

DROP TEMPORARY TABLE IF EXISTS tmp_hive_order_stage_broad_role;
CREATE TEMPORARY TABLE tmp_hive_order_stage_broad_role (
    role_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (role_code)
) ENGINE=MEMORY;

INSERT INTO tmp_hive_order_stage_broad_role (role_code) VALUES
('EMPLOYEE'),
('SALES_STAFF'),
('SALES_MANAGER'),
('WAREHOUSE_STAFF'),
('WAREHOUSE_MANAGER'),
('PRODUCTION_STAFF'),
('PRODUCTION_MANAGER'),
('QUALITY_STAFF'),
('QUALITY_MANAGER'),
('FINANCE_STAFF'),
('FINANCE_MANAGER'),
('HR_STAFF'),
('HR_MANAGER'),
('APPROVAL_MANAGER'),
('DOCUMENT_MANAGER'),
('EQUIPMENT_STAFF'),
('EQUIPMENT_MANAGER'),
('AI_MANAGER');

UPDATE sys_role_permission role_perm
INNER JOIN sys_role role_item
    ON role_item.id = role_perm.role_id
INNER JOIN tmp_hive_order_stage_broad_role broad_role
    ON BINARY broad_role.role_code = BINARY role_item.role_code
INNER JOIN sys_permission permission
    ON permission.id = role_perm.permission_id
SET role_perm.is_deleted = 1
WHERE IFNULL(role_perm.is_deleted, 0) = 0
  AND BINARY permission.perm_code IN (
      BINARY 'sales:order:status',
      BINARY 'production:order:status'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_hive_order_stage_broad_role;
DROP TEMPORARY TABLE IF EXISTS tmp_hive_order_stage_role_perm;
DROP TEMPORARY TABLE IF EXISTS tmp_hive_order_stage_perm_seed;
