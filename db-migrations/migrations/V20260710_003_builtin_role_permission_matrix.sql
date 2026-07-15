-- Rebuild tenant built-in roles around one employee baseline and scoped business duties.
-- Custom roles and valid personal permission overrides are preserved.

SET NAMES utf8mb4;

-- Dedicated installation-task permissions replace the former order:list shortcut.
INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, create_time, update_time, is_deleted)
VALUES (0, 'installation', 1, 650, '安装任务', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name), perm_type = VALUES(perm_type), sort = VALUES(sort), is_deleted = 0;

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, create_time, update_time, is_deleted)
SELECT parent.id, seed.perm_code, 3, seed.sort_no, seed.perm_name, NOW(), NOW(), 0
FROM sys_permission parent
CROSS JOIN (
    SELECT 'installation:*' AS perm_code, 651 AS sort_no, '安装任务-全部权限' AS perm_name
    UNION ALL SELECT 'installation:list', 652, '查看安装任务'
    UNION ALL SELECT 'installation:update', 653, '更新安装任务'
    UNION ALL SELECT 'installation:attachment:upload', 654, '上传安装任务附件'
    UNION ALL SELECT 'installation:attachment:download', 655, '下载安装任务附件'
) seed
WHERE BINARY parent.perm_code = BINARY 'installation'
  AND IFNULL(parent.is_deleted, 0) = 0
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), perm_name = VALUES(perm_name), sort = VALUES(sort), is_deleted = 0;

-- Keep order approval separate from direct order status mutation.
INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, create_time, update_time, is_deleted)
SELECT COALESCE(parent.id, 0), 'approval:order:audit', 3, 635, '审核订单流转', NOW(), NOW(), 0
FROM (SELECT 1) singleton
LEFT JOIN sys_permission parent
  ON BINARY parent.perm_code = BINARY 'approval'
 AND IFNULL(parent.is_deleted, 0) = 0
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), perm_name = VALUES(perm_name), is_deleted = 0;

DROP TEMPORARY TABLE IF EXISTS tmp_hive_builtin_role;
CREATE TEMPORARY TABLE tmp_hive_builtin_role (
    role_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    role_name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (role_code)
) ENGINE=MEMORY;

INSERT INTO tmp_hive_builtin_role (role_code, role_name) VALUES
('ADMIN', '企业负责人'),
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
('INSTALLATION_STAFF', '安装专员'),
('INSTALLATION_MANAGER', '安装负责人'),
('APPROVAL_MANAGER', '审批负责人'),
('DOCUMENT_MANAGER', '文档负责人'),
('EQUIPMENT_STAFF', '设备巡检员'),
('EQUIPMENT_MANAGER', '设备负责人');

UPDATE sys_role role_item
INNER JOIN tmp_hive_builtin_role seed
  ON BINARY seed.role_code = BINARY role_item.role_code
SET role_item.role_name = seed.role_name,
    role_item.is_system = 1,
    role_item.update_time = NOW()
WHERE IFNULL(role_item.is_deleted, 0) = 0;

INSERT INTO sys_role (tenant_code, role_code, role_name, is_system, create_time, update_time, is_deleted)
SELECT tenant.tenant_code, seed.role_code, seed.role_name, 1, NOW(), NOW(), 0
FROM tenant
CROSS JOIN tmp_hive_builtin_role seed
WHERE tenant.tenant_code IS NOT NULL
  AND tenant.tenant_code <> ''
  AND IFNULL(tenant.deleted, 0) = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role existing_role
      WHERE BINARY existing_role.tenant_code = BINARY tenant.tenant_code
        AND BINARY existing_role.role_code = BINARY seed.role_code
        AND IFNULL(existing_role.is_deleted, 0) = 0
  );

-- Move duplicate owner-role users to ADMIN before retiring TENANT_OWNER.
INSERT INTO sys_user_role (user_id, tenant_code, role_id, create_time, is_deleted)
SELECT old_binding.user_id, old_binding.tenant_code, new_role.id, NOW(), 0
FROM sys_user_role old_binding
INNER JOIN sys_role old_role
  ON old_role.id = old_binding.role_id
 AND BINARY old_role.role_code = BINARY 'TENANT_OWNER'
 AND IFNULL(old_role.is_deleted, 0) = 0
INNER JOIN sys_role new_role
  ON BINARY new_role.tenant_code = BINARY old_binding.tenant_code
 AND BINARY new_role.role_code = BINARY 'ADMIN'
 AND IFNULL(new_role.is_deleted, 0) = 0
