-- V20260526_002_fine_grained_role_permissions.sql
-- Purpose:
-- 1. Rebuild the built-in permission baseline around business responsibilities.
-- 2. Hide legacy test roles from customer-facing role assignment.
-- 3. Keep ordinary employees safe by default while allowing mini-app core work.
-- 4. Use BINARY comparisons to avoid cross-table collation drift.

SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS tmp_hive_perm_seed;
CREATE TEMPORARY TABLE tmp_hive_perm_seed (
    parent_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
    perm_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    perm_type INT NOT NULL,
    sort_no INT NOT NULL,
    perm_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (perm_code)
) ENGINE=MEMORY;

INSERT INTO tmp_hive_perm_seed (parent_code, perm_code, perm_type, sort_no, perm_name) VALUES
(NULL, 'dashboard', 1, 100, '总览大盘'),
(NULL, 'notification', 1, 120, '企业通知公告'),
(NULL, 'sales', 1, 200, '订单管理'),
(NULL, 'production', 1, 220, '生产管理'),
(NULL, 'inventory', 1, 300, '库存管理'),
(NULL, 'receipt', 1, 320, '出库单打印'),
(NULL, 'label', 1, 340, '标签模板'),
(NULL, 'badproduct', 1, 400, '质量管理'),
(NULL, 'customer', 1, 500, '客户管理'),
(NULL, 'price', 1, 520, '价格管理'),
(NULL, 'approval', 1, 600, '审批中心'),
(NULL, 'attendance', 1, 700, '考勤管理'),
(NULL, 'equipment', 1, 760, '设备巡检'),
(NULL, 'employee', 1, 800, '员工管理'),
(NULL, 'role', 1, 820, '角色管理'),
(NULL, 'document', 1, 900, '文档管理'),
(NULL, 'order', 1, 940, '订单预警'),
(NULL, 'table', 1, 980, '列表导出'),
('dashboard', 'dashboard:*', 3, 101, '总览大盘-全部权限'),
('dashboard', 'dashboard:ai', 2, 110, '经营建议'),
('dashboard:ai', 'dashboard:ai:view', 3, 111, '查看经营建议'),
('dashboard:ai', 'dashboard:ai:*', 3, 112, '经营建议-全部维度'),
('dashboard:ai', 'dashboard:ai:inventory', 3, 113, '经营建议-库存维度'),
('dashboard:ai', 'dashboard:ai:order', 3, 114, '经营建议-订单维度'),
('dashboard:ai', 'dashboard:ai:customer', 3, 115, '经营建议-客户维度'),
('dashboard:ai', 'dashboard:ai:quality', 3, 116, '经营建议-质量维度'),
('dashboard:ai', 'dashboard:ai:finance', 3, 117, '经营建议-财务维度'),
('dashboard:ai', 'dashboard:ai:employee', 3, 118, '经营建议-员工维度'),
('dashboard:ai', 'dashboard:ai:operation', 3, 119, '经营建议-运营维度'),
('notification', 'notification:announcement:list', 3, 121, '查看企业通知公告'),
('notification', 'notification:announcement:publish', 3, 122, '发布企业通知公告'),
('notification', 'notification:announcement:*', 3, 129, '企业通知公告-全部权限'),
('sales', 'sales:order', 2, 201, '订单'),
('sales:order', 'sales:order:*', 3, 202, '订单-全部权限'),
('sales:order', 'sales:order:list', 3, 203, '查看订单列表'),
('sales:order', 'sales:order:detail', 3, 204, '查看订单详情'),
('sales:order', 'sales:order:status', 3, 205, '创建/编辑/流转订单'),
('production', 'production:order', 2, 221, '生产订单'),
('production:order', 'production:order:*', 3, 222, '生产订单-全部权限'),
('production:order', 'production:order:list', 3, 223, '查看生产订单列表'),
('production:order', 'production:order:detail', 3, 224, '查看生产订单详情'),
('production:order', 'production:order:log', 3, 225, '查看生产订单日志'),
('production:order', 'production:order:status', 3, 226, '创建/编辑/流转生产订单'),
('inventory', 'inventory:*', 3, 301, '库存管理-全部权限'),
('inventory', 'inventory:warning:list', 3, 302, '查看库存与库存预警'),
('inventory', 'inventory:warning:setting', 3, 303, '维护库存预警设置'),
('inventory', 'inventory:record:recent', 3, 304, '查看库存流水'),
('inventory', 'inventory:trend', 3, 305, '查看库存趋势'),
('inventory', 'inventory:barcode:search', 3, 306, '搜索库存条码'),
('inventory', 'inventory:model:search', 3, 307, '搜索库存型号'),
('inventory', 'inventory:cloth:in', 3, 308, '布匹入库'),
('inventory', 'inventory:cloth:out', 3, 309, '布匹出库'),
('receipt', 'receipt:print:*', 3, 321, '出库单打印-全部权限'),
('receipt', 'receipt:print:list', 3, 322, '查看待打印出库单'),
('receipt', 'receipt:print:detail', 3, 323, '查看出库单详情'),
('receipt', 'receipt:print:mark', 3, 324, '修正/确认出库单打印'),
('receipt', 'receipt:print:cancel', 3, 325, '作废出库单'),
('label', 'label:template', 2, 341, '标签模板'),
('label:template', 'label:template:*', 3, 342, '标签模板-全部权限'),
('label:template', 'label:template:list', 3, 343, '查看标签模板'),
('label:template', 'label:template:detail', 3, 344, '查看标签模板详情'),
('label:template', 'label:template:save', 3, 345, '保存标签模板'),
('label:template', 'label:template:upload', 3, 346, '上传标签模板'),
('label:template', 'label:template:default', 3, 347, '设置默认标签模板'),
('label:template', 'label:template:disable', 3, 348, '停用标签模板'),
('badproduct', 'badproduct:*', 3, 401, '质量管理-全部权限'),
('badproduct', 'badproduct:list', 3, 402, '查看质量记录'),
('badproduct', 'badproduct:save', 3, 403, '登记质量记录'),
('badproduct', 'badproduct:process', 3, 404, '处理质量记录'),
('customer', 'customer:*', 3, 501, '客户管理-全部权限'),
('customer', 'customer:page', 3, 502, '查看客户列表'),
('customer', 'customer:detail', 3, 503, '查看客户详情'),
('customer', 'customer:add', 3, 504, '新增客户'),
('customer', 'customer:update', 3, 505, '编辑客户'),
('price', 'price:*', 3, 521, '价格管理-全部权限'),
('price', 'price:list', 3, 522, '查看价格列表'),
('price', 'price:detail', 3, 523, '查看价格详情'),
('price', 'price:publish', 3, 524, '发布/导入价格'),
('price', 'price:delete', 3, 525, '删除价格'),
('approval', 'approval:*', 3, 601, '审批中心-全部权限'),
('approval', 'approval:leave', 2, 602, '请假审批'),
('approval:leave', 'approval:leave:submit', 3, 603, '提交请假申请'),
('approval:leave', 'approval:leave:detail', 3, 604, '查看请假详情'),
('approval:leave', 'approval:leave:audit', 3, 605, '审批请假单'),
('approval', 'approval:finance', 2, 612, '财务审批'),
('approval:finance', 'approval:finance:submit', 3, 613, '提交财务审批'),
('approval:finance', 'approval:finance:detail', 3, 614, '查看财务审批详情'),
('approval:finance', 'approval:finance:audit', 3, 615, '审批财务单'),
('approval', 'approval:resignation', 2, 622, '离职审批'),
('approval:resignation', 'approval:resignation:submit', 3, 623, '提交离职审批'),
('approval:resignation', 'approval:resignation:detail', 3, 624, '查看离职审批详情'),
('approval:resignation', 'approval:resignation:audit', 3, 625, '审批离职单'),
('attendance', 'attendance:*', 3, 701, '考勤管理-全部权限'),
('attendance', 'attendance:punch', 3, 702, '小程序考勤打卡'),
('attendance', 'attendance:record:list', 3, 703, '查看考勤记录'),
('equipment', 'equipment:*', 3, 761, '设备巡检-全部权限'),
('equipment', 'equipment:list', 3, 762, '查看设备列表'),
('equipment', 'equipment:detail', 3, 763, '查看设备详情'),
('equipment', 'equipment:save', 3, 764, '维护设备'),
('equipment', 'equipment:inspection:list', 3, 765, '查看设备巡检记录'),
('equipment', 'equipment:inspection:submit', 3, 766, '提交设备巡检'),
('employee', 'employee:*', 3, 801, '员工管理-全部权限'),
('employee', 'employee:list', 3, 802, '查看员工列表'),
('employee', 'employee:detail', 3, 803, '查看员工详情'),
('employee', 'employee:create', 3, 804, '新增/导入员工'),
('employee', 'employee:update', 3, 805, '编辑员工'),
('employee', 'employee:status', 3, 806, '调整员工状态'),
('employee', 'employee:delete', 3, 807, '删除员工'),
('employee', 'employee:export', 3, 808, '导出员工数据'),
('role', 'role:*', 3, 821, '角色管理-全部权限'),
('role', 'role:list', 3, 822, '查看角色列表'),
('role', 'role:create', 3, 823, '创建角色'),
('role', 'role:update', 3, 824, '配置角色权限'),
('role', 'role:permission:list', 3, 825, '查看权限树'),
('document', 'document:*', 3, 901, '文档管理-全部权限'),
('document', 'document:list', 3, 902, '查看文档列表'),
('document', 'document:breadcrumbs', 3, 903, '查看文档路径'),
('document', 'document:folder:create', 3, 904, '创建文件夹'),
('document', 'document:file:upload', 3, 905, '上传文件'),
('document', 'document:rename', 3, 906, '重命名文档'),
('document', 'document:move', 3, 907, '移动文档'),
('order', 'order:warning:setting', 3, 941, '维护订单预警设置'),
('table', 'table:export', 3, 981, '导出列表数据');

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, create_time, update_time, is_deleted)
SELECT COALESCE(parent.id, 0), seed.perm_code, seed.perm_type, seed.sort_no, seed.perm_name, NOW(), NOW(), 0
FROM tmp_hive_perm_seed seed
LEFT JOIN sys_permission parent
    ON BINARY parent.perm_code = BINARY seed.parent_code
   AND IFNULL(parent.is_deleted, 0) = 0
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_permission existed
    WHERE BINARY existed.perm_code = BINARY seed.perm_code
);

