-- V20260526_003_default_role_permission_refinement.sql
-- Purpose:
-- 1. Refine built-in roles into safer business roles.
-- 2. Keep EMPLOYEE as a minimal self-service role.
-- 3. Split operator and manager permissions so default assignment is not over-privileged.
-- 4. Use BINARY comparisons to avoid collation drift across historical tables.

SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS tmp_hive_refined_role;
CREATE TEMPORARY TABLE tmp_hive_refined_role (
    role_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    role_name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (role_code)
) ENGINE=MEMORY;

INSERT INTO tmp_hive_refined_role (role_code, role_name) VALUES
('ADMIN', '系统管理员'),
('TENANT_OWNER', '租户负责人'),
('EMPLOYEE', '普通员工'),
('SALES_STAFF', '销售专员'),
('SALES_MANAGER', '销售负责人'),
('WAREHOUSE_STAFF', '仓储专员'),
('WAREHOUSE_MANAGER', '仓储负责人'),
('PRODUCTION_STAFF', '生产专员'),
('PRODUCTION_MANAGER', '生产负责人'),
('QUALITY_STAFF', '质量专员'),
('QUALITY_MANAGER', '质量负责人'),
('FINANCE_STAFF', '财务专员'),
('FINANCE_MANAGER', '财务负责人'),
('HR_STAFF', '人事专员'),
('HR_MANAGER', '人事负责人'),
('APPROVAL_MANAGER', '审批负责人'),
('DOCUMENT_MANAGER', '文档负责人'),
('EQUIPMENT_STAFF', '设备巡检员'),
('EQUIPMENT_MANAGER', '设备负责人'),
('AI_MANAGER', '经营分析负责人');

UPDATE sys_role role_item
INNER JOIN tmp_hive_refined_role seed
    ON BINARY seed.role_code = BINARY role_item.role_code
SET role_item.role_name = seed.role_name,
    role_item.is_system = 1,
    role_item.is_deleted = 0,
    role_item.update_time = NOW();

INSERT INTO sys_role (tenant_code, role_code, role_name, is_system, create_time, update_time, is_deleted)
SELECT tenant.tenant_code, seed.role_code, seed.role_name, 1, NOW(), NOW(), 0
FROM tenant
CROSS JOIN tmp_hive_refined_role seed
WHERE tenant.tenant_code IS NOT NULL
  AND tenant.tenant_code <> ''
  AND IFNULL(tenant.deleted, 0) = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role existed
      WHERE BINARY existed.tenant_code = BINARY tenant.tenant_code
        AND BINARY existed.role_code = BINARY seed.role_code
        AND IFNULL(existed.is_deleted, 0) = 0
  );

DROP TEMPORARY TABLE IF EXISTS tmp_hive_role_perm_allow;
CREATE TEMPORARY TABLE tmp_hive_role_perm_allow (
    role_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    perm_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (role_code, perm_code)
) ENGINE=MEMORY;

INSERT INTO tmp_hive_role_perm_allow (role_code, perm_code) VALUES
-- Ordinary employees: only self-service, announcements, and safe basic lookups.
('EMPLOYEE', 'attendance:punch'),
('EMPLOYEE', 'attendance:record:list'),
('EMPLOYEE', 'approval:leave:submit'),
('EMPLOYEE', 'approval:leave:detail'),
('EMPLOYEE', 'approval:finance:submit'),
('EMPLOYEE', 'approval:finance:detail'),
('EMPLOYEE', 'approval:resignation:submit'),
('EMPLOYEE', 'approval:resignation:detail'),
('EMPLOYEE', 'equipment:list'),
('EMPLOYEE', 'equipment:detail'),
('EMPLOYEE', 'equipment:inspection:submit'),
('EMPLOYEE', 'document:list'),
('EMPLOYEE', 'document:breadcrumbs'),
('EMPLOYEE', 'notification:announcement:list'),

-- Sales.
('SALES_STAFF', 'sales:order:list'),
('SALES_STAFF', 'sales:order:detail'),
('SALES_STAFF', 'sales:order:status'),
('SALES_STAFF', 'production:order:list'),
('SALES_STAFF', 'production:order:detail'),
('SALES_STAFF', 'production:order:log'),
('SALES_STAFF', 'customer:page'),
('SALES_STAFF', 'customer:detail'),
('SALES_STAFF', 'customer:add'),
('SALES_STAFF', 'customer:update'),
('SALES_STAFF', 'price:list'),
('SALES_STAFF', 'price:detail'),
('SALES_STAFF', 'approval:finance:submit'),
('SALES_STAFF', 'approval:finance:detail'),
('SALES_STAFF', 'notification:announcement:list'),
('SALES_STAFF', 'document:list'),
('SALES_STAFF', 'document:breadcrumbs'),