WHERE IFNULL(old_binding.is_deleted, 0) = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role existing_binding
      WHERE existing_binding.user_id = old_binding.user_id
        AND BINARY existing_binding.tenant_code = BINARY old_binding.tenant_code
        AND existing_binding.role_id = new_role.id
        AND IFNULL(existing_binding.is_deleted, 0) = 0
  );

-- AI role users retain employee self-service after the retired feature is removed.
INSERT INTO sys_user_role (user_id, tenant_code, role_id, create_time, is_deleted)
SELECT old_binding.user_id, old_binding.tenant_code, new_role.id, NOW(), 0
FROM sys_user_role old_binding
INNER JOIN sys_role old_role
  ON old_role.id = old_binding.role_id
 AND BINARY old_role.role_code = BINARY 'AI_MANAGER'
 AND IFNULL(old_role.is_deleted, 0) = 0
INNER JOIN sys_role new_role
  ON BINARY new_role.tenant_code = BINARY old_binding.tenant_code
 AND BINARY new_role.role_code = BINARY 'EMPLOYEE'
 AND IFNULL(new_role.is_deleted, 0) = 0
WHERE IFNULL(old_binding.is_deleted, 0) = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role existing_binding
      WHERE existing_binding.user_id = old_binding.user_id
        AND BINARY existing_binding.tenant_code = BINARY old_binding.tenant_code
        AND existing_binding.role_id = new_role.id
        AND IFNULL(existing_binding.is_deleted, 0) = 0
  );

UPDATE sys_user_role user_role
INNER JOIN sys_role old_role ON old_role.id = user_role.role_id
SET user_role.is_deleted = 1
WHERE IFNULL(user_role.is_deleted, 0) = 0
  AND BINARY old_role.role_code IN (BINARY 'TENANT_OWNER', BINARY 'AI_MANAGER');

UPDATE sys_role_permission role_permission
INNER JOIN sys_role old_role ON old_role.id = role_permission.role_id
SET role_permission.is_deleted = 1
WHERE IFNULL(role_permission.is_deleted, 0) = 0
  AND BINARY old_role.role_code IN (BINARY 'TENANT_OWNER', BINARY 'AI_MANAGER');

UPDATE sys_role old_role
SET old_role.is_deleted = 1,
    old_role.update_time = NOW()
WHERE IFNULL(old_role.is_deleted, 0) = 0
  AND BINARY old_role.role_code IN (BINARY 'TENANT_OWNER', BINARY 'AI_MANAGER');

-- Retired AI permissions and their personal overrides stay in history but are inactive.
UPDATE sys_user_permission user_permission
INNER JOIN sys_permission permission ON permission.id = user_permission.permission_id
SET user_permission.is_deleted = 1,
    user_permission.update_time = NOW()
WHERE IFNULL(user_permission.is_deleted, 0) = 0
  AND permission.perm_code LIKE BINARY 'dashboard:ai%';

UPDATE sys_role_permission role_permission
INNER JOIN sys_permission permission ON permission.id = role_permission.permission_id
SET role_permission.is_deleted = 1
WHERE IFNULL(role_permission.is_deleted, 0) = 0
  AND permission.perm_code LIKE BINARY 'dashboard:ai%';

UPDATE sys_permission permission
SET permission.is_deleted = 1,
    permission.update_time = NOW()
WHERE IFNULL(permission.is_deleted, 0) = 0
  AND permission.perm_code LIKE BINARY 'dashboard:ai%';

DROP TEMPORARY TABLE IF EXISTS tmp_hive_role_perm_allow;
CREATE TEMPORARY TABLE tmp_hive_role_perm_allow (
    role_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    perm_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (role_code, perm_code)
) ENGINE=MEMORY;

-- Every non-admin built-in role receives the same least-privilege employee baseline.
INSERT IGNORE INTO tmp_hive_role_perm_allow (role_code, perm_code)
SELECT role_item.role_code, baseline.perm_code
FROM tmp_hive_builtin_role role_item
CROSS JOIN (
    SELECT 'attendance:punch' AS perm_code
    UNION ALL SELECT 'attendance:record:list'
    UNION ALL SELECT 'approval:leave:submit'
    UNION ALL SELECT 'approval:leave:detail'
    UNION ALL SELECT 'approval:finance:submit'
    UNION ALL SELECT 'approval:finance:detail'
    UNION ALL SELECT 'approval:resignation:submit'
    UNION ALL SELECT 'approval:resignation:detail'
    UNION ALL SELECT 'document:list'
    UNION ALL SELECT 'document:breadcrumbs'
    UNION ALL SELECT 'notification:announcement:list'
) baseline
WHERE BINARY role_item.role_code <> BINARY 'ADMIN';