UPDATE sys_permission permission
INNER JOIN tmp_hive_perm_seed seed
    ON BINARY seed.perm_code = BINARY permission.perm_code
LEFT JOIN sys_permission parent
    ON BINARY parent.perm_code = BINARY seed.parent_code
   AND IFNULL(parent.is_deleted, 0) = 0
SET permission.parent_id = CASE
        WHEN seed.parent_code IS NULL THEN 0
        ELSE COALESCE(parent.id, 0)
    END,
    permission.perm_type = seed.perm_type,
    permission.sort = seed.sort_no,
    permission.perm_name = seed.perm_name,
    permission.is_deleted = 0,
    permission.update_time = NOW();

DROP TEMPORARY TABLE IF EXISTS tmp_hive_builtin_role;
CREATE TEMPORARY TABLE tmp_hive_builtin_role (
    role_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    role_name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (role_code)
) ENGINE=MEMORY;

INSERT INTO tmp_hive_builtin_role (role_code, role_name) VALUES
('ADMIN', '系统管理员'),
('EMPLOYEE', '普通员工'),
('SALES_MANAGER', '销售负责人'),
('WAREHOUSE_MANAGER', '仓储负责人'),
('PRODUCTION_MANAGER', '生产负责人'),
('QUALITY_MANAGER', '质量负责人'),
('FINANCE_MANAGER', '财务负责人'),
('HR_MANAGER', '人事负责人'),
('APPROVAL_MANAGER', '审批负责人'),
('DOCUMENT_MANAGER', '文档负责人'),
('AI_MANAGER', '经营分析负责人');