('SALES_MANAGER', 'sales:order:list'),
('SALES_MANAGER', 'sales:order:detail'),
('SALES_MANAGER', 'sales:order:status'),
('SALES_MANAGER', 'production:order:list'),
('SALES_MANAGER', 'production:order:detail'),
('SALES_MANAGER', 'production:order:log'),
('SALES_MANAGER', 'customer:page'),
('SALES_MANAGER', 'customer:detail'),
('SALES_MANAGER', 'customer:add'),
('SALES_MANAGER', 'customer:update'),
('SALES_MANAGER', 'price:list'),
('SALES_MANAGER', 'price:detail'),
('SALES_MANAGER', 'price:publish'),
('SALES_MANAGER', 'approval:finance:submit'),
('SALES_MANAGER', 'approval:finance:detail'),
('SALES_MANAGER', 'order:warning:setting'),
('SALES_MANAGER', 'notification:announcement:list'),
('SALES_MANAGER', 'document:list'),
('SALES_MANAGER', 'document:breadcrumbs'),
('SALES_MANAGER', 'table:export'),

-- Warehouse.
('WAREHOUSE_STAFF', 'inventory:warning:list'),
('WAREHOUSE_STAFF', 'inventory:record:recent'),
('WAREHOUSE_STAFF', 'inventory:trend'),
('WAREHOUSE_STAFF', 'inventory:barcode:search'),
('WAREHOUSE_STAFF', 'inventory:model:search'),
('WAREHOUSE_STAFF', 'inventory:cloth:in'),
('WAREHOUSE_STAFF', 'inventory:cloth:out'),
('WAREHOUSE_STAFF', 'receipt:print:list'),
('WAREHOUSE_STAFF', 'receipt:print:detail'),
('WAREHOUSE_STAFF', 'receipt:print:mark'),
('WAREHOUSE_STAFF', 'label:template:list'),
('WAREHOUSE_STAFF', 'label:template:detail'),
('WAREHOUSE_STAFF', 'label:template:default'),
('WAREHOUSE_STAFF', 'sales:order:list'),
('WAREHOUSE_STAFF', 'sales:order:detail'),
('WAREHOUSE_STAFF', 'production:order:list'),
('WAREHOUSE_STAFF', 'production:order:detail'),
('WAREHOUSE_STAFF', 'equipment:list'),
('WAREHOUSE_STAFF', 'equipment:detail'),
('WAREHOUSE_STAFF', 'equipment:inspection:submit'),
('WAREHOUSE_STAFF', 'notification:announcement:list'),

('WAREHOUSE_MANAGER', 'inventory:warning:list'),
('WAREHOUSE_MANAGER', 'inventory:warning:setting'),
('WAREHOUSE_MANAGER', 'inventory:record:recent'),
('WAREHOUSE_MANAGER', 'inventory:trend'),
('WAREHOUSE_MANAGER', 'inventory:barcode:search'),
('WAREHOUSE_MANAGER', 'inventory:model:search'),
('WAREHOUSE_MANAGER', 'inventory:cloth:in'),
('WAREHOUSE_MANAGER', 'inventory:cloth:out'),
('WAREHOUSE_MANAGER', 'receipt:print:list'),
('WAREHOUSE_MANAGER', 'receipt:print:detail'),
('WAREHOUSE_MANAGER', 'receipt:print:mark'),
('WAREHOUSE_MANAGER', 'receipt:print:cancel'),
('WAREHOUSE_MANAGER', 'label:template:list'),
('WAREHOUSE_MANAGER', 'label:template:detail'),
('WAREHOUSE_MANAGER', 'label:template:save'),
('WAREHOUSE_MANAGER', 'label:template:upload'),
('WAREHOUSE_MANAGER', 'label:template:default'),
('WAREHOUSE_MANAGER', 'label:template:disable'),
('WAREHOUSE_MANAGER', 'sales:order:list'),
('WAREHOUSE_MANAGER', 'sales:order:detail'),
('WAREHOUSE_MANAGER', 'sales:order:status'),
('WAREHOUSE_MANAGER', 'production:order:list'),
('WAREHOUSE_MANAGER', 'production:order:detail'),
('WAREHOUSE_MANAGER', 'production:order:status'),
('WAREHOUSE_MANAGER', 'equipment:list'),
('WAREHOUSE_MANAGER', 'equipment:detail'),
('WAREHOUSE_MANAGER', 'equipment:inspection:list'),
('WAREHOUSE_MANAGER', 'equipment:inspection:submit'),
('WAREHOUSE_MANAGER', 'notification:announcement:list'),
('WAREHOUSE_MANAGER', 'table:export'),