INSERT IGNORE INTO tmp_hive_role_perm_allow (role_code, perm_code) VALUES
-- Sales staff and manager.
('SALES_STAFF', 'customer:page'), ('SALES_STAFF', 'customer:detail'),
('SALES_STAFF', 'customer:add'), ('SALES_STAFF', 'customer:update'),
('SALES_STAFF', 'price:list'), ('SALES_STAFF', 'price:detail'),
('SALES_STAFF', 'order:list'), ('SALES_STAFF', 'order:detail'), ('SALES_STAFF', 'order:create'),
('SALES_STAFF', 'order:status:budgeting'), ('SALES_STAFF', 'order:status:budget-completed'),
('SALES_STAFF', 'order:status:pending-confirm'),
('SALES_MANAGER', 'customer:page'), ('SALES_MANAGER', 'customer:detail'),
('SALES_MANAGER', 'customer:add'), ('SALES_MANAGER', 'customer:update'),
('SALES_MANAGER', 'price:list'), ('SALES_MANAGER', 'price:detail'),
('SALES_MANAGER', 'price:publish'), ('SALES_MANAGER', 'price:delete'),
('SALES_MANAGER', 'order:list'), ('SALES_MANAGER', 'order:detail'), ('SALES_MANAGER', 'order:create'),
('SALES_MANAGER', 'order:status:*'), ('SALES_MANAGER', 'order:warning:setting'),
('SALES_MANAGER', 'table:export'),

-- Warehouse staff and manager.
('WAREHOUSE_STAFF', 'inventory:warning:list'), ('WAREHOUSE_STAFF', 'inventory:record:recent'),
('WAREHOUSE_STAFF', 'inventory:trend'), ('WAREHOUSE_STAFF', 'inventory:barcode:search'),
('WAREHOUSE_STAFF', 'inventory:model:search'), ('WAREHOUSE_STAFF', 'inventory:cloth:in'),
('WAREHOUSE_STAFF', 'inventory:cloth:out'), ('WAREHOUSE_STAFF', 'receipt:print:list'),
('WAREHOUSE_STAFF', 'receipt:print:detail'), ('WAREHOUSE_STAFF', 'receipt:print:mark'),
('WAREHOUSE_STAFF', 'label:template:list'), ('WAREHOUSE_STAFF', 'label:template:detail'),
('WAREHOUSE_STAFF', 'label:template:default'), ('WAREHOUSE_STAFF', 'order:list'),
('WAREHOUSE_STAFF', 'order:detail'), ('WAREHOUSE_STAFF', 'order:status:pending-material'),
('WAREHOUSE_STAFF', 'order:status:pending-ship'), ('WAREHOUSE_STAFF', 'order:status:shipped'),
('WAREHOUSE_MANAGER', 'inventory:*'), ('WAREHOUSE_MANAGER', 'receipt:print:*'),
('WAREHOUSE_MANAGER', 'label:template:*'), ('WAREHOUSE_MANAGER', 'order:list'),
('WAREHOUSE_MANAGER', 'order:detail'), ('WAREHOUSE_MANAGER', 'order:status:pending-material'),
('WAREHOUSE_MANAGER', 'order:status:pending-ship'), ('WAREHOUSE_MANAGER', 'order:status:shipped'),
('WAREHOUSE_MANAGER', 'table:export'),

-- Production staff and manager.
('PRODUCTION_STAFF', 'order:list'), ('PRODUCTION_STAFF', 'order:detail'),
('PRODUCTION_STAFF', 'order:status:pending-material'), ('PRODUCTION_STAFF', 'order:status:producing'),
('PRODUCTION_STAFF', 'order:status:pending-ship'), ('PRODUCTION_STAFF', 'inventory:warning:list'),
('PRODUCTION_STAFF', 'inventory:model:search'), ('PRODUCTION_STAFF', 'badproduct:list'),
('PRODUCTION_STAFF', 'badproduct:save'), ('PRODUCTION_STAFF', 'equipment:list'),
('PRODUCTION_STAFF', 'equipment:detail'), ('PRODUCTION_STAFF', 'equipment:inspection:submit'),
('PRODUCTION_MANAGER', 'order:list'), ('PRODUCTION_MANAGER', 'order:detail'),
('PRODUCTION_MANAGER', 'order:status:pending-material'), ('PRODUCTION_MANAGER', 'order:status:producing'),
('PRODUCTION_MANAGER', 'order:status:pending-ship'), ('PRODUCTION_MANAGER', 'inventory:warning:list'),
('PRODUCTION_MANAGER', 'inventory:record:recent'), ('PRODUCTION_MANAGER', 'inventory:model:search'),
('PRODUCTION_MANAGER', 'badproduct:list'), ('PRODUCTION_MANAGER', 'badproduct:save'),
('PRODUCTION_MANAGER', 'badproduct:process'), ('PRODUCTION_MANAGER', 'equipment:list'),
('PRODUCTION_MANAGER', 'equipment:detail'), ('PRODUCTION_MANAGER', 'equipment:inspection:list'),
('PRODUCTION_MANAGER', 'equipment:inspection:submit'), ('PRODUCTION_MANAGER', 'table:export'),