UPDATE sys_role role_item
INNER JOIN tmp_hive_builtin_role seed
    ON BINARY seed.role_code = BINARY role_item.role_code
SET role_item.role_name = seed.role_name,
    role_item.is_system = 1,
    role_item.is_deleted = 0,
    role_item.update_time = NOW();

INSERT INTO sys_role (tenant_code, role_code, role_name, is_system, create_time, update_time, is_deleted)
SELECT tenant.tenant_code, seed.role_code, seed.role_name, 1, NOW(), NOW(), 0
FROM tenant
CROSS JOIN tmp_hive_builtin_role seed
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

UPDATE sys_role
SET is_deleted = 1,
    update_time = NOW()
WHERE (BINARY role_code LIKE BINARY 'TEST%'
       OR BINARY role_code = BINARY 'AUTO_DASHBOARD_AI')
  AND IFNULL(is_deleted, 0) = 0;

-- The developer-only permission node must not appear in customer role setup.
-- Keep '*' active for the real super account, but hide it in RoleService.
UPDATE sys_permission
SET is_deleted = 1,
    update_time = NOW()
WHERE BINARY perm_code IN (
    BINARY 'super',
    BINARY 'developer:super'
)
  AND IFNULL(is_deleted, 0) = 0;

DROP TEMPORARY TABLE IF EXISTS tmp_hive_role_perm_allow;
CREATE TEMPORARY TABLE tmp_hive_role_perm_allow (
    role_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    perm_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (role_code, perm_code)
) ENGINE=MEMORY;