-- Production.
('PRODUCTION_STAFF', 'production:order:list'),
('PRODUCTION_STAFF', 'production:order:detail'),
('PRODUCTION_STAFF', 'production:order:log'),
('PRODUCTION_STAFF', 'production:order:status'),
('PRODUCTION_STAFF', 'sales:order:list'),
('PRODUCTION_STAFF', 'sales:order:detail'),
('PRODUCTION_STAFF', 'inventory:warning:list'),
('PRODUCTION_STAFF', 'inventory:model:search'),
('PRODUCTION_STAFF', 'badproduct:list'),
('PRODUCTION_STAFF', 'badproduct:save'),
('PRODUCTION_STAFF', 'equipment:list'),
('PRODUCTION_STAFF', 'equipment:detail'),
('PRODUCTION_STAFF', 'equipment:inspection:submit'),
('PRODUCTION_STAFF', 'notification:announcement:list'),

('PRODUCTION_MANAGER', 'production:order:list'),
('PRODUCTION_MANAGER', 'production:order:detail'),
('PRODUCTION_MANAGER', 'production:order:log'),
('PRODUCTION_MANAGER', 'production:order:status'),
('PRODUCTION_MANAGER', 'sales:order:list'),
('PRODUCTION_MANAGER', 'sales:order:detail'),
('PRODUCTION_MANAGER', 'sales:order:status'),
('PRODUCTION_MANAGER', 'inventory:warning:list'),
('PRODUCTION_MANAGER', 'inventory:record:recent'),
('PRODUCTION_MANAGER', 'inventory:model:search'),
('PRODUCTION_MANAGER', 'badproduct:list'),
('PRODUCTION_MANAGER', 'badproduct:save'),
('PRODUCTION_MANAGER', 'badproduct:process'),
('PRODUCTION_MANAGER', 'equipment:list'),
('PRODUCTION_MANAGER', 'equipment:detail'),
('PRODUCTION_MANAGER', 'equipment:inspection:list'),
('PRODUCTION_MANAGER', 'equipment:inspection:submit'),
('PRODUCTION_MANAGER', 'notification:announcement:list'),
('PRODUCTION_MANAGER', 'table:export'),

-- Quality.
('QUALITY_STAFF', 'badproduct:list'),
('QUALITY_STAFF', 'badproduct:save'),
('QUALITY_STAFF', 'sales:order:list'),
('QUALITY_STAFF', 'sales:order:detail'),
('QUALITY_STAFF', 'production:order:list'),
('QUALITY_STAFF', 'production:order:detail'),
('QUALITY_STAFF', 'production:order:log'),
('QUALITY_STAFF', 'document:list'),
('QUALITY_STAFF', 'document:breadcrumbs'),
('QUALITY_STAFF', 'document:file:upload'),
('QUALITY_STAFF', 'notification:announcement:list'),

('QUALITY_MANAGER', 'badproduct:list'),
('QUALITY_MANAGER', 'badproduct:save'),
('QUALITY_MANAGER', 'badproduct:process'),
('QUALITY_MANAGER', 'sales:order:list'),
('QUALITY_MANAGER', 'sales:order:detail'),
('QUALITY_MANAGER', 'production:order:list'),
('QUALITY_MANAGER', 'production:order:detail'),
('QUALITY_MANAGER', 'production:order:log'),
('QUALITY_MANAGER', 'equipment:list'),
('QUALITY_MANAGER', 'equipment:detail'),
('QUALITY_MANAGER', 'equipment:inspection:list'),
('QUALITY_MANAGER', 'document:list'),
('QUALITY_MANAGER', 'document:breadcrumbs'),
('QUALITY_MANAGER', 'document:file:upload'),
('QUALITY_MANAGER', 'notification:announcement:list'),
('QUALITY_MANAGER', 'table:export'),