-- Quality staff and manager.
('QUALITY_STAFF', 'badproduct:list'), ('QUALITY_STAFF', 'badproduct:save'),
('QUALITY_STAFF', 'document:file:upload'),
('QUALITY_MANAGER', 'badproduct:list'), ('QUALITY_MANAGER', 'badproduct:save'),
('QUALITY_MANAGER', 'badproduct:process'), ('QUALITY_MANAGER', 'equipment:list'),
('QUALITY_MANAGER', 'equipment:detail'), ('QUALITY_MANAGER', 'equipment:inspection:list'),
('QUALITY_MANAGER', 'document:file:upload'), ('QUALITY_MANAGER', 'table:export'),

-- Finance staff and manager.
('FINANCE_STAFF', 'approval:finance'), ('FINANCE_STAFF', 'approval:finance:submit'),
('FINANCE_STAFF', 'approval:finance:detail'), ('FINANCE_STAFF', 'order:list'),
('FINANCE_STAFF', 'order:detail'), ('FINANCE_STAFF', 'order:status:pending-pay'),
('FINANCE_STAFF', 'price:list'), ('FINANCE_STAFF', 'price:detail'),
('FINANCE_MANAGER', 'approval:finance'), ('FINANCE_MANAGER', 'approval:finance:submit'),
('FINANCE_MANAGER', 'approval:finance:detail'), ('FINANCE_MANAGER', 'approval:finance:audit'),
('FINANCE_MANAGER', 'order:list'), ('FINANCE_MANAGER', 'order:detail'),
('FINANCE_MANAGER', 'order:status:pending-pay'), ('FINANCE_MANAGER', 'order:status:pending-material'),
('FINANCE_MANAGER', 'price:list'), ('FINANCE_MANAGER', 'price:detail'),
('FINANCE_MANAGER', 'price:publish'), ('FINANCE_MANAGER', 'price:delete'),
('FINANCE_MANAGER', 'table:export'),

-- HR staff and manager.
('HR_STAFF', 'employee:list'), ('HR_STAFF', 'employee:detail'),
('HR_STAFF', 'attendance:record:list'), ('HR_STAFF', 'approval:leave'),
('HR_STAFF', 'approval:leave:detail'), ('HR_STAFF', 'approval:resignation'),
('HR_STAFF', 'approval:resignation:detail'),
('HR_MANAGER', 'employee:*'), ('HR_MANAGER', 'attendance:*'),
('HR_MANAGER', 'approval:leave'), ('HR_MANAGER', 'approval:leave:detail'),
('HR_MANAGER', 'approval:leave:audit'), ('HR_MANAGER', 'approval:resignation'),
('HR_MANAGER', 'approval:resignation:detail'), ('HR_MANAGER', 'approval:resignation:audit'),
('HR_MANAGER', 'notification:announcement:publish'), ('HR_MANAGER', 'table:export'),

-- Installation staff and manager.
('INSTALLATION_STAFF', 'installation:list'), ('INSTALLATION_STAFF', 'installation:update'),
('INSTALLATION_STAFF', 'installation:attachment:upload'),
('INSTALLATION_STAFF', 'installation:attachment:download'),
('INSTALLATION_STAFF', 'order:list'), ('INSTALLATION_STAFF', 'order:detail'),
('INSTALLATION_STAFF', 'order:status:shipped'), ('INSTALLATION_STAFF', 'order:status:completed'),
('INSTALLATION_MANAGER', 'installation:*'), ('INSTALLATION_MANAGER', 'order:list'),
('INSTALLATION_MANAGER', 'order:detail'), ('INSTALLATION_MANAGER', 'order:status:pending-ship'),
('INSTALLATION_MANAGER', 'order:status:shipped'), ('INSTALLATION_MANAGER', 'order:status:completed'),
('INSTALLATION_MANAGER', 'table:export'),