INSERT INTO tmp_hive_role_perm_allow (role_code, perm_code) VALUES
('EMPLOYEE', 'attendance:punch'),
('EMPLOYEE', 'attendance:record:list'),
('EMPLOYEE', 'approval:leave:submit'),
('EMPLOYEE', 'approval:leave:detail'),
('EMPLOYEE', 'approval:finance:submit'),
('EMPLOYEE', 'approval:finance:detail'),
('EMPLOYEE', 'approval:resignation:submit'),
('EMPLOYEE', 'approval:resignation:detail'),
('EMPLOYEE', 'sales:order:list'),
('EMPLOYEE', 'sales:order:detail'),
('EMPLOYEE', 'sales:order:status'),
('EMPLOYEE', 'production:order:list'),
('EMPLOYEE', 'production:order:detail'),
('EMPLOYEE', 'production:order:log'),
('EMPLOYEE', 'production:order:status'),
('EMPLOYEE', 'inventory:barcode:search'),
('EMPLOYEE', 'inventory:model:search'),
('EMPLOYEE', 'inventory:record:recent'),
('EMPLOYEE', 'inventory:warning:list'),
('EMPLOYEE', 'inventory:cloth:in'),
('EMPLOYEE', 'inventory:cloth:out'),
('EMPLOYEE', 'badproduct:list'),
('EMPLOYEE', 'badproduct:save'),
('EMPLOYEE', 'customer:page'),
('EMPLOYEE', 'customer:detail'),
('EMPLOYEE', 'label:template:list'),
('EMPLOYEE', 'label:template:detail'),
('EMPLOYEE', 'label:template:default'),
('EMPLOYEE', 'equipment:list'),
('EMPLOYEE', 'equipment:detail'),
('EMPLOYEE', 'equipment:inspection:submit'),
('EMPLOYEE', 'document:list'),
('EMPLOYEE', 'document:breadcrumbs'),
('EMPLOYEE', 'notification:announcement:list'),

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
('SALES_MANAGER', 'approval:finance:submit'),
('SALES_MANAGER', 'approval:finance:detail'),
('SALES_MANAGER', 'order:warning:setting'),
('SALES_MANAGER', 'notification:announcement:list'),
('SALES_MANAGER', 'document:list'),
('SALES_MANAGER', 'document:breadcrumbs'),
('SALES_MANAGER', 'table:export'),

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
('WAREHOUSE_MANAGER', 'equipment:inspection:submit'),
('WAREHOUSE_MANAGER', 'table:export'),

('PRODUCTION_MANAGER', 'production:order:list'),
('PRODUCTION_MANAGER', 'production:order:detail'),
('PRODUCTION_MANAGER', 'production:order:log'),
('PRODUCTION_MANAGER', 'production:order:status'),
('PRODUCTION_MANAGER', 'sales:order:list'),
('PRODUCTION_MANAGER', 'sales:order:detail'),
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
('PRODUCTION_MANAGER', 'table:export'),

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
('QUALITY_MANAGER', 'table:export'),

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
('FINANCE_MANAGER', 'table:export'),

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
('HR_MANAGER', 'table:export'),

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
('APPROVAL_MANAGER', 'table:export'),

('DOCUMENT_MANAGER', 'document:list'),
('DOCUMENT_MANAGER', 'document:breadcrumbs'),
('DOCUMENT_MANAGER', 'document:folder:create'),
('DOCUMENT_MANAGER', 'document:file:upload'),
('DOCUMENT_MANAGER', 'document:rename'),
('DOCUMENT_MANAGER', 'document:move'),
('DOCUMENT_MANAGER', 'notification:announcement:list'),
('DOCUMENT_MANAGER', 'table:export'),

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
INNER JOIN sys_role r
    ON r.id = rp.role_id
INNER JOIN tmp_hive_builtin_role built_in
    ON BINARY built_in.role_code = BINARY r.role_code
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
      BINARY 'developer:super',
      BINARY 'platform'
  )
  AND BINARY permission.perm_code NOT LIKE BINARY 'platform:%'
ON DUPLICATE KEY UPDATE is_deleted = 0;

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
DROP TEMPORARY TABLE IF EXISTS tmp_hive_builtin_role;
DROP TEMPORARY TABLE IF EXISTS tmp_hive_perm_seed;