-- Finance.
('FINANCE_STAFF', 'approval:finance'),
('FINANCE_STAFF', 'approval:finance:submit'),
('FINANCE_STAFF', 'approval:finance:detail'),
('FINANCE_STAFF', 'sales:order:list'),
('FINANCE_STAFF', 'sales:order:detail'),
('FINANCE_STAFF', 'production:order:list'),
('FINANCE_STAFF', 'production:order:detail'),
('FINANCE_STAFF', 'price:list'),
('FINANCE_STAFF', 'price:detail'),
('FINANCE_STAFF', 'notification:announcement:list'),

('FINANCE_MANAGER', 'approval:finance'),
('FINANCE_MANAGER', 'approval:finance:submit'),
('FINANCE_MANAGER', 'approval:finance:detail'),
('FINANCE_MANAGER', 'approval:finance:audit'),
('FINANCE_MANAGER', 'sales:order:list'),
('FINANCE_MANAGER', 'sales:order:detail'),
('FINANCE_MANAGER', 'production:order:list'),
('FINANCE_MANAGER', 'production:order:detail'),
('FINANCE_MANAGER', 'price:list'),
('FINANCE_MANAGER', 'price:detail'),
('FINANCE_MANAGER', 'price:publish'),
('FINANCE_MANAGER', 'price:delete'),
('FINANCE_MANAGER', 'notification:announcement:list'),
('FINANCE_MANAGER', 'table:export'),

-- HR.
('HR_STAFF', 'employee:list'),
('HR_STAFF', 'employee:detail'),
('HR_STAFF', 'attendance:record:list'),
('HR_STAFF', 'approval:leave'),
('HR_STAFF', 'approval:leave:detail'),
('HR_STAFF', 'approval:resignation'),
('HR_STAFF', 'approval:resignation:detail'),
('HR_STAFF', 'notification:announcement:list'),

('HR_MANAGER', 'employee:list'),
('HR_MANAGER', 'employee:detail'),
('HR_MANAGER', 'employee:create'),
('HR_MANAGER', 'employee:update'),
('HR_MANAGER', 'employee:status'),
('HR_MANAGER', 'employee:delete'),
('HR_MANAGER', 'employee:export'),
('HR_MANAGER', 'attendance:*'),
('HR_MANAGER', 'approval:leave'),
('HR_MANAGER', 'approval:leave:detail'),
('HR_MANAGER', 'approval:leave:audit'),
('HR_MANAGER', 'approval:resignation'),
('HR_MANAGER', 'approval:resignation:detail'),
('HR_MANAGER', 'approval:resignation:audit'),
('HR_MANAGER', 'notification:announcement:list'),
('HR_MANAGER', 'notification:announcement:publish'),
('HR_MANAGER', 'table:export'),

-- Cross-module managers.
('APPROVAL_MANAGER', 'approval:leave'),
('APPROVAL_MANAGER', 'approval:leave:detail'),
('APPROVAL_MANAGER', 'approval:leave:audit'),
('APPROVAL_MANAGER', 'approval:finance'),
('APPROVAL_MANAGER', 'approval:finance:detail'),
('APPROVAL_MANAGER', 'approval:finance:audit'),
('APPROVAL_MANAGER', 'approval:resignation'),
('APPROVAL_MANAGER', 'approval:resignation:detail'),
('APPROVAL_MANAGER', 'approval:resignation:audit'),
('APPROVAL_MANAGER', 'sales:order:list'),
('APPROVAL_MANAGER', 'sales:order:detail'),
('APPROVAL_MANAGER', 'sales:order:status'),
('APPROVAL_MANAGER', 'production:order:list'),
('APPROVAL_MANAGER', 'production:order:detail'),
('APPROVAL_MANAGER', 'production:order:status'),
('APPROVAL_MANAGER', 'notification:announcement:list'),
('APPROVAL_MANAGER', 'table:export'),

('DOCUMENT_MANAGER', 'document:list'),
('DOCUMENT_MANAGER', 'document:breadcrumbs'),
('DOCUMENT_MANAGER', 'document:folder:create'),
('DOCUMENT_MANAGER', 'document:file:upload'),
('DOCUMENT_MANAGER', 'document:rename'),
('DOCUMENT_MANAGER', 'document:move'),
('DOCUMENT_MANAGER', 'notification:announcement:list'),
('DOCUMENT_MANAGER', 'table:export'),