-- Cross-module approval, document and equipment roles.
('APPROVAL_MANAGER', 'approval:leave'), ('APPROVAL_MANAGER', 'approval:leave:detail'),
('APPROVAL_MANAGER', 'approval:leave:audit'), ('APPROVAL_MANAGER', 'approval:finance'),
('APPROVAL_MANAGER', 'approval:finance:detail'), ('APPROVAL_MANAGER', 'approval:finance:audit'),
('APPROVAL_MANAGER', 'approval:resignation'), ('APPROVAL_MANAGER', 'approval:resignation:detail'),
('APPROVAL_MANAGER', 'approval:resignation:audit'), ('APPROVAL_MANAGER', 'approval:order:audit'),
('APPROVAL_MANAGER', 'order:list'), ('APPROVAL_MANAGER', 'order:detail'),
('APPROVAL_MANAGER', 'order:status:pending-confirm'), ('APPROVAL_MANAGER', 'order:status:pending-pay'),
('APPROVAL_MANAGER', 'order:status:pending-cancel'), ('APPROVAL_MANAGER', 'badproduct:list'),
('APPROVAL_MANAGER', 'badproduct:process'), ('APPROVAL_MANAGER', 'table:export'),
('DOCUMENT_MANAGER', 'document:list'), ('DOCUMENT_MANAGER', 'document:breadcrumbs'),
('DOCUMENT_MANAGER', 'document:folder:create'), ('DOCUMENT_MANAGER', 'document:file:upload'),
('DOCUMENT_MANAGER', 'document:rename'), ('DOCUMENT_MANAGER', 'document:move'),
('DOCUMENT_MANAGER', 'table:export'),
('EQUIPMENT_STAFF', 'equipment:list'), ('EQUIPMENT_STAFF', 'equipment:detail'),
('EQUIPMENT_STAFF', 'equipment:inspection:submit'),
('EQUIPMENT_MANAGER', 'equipment:list'), ('EQUIPMENT_MANAGER', 'equipment:detail'),
('EQUIPMENT_MANAGER', 'equipment:save'), ('EQUIPMENT_MANAGER', 'equipment:inspection:list'),
('EQUIPMENT_MANAGER', 'equipment:inspection:submit'), ('EQUIPMENT_MANAGER', 'table:export');

-- One-time reset applies only to retained built-in roles. Custom roles are untouched.
UPDATE sys_role_permission role_permission
INNER JOIN sys_role role_item ON role_item.id = role_permission.role_id
INNER JOIN tmp_hive_builtin_role seed
  ON BINARY seed.role_code = BINARY role_item.role_code
SET role_permission.is_deleted = 1
WHERE IFNULL(role_item.is_deleted, 0) = 0
  AND IFNULL(role_permission.is_deleted, 0) = 0;

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT role_item.id, permission.id, NOW(), 0
FROM sys_role role_item
INNER JOIN tmp_hive_role_perm_allow allow_permission
  ON BINARY allow_permission.role_code = BINARY role_item.role_code
INNER JOIN sys_permission permission
  ON BINARY permission.perm_code = BINARY allow_permission.perm_code
 AND IFNULL(permission.is_deleted, 0) = 0
WHERE IFNULL(role_item.is_deleted, 0) = 0
ON DUPLICATE KEY UPDATE is_deleted = 0;

-- ADMIN receives every active tenant permission except platform/developer-only nodes.
INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT role_item.id, permission.id, NOW(), 0
FROM sys_role role_item
CROSS JOIN sys_permission permission
WHERE IFNULL(role_item.is_deleted, 0) = 0
  AND BINARY role_item.role_code = BINARY 'ADMIN'
  AND IFNULL(permission.is_deleted, 0) = 0
  AND BINARY permission.perm_code NOT IN (
      BINARY '*', BINARY '*:*', BINARY 'super', BINARY 'developer:super', BINARY 'platform'
  )
  AND BINARY permission.perm_code NOT LIKE BINARY 'platform:%'
  AND BINARY permission.perm_code NOT LIKE BINARY 'dashboard:ai%'
ON DUPLICATE KEY UPDATE is_deleted = 0;

DROP TEMPORARY TABLE IF EXISTS tmp_hive_role_perm_allow;
DROP TEMPORARY TABLE IF EXISTS tmp_hive_builtin_role;