('EQUIPMENT_STAFF', 'equipment:list'),
('EQUIPMENT_STAFF', 'equipment:detail'),
('EQUIPMENT_STAFF', 'equipment:inspection:submit'),
('EQUIPMENT_STAFF', 'notification:announcement:list'),

('EQUIPMENT_MANAGER', 'equipment:list'),
('EQUIPMENT_MANAGER', 'equipment:detail'),
('EQUIPMENT_MANAGER', 'equipment:save'),
('EQUIPMENT_MANAGER', 'equipment:inspection:list'),
('EQUIPMENT_MANAGER', 'equipment:inspection:submit'),
('EQUIPMENT_MANAGER', 'notification:announcement:list'),
('EQUIPMENT_MANAGER', 'table:export'),

('AI_MANAGER', 'dashboard:ai:view'),
('AI_MANAGER', 'dashboard:ai:*'),
('AI_MANAGER', 'dashboard:ai:inventory'),
('AI_MANAGER', 'dashboard:ai:order'),
('AI_MANAGER', 'dashboard:ai:customer'),
('AI_MANAGER', 'dashboard:ai:quality'),
('AI_MANAGER', 'dashboard:ai:finance'),
('AI_MANAGER', 'dashboard:ai:employee'),
('AI_MANAGER', 'dashboard:ai:operation'),
('AI_MANAGER', 'sales:order:list'),
('AI_MANAGER', 'production:order:list'),
('AI_MANAGER', 'inventory:warning:list'),
('AI_MANAGER', 'inventory:record:recent'),
('AI_MANAGER', 'customer:page'),
('AI_MANAGER', 'badproduct:list'),
('AI_MANAGER', 'notification:announcement:list');

UPDATE sys_role_permission rp
INNER JOIN sys_role role_item
    ON role_item.id = rp.role_id
INNER JOIN tmp_hive_refined_role seed
    ON BINARY seed.role_code = BINARY role_item.role_code
SET rp.is_deleted = 1
WHERE IFNULL(rp.is_deleted, 0) = 0;

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT role_item.id, permission.id, NOW(), 0
FROM sys_role role_item
INNER JOIN tmp_hive_role_perm_allow allow_perm
    ON BINARY allow_perm.role_code = BINARY role_item.role_code
INNER JOIN sys_permission permission
    ON BINARY permission.perm_code = BINARY allow_perm.perm_code
   AND IFNULL(permission.is_deleted, 0) = 0
WHERE IFNULL(role_item.is_deleted, 0) = 0
ON DUPLICATE KEY UPDATE is_deleted = 0;

-- Tenant administrators get all business permissions, but never receive platform or developer-only nodes.
INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT role_item.id, permission.id, NOW(), 0
FROM sys_role role_item
CROSS JOIN sys_permission permission
WHERE IFNULL(role_item.is_deleted, 0) = 0
  AND IFNULL(permission.is_deleted, 0) = 0
  AND BINARY UPPER(COALESCE(role_item.role_code, '')) IN (
      BINARY 'ADMIN',
      BINARY 'TENANT_OWNER'
  )
  AND BINARY permission.perm_code NOT IN (
      BINARY '*',
      BINARY '*:*',
      BINARY 'super',
      BINARY 'developer:super',
      BINARY 'platform'
  )
  AND BINARY permission.perm_code NOT LIKE BINARY 'platform:%'
ON DUPLICATE KEY UPDATE is_deleted = 0;

-- Real platform roles keep platform permissions. They are hidden from tenant role assignment in code.
INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT role_item.id, permission.id, NOW(), 0
FROM sys_role role_item
CROSS JOIN sys_permission permission
WHERE IFNULL(role_item.is_deleted, 0) = 0
  AND IFNULL(permission.is_deleted, 0) = 0
  AND BINARY UPPER(COALESCE(role_item.role_code, '')) IN (
      BINARY 'SUPER_ADMIN',
      BINARY 'PLATFORM_ADMIN'
  )
ON DUPLICATE KEY UPDATE is_deleted = 0;

DROP TEMPORARY TABLE IF EXISTS tmp_hive_role_perm_allow;
DROP TEMPORARY TABLE IF EXISTS tmp_hive_refined_role;
