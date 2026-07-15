-- Hive tenant permission catalog V3.
-- Historical migrations are immutable; this migration is additive and repeatable.

DELIMITER $$

DROP PROCEDURE IF EXISTS hive_permission_v3_add_column$$
CREATE PROCEDURE hive_permission_v3_add_column(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND column_name = p_column_name
    ) THEN
        SET @permission_v3_ddl = CONCAT(
            'ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_definition
        );
        PREPARE permission_v3_stmt FROM @permission_v3_ddl;
        EXECUTE permission_v3_stmt;
        DEALLOCATE PREPARE permission_v3_stmt;
    END IF;
END$$

DELIMITER ;

CALL hive_permission_v3_add_column(
    'sys_permission', 'module_code',
    'varchar(64) DEFAULT NULL COMMENT ''权限所属模块'' AFTER `perm_code`'
);
CALL hive_permission_v3_add_column(
    'sys_permission', 'assignable',
    'tinyint NOT NULL DEFAULT 0 COMMENT ''是否可分配：0-否，1-是'' AFTER `perm_type`'
);
CALL hive_permission_v3_add_column(
    'sys_permission', 'status',
    'tinyint NOT NULL DEFAULT 1 COMMENT ''权限状态：0-停用，1-启用'' AFTER `assignable`'
);
CALL hive_permission_v3_add_column(
    'user', 'permission_version',
    'bigint NOT NULL DEFAULT 1 COMMENT ''权限版本'' AFTER `role_level`'
);
CALL hive_permission_v3_add_column(
    'user', 'auth_version',
    'bigint NOT NULL DEFAULT 1 COMMENT ''登录态版本'' AFTER `permission_version`'
);

DROP PROCEDURE hive_permission_v3_add_column;

CREATE TABLE IF NOT EXISTS `sys_permission_catalog` (
  `id` tinyint NOT NULL,
  `catalog_version` bigint NOT NULL DEFAULT 1,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户权限目录版本';

CREATE TABLE IF NOT EXISTS `order_responsibility` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(50) NOT NULL,
  `order_type` varchar(20) NOT NULL COMMENT 'sales/production',
  `order_id` varchar(50) NOT NULL,
  `lane` varchar(20) NOT NULL COMMENT 'SALES/PRODUCTION/INSTALLATION',
  `user_id` bigint DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `active` tinyint NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_responsibility_lane` (`tenant_code`,`order_type`,`order_id`,`lane`),
  KEY `idx_order_responsibility_user` (`tenant_code`,`lane`,`user_id`,`active`),
  KEY `idx_order_responsibility_department` (`tenant_code`,`lane`,`department_id`,`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单当前责任范围';

DROP TEMPORARY TABLE IF EXISTS permission_v3_catalog;
CREATE TEMPORARY TABLE permission_v3_catalog (
  perm_code varchar(100) NOT NULL,
  parent_code varchar(100) DEFAULT NULL,
  module_code varchar(64) NOT NULL,
  perm_type int NOT NULL,
  assignable tinyint NOT NULL,
  sort_no int NOT NULL,
  perm_name varchar(100) NOT NULL,
  PRIMARY KEY (perm_code)
);

INSERT INTO permission_v3_catalog
  (perm_code, parent_code, module_code, perm_type, assignable, sort_no, perm_name)
VALUES
('dashboard', NULL, 'dashboard', 1, 0, 100, '总览大盘'),
('dashboard:view', 'dashboard', 'dashboard', 2, 1, 101, '查看总览大盘'),

('notification', NULL, 'notification', 1, 0, 120, '企业通知公告'),
('notification:announcement:list', 'notification', 'notification', 2, 1, 121, '查看企业通知公告'),
('notification:announcement:publish', 'notification', 'notification', 3, 1, 122, '发布企业通知公告'),

('order', NULL, 'order', 1, 0, 200, '订单管理'),
('order:list', 'order', 'order', 2, 1, 201, '查看订单列表'),
('order:detail', 'order', 'order', 2, 1, 202, '查看订单详情'),
('order:create', 'order', 'order', 3, 1, 203, '新建订单'),
('order:update', 'order', 'order', 3, 1, 204, '编辑订单'),
('order:print', 'order', 'order', 3, 1, 205, '打印订单'),
('order:warning:list', 'order', 'order', 2, 1, 206, '查看订单预警'),
('order:warning:setting', 'order', 'order', 3, 1, 207, '设置订单预警'),
('order:audit', 'order', 'order', 1, 0, 220, '订单审核'),
('order:audit:shipment', 'order:audit', 'order', 3, 1, 221, '审核发货申请'),
('order:audit:cancel', 'order:audit', 'order', 3, 1, 222, '审核取消申请'),
('order:scope', 'order', 'order', 1, 0, 230, '订单数据范围'),
('order:scope:sales:self', 'order:scope', 'order', 4, 1, 231, '本人销售订单'),
('order:scope:sales:department', 'order:scope', 'order', 4, 1, 232, '本部门销售订单'),
('order:scope:production:self', 'order:scope', 'order', 4, 1, 233, '本人生产订单'),
('order:scope:production:department', 'order:scope', 'order', 4, 1, 234, '本部门生产订单'),
('order:scope:assigned', 'order:scope', 'order', 4, 1, 235, '分配给我的订单'),
('order:scope:installation:department', 'order:scope', 'order', 4, 1, 236, '本部门安装订单'),
('order:scope:tenant', 'order:scope', 'order', 4, 1, 237, '全部租户订单'),
('order:status', 'order', 'order', 1, 0, 240, '订单状态权限'),

('order:status:budgeting', 'order:status', 'order', 1, 0, 241, '图纸预算中'),
('order:status:budgeting:view', 'order:status:budgeting', 'order', 4, 1, 2411, '查看图纸预算中订单'),
('order:status:budgeting:advance', 'order:status:budgeting', 'order', 4, 1, 2412, '完成图纸预算'),
('order:status:budgeting:cancel', 'order:status:budgeting', 'order', 4, 1, 2413, '取消图纸预算订单'),
('order:status:budget-completed', 'order:status', 'order', 1, 0, 242, '预算已完成'),
('order:status:budget-completed:view', 'order:status:budget-completed', 'order', 4, 1, 2421, '查看预算已完成订单'),
('order:status:pending-confirm', 'order:status', 'order', 1, 0, 243, '待确认'),
('order:status:pending-confirm:view', 'order:status:pending-confirm', 'order', 4, 1, 2431, '查看待确认订单'),
('order:status:pending-confirm:advance', 'order:status:pending-confirm', 'order', 4, 1, 2432, '推进待确认订单'),
('order:status:pending-confirm:cancel', 'order:status:pending-confirm', 'order', 4, 1, 2433, '取消待确认订单'),
('order:status:pending-pay', 'order:status', 'order', 1, 0, 244, '待收款'),
('order:status:pending-pay:view', 'order:status:pending-pay', 'order', 4, 1, 2441, '查看待收款订单'),
('order:status:pending-pay:advance', 'order:status:pending-pay', 'order', 4, 1, 2442, '推进待收款订单'),
('order:status:pending-pay:rollback', 'order:status:pending-pay', 'order', 4, 1, 2443, '回退待收款订单'),
('order:status:pending-pay:cancel', 'order:status:pending-pay', 'order', 4, 1, 2444, '取消待收款订单'),
('order:status:pending-material', 'order:status', 'order', 1, 0, 245, '备料中'),
('order:status:pending-material:view', 'order:status:pending-material', 'order', 4, 1, 2451, '查看备料中订单'),
('order:status:pending-material:advance', 'order:status:pending-material', 'order', 4, 1, 2452, '推进备料中订单'),
('order:status:pending-material:rollback', 'order:status:pending-material', 'order', 4, 1, 2453, '回退备料中订单'),
('order:status:pending-material:cancel', 'order:status:pending-material', 'order', 4, 1, 2454, '取消备料中订单'),
('order:status:producing', 'order:status', 'order', 1, 0, 246, '生产中'),
('order:status:producing:view', 'order:status:producing', 'order', 4, 1, 2461, '查看生产中订单'),
('order:status:producing:advance', 'order:status:producing', 'order', 4, 1, 2462, '推进生产中订单'),
('order:status:producing:rollback', 'order:status:producing', 'order', 4, 1, 2463, '回退生产中订单'),
('order:status:producing:cancel', 'order:status:producing', 'order', 4, 1, 2464, '取消生产中订单'),
('order:status:pending-ship', 'order:status', 'order', 1, 0, 247, '待发货'),
('order:status:pending-ship:view', 'order:status:pending-ship', 'order', 4, 1, 2471, '查看待发货订单'),
('order:status:pending-ship:advance', 'order:status:pending-ship', 'order', 4, 1, 2472, '申请发货'),
('order:status:pending-ship:rollback', 'order:status:pending-ship', 'order', 4, 1, 2473, '回退待发货订单'),
('order:status:pending-ship:cancel', 'order:status:pending-ship', 'order', 4, 1, 2474, '取消待发货订单'),
('order:status:shipped', 'order:status', 'order', 1, 0, 248, '已发货'),
('order:status:shipped:view', 'order:status:shipped', 'order', 4, 1, 2481, '查看已发货订单'),
('order:status:shipped:advance', 'order:status:shipped', 'order', 4, 1, 2482, '完成已发货订单'),
('order:status:shipped:rollback', 'order:status:shipped', 'order', 4, 1, 2483, '回退已发货订单'),
('order:status:shipped:cancel', 'order:status:shipped', 'order', 4, 1, 2484, '取消已发货订单'),
('order:status:completed', 'order:status', 'order', 1, 0, 249, '已完成'),
('order:status:completed:view', 'order:status:completed', 'order', 4, 1, 2491, '查看已完成订单'),
('order:status:completed:rollback', 'order:status:completed', 'order', 4, 1, 2492, '回退已完成订单'),
('order:status:pending-cancel', 'order:status', 'order', 1, 0, 250, '取消审核中'),
('order:status:pending-cancel:view', 'order:status:pending-cancel', 'order', 4, 1, 2501, '查看取消审核中订单'),
('order:status:cancelled', 'order:status', 'order', 1, 0, 251, '已取消'),
('order:status:cancelled:view', 'order:status:cancelled', 'order', 4, 1, 2511, '查看已取消订单'),

('inventory', NULL, 'inventory', 1, 0, 300, '库存管理'),
('inventory:list', 'inventory', 'inventory', 2, 1, 301, '查看库存列表'),
('inventory:detail', 'inventory', 'inventory', 2, 1, 302, '查看库存详情'),
('inventory:warning:list', 'inventory', 'inventory', 2, 1, 303, '查看库存预警'),
('inventory:warning:setting', 'inventory', 'inventory', 3, 1, 304, '设置库存预警'),
('inventory:record:list', 'inventory', 'inventory', 2, 1, 305, '查看库存流水'),
('inventory:trend', 'inventory', 'inventory', 2, 1, 306, '查看库存趋势'),
('inventory:barcode:search', 'inventory', 'inventory', 2, 1, 307, '查询库存条码'),
('inventory:model:search', 'inventory', 'inventory', 2, 1, 308, '查询库存型号'),
('inventory:cloth:in', 'inventory', 'inventory', 3, 1, 309, '库存入库'),
('inventory:cloth:out', 'inventory', 'inventory', 3, 1, 310, '库存出库'),
('inventory:import', 'inventory', 'inventory', 3, 1, 311, '导入库存'),
('inventory:export', 'inventory', 'inventory', 3, 1, 312, '导出库存'),

('print', NULL, 'print', 1, 0, 320, '打印管理'),
('print:receipt', 'print', 'print', 1, 0, 321, '出库单打印'),
('print:receipt:list', 'print:receipt', 'print', 2, 1, 322, '查看出库单'),
('print:receipt:detail', 'print:receipt', 'print', 2, 1, 323, '查看出库单详情'),
('print:receipt:execute', 'print:receipt', 'print', 3, 1, 324, '执行出库打印'),
('print:receipt:update', 'print:receipt', 'print', 3, 1, 325, '修正出库打印'),
('print:receipt:cancel', 'print:receipt', 'print', 3, 1, 326, '取消出库打印'),
('print:label', 'print', 'print', 1, 0, 330, '标签打印'),
('print:label:list', 'print:label', 'print', 2, 1, 331, '查看标签模板'),
('print:label:detail', 'print:label', 'print', 2, 1, 332, '查看标签模板详情'),
('print:label:create', 'print:label', 'print', 3, 1, 333, '新建标签模板'),
('print:label:update', 'print:label', 'print', 3, 1, 334, '编辑标签模板'),
('print:label:upload', 'print:label', 'print', 3, 1, 335, '上传标签模板'),
('print:label:default', 'print:label', 'print', 3, 1, 336, '设置默认标签模板'),
('print:label:disable', 'print:label', 'print', 3, 1, 337, '停用标签模板'),

('quality', NULL, 'quality', 1, 0, 400, '质量管理'),
('quality:list', 'quality', 'quality', 2, 1, 401, '查看质量记录'),
('quality:detail', 'quality', 'quality', 2, 1, 402, '查看质量详情'),
('quality:create', 'quality', 'quality', 3, 1, 403, '新建质量记录'),
('quality:update', 'quality', 'quality', 3, 1, 404, '编辑质量记录'),
('quality:process', 'quality', 'quality', 3, 1, 405, '处理质量记录'),
('quality:audit', 'quality', 'quality', 3, 1, 406, '审核质量记录'),
('quality:attachment:upload', 'quality', 'quality', 3, 1, 407, '上传质量附件'),
('quality:attachment:download', 'quality', 'quality', 3, 1, 408, '下载质量附件'),
('quality:export', 'quality', 'quality', 3, 1, 409, '导出质量记录'),

('customer', NULL, 'customer', 1, 0, 500, '客户管理'),
('customer:list', 'customer', 'customer', 2, 1, 501, '查看客户列表'),
('customer:detail', 'customer', 'customer', 2, 1, 502, '查看客户详情'),
('customer:create', 'customer', 'customer', 3, 1, 503, '新建客户'),
('customer:update', 'customer', 'customer', 3, 1, 504, '编辑客户'),
('customer:delete', 'customer', 'customer', 3, 1, 505, '删除客户'),
('customer:import', 'customer', 'customer', 3, 1, 506, '导入客户'),
('customer:export', 'customer', 'customer', 3, 1, 507, '导出客户'),

('price', NULL, 'price', 1, 0, 520, '价格管理'),
('price:list', 'price', 'price', 2, 1, 521, '查看价格列表'),
('price:detail', 'price', 'price', 2, 1, 522, '查看价格详情'),
('price:create', 'price', 'price', 3, 1, 523, '新建价格'),
('price:update', 'price', 'price', 3, 1, 524, '编辑价格'),
('price:publish', 'price', 'price', 3, 1, 525, '发布价格'),
('price:delete', 'price', 'price', 3, 1, 526, '删除价格'),
('price:import', 'price', 'price', 3, 1, 527, '导入价格'),
('price:export', 'price', 'price', 3, 1, 528, '导出价格'),

('approval', NULL, 'approval', 1, 0, 600, '审批中心'),
('approval:list', 'approval', 'approval', 2, 1, 601, '进入审批中心'),
('approval:leave', 'approval', 'approval', 1, 0, 610, '请假审批'),
('approval:leave:list', 'approval:leave', 'approval', 2, 1, 611, '查看请假审批'),
('approval:leave:submit', 'approval:leave', 'approval', 3, 1, 612, '提交请假申请'),
('approval:leave:detail', 'approval:leave', 'approval', 2, 1, 613, '查看请假详情'),
('approval:leave:audit', 'approval:leave', 'approval', 3, 1, 614, '审核请假申请'),
('approval:finance', 'approval', 'approval', 1, 0, 620, '财务审批'),
('approval:finance:list', 'approval:finance', 'approval', 2, 1, 621, '查看财务审批'),
('approval:finance:submit', 'approval:finance', 'approval', 3, 1, 622, '提交财务申请'),
('approval:finance:detail', 'approval:finance', 'approval', 2, 1, 623, '查看财务详情'),
('approval:finance:audit', 'approval:finance', 'approval', 3, 1, 624, '审核财务申请'),
('approval:resignation', 'approval', 'approval', 1, 0, 630, '离职审批'),
('approval:resignation:list', 'approval:resignation', 'approval', 2, 1, 631, '查看离职审批'),
('approval:resignation:submit', 'approval:resignation', 'approval', 3, 1, 632, '提交离职申请'),
('approval:resignation:detail', 'approval:resignation', 'approval', 2, 1, 633, '查看离职详情'),
('approval:resignation:audit', 'approval:resignation', 'approval', 3, 1, 634, '审核离职申请'),
('approval:auditor', 'approval', 'approval', 1, 0, 640, '审核人配置'),
('approval:auditor:list', 'approval:auditor', 'approval', 2, 1, 641, '查看审核人'),
('approval:auditor:setting', 'approval:auditor', 'approval', 3, 1, 642, '设置审核人'),

('installation', NULL, 'installation', 1, 0, 650, '安装任务'),
('installation:list', 'installation', 'installation', 2, 1, 651, '查看安装任务'),
('installation:detail', 'installation', 'installation', 2, 1, 652, '查看安装任务详情'),
('installation:update', 'installation', 'installation', 3, 1, 653, '处理安装任务'),
('installation:attachment:upload', 'installation', 'installation', 3, 1, 654, '上传安装附件'),
('installation:attachment:download', 'installation', 'installation', 3, 1, 655, '下载安装附件'),
('installation:export', 'installation', 'installation', 3, 1, 656, '导出安装任务'),

('attendance', NULL, 'attendance', 1, 0, 700, '考勤管理'),
('attendance:punch', 'attendance', 'attendance', 3, 1, 701, '员工打卡'),
('attendance:record:list', 'attendance', 'attendance', 2, 1, 702, '查看考勤记录'),
('attendance:rule:list', 'attendance', 'attendance', 2, 1, 703, '查看考勤规则'),
('attendance:rule:update', 'attendance', 'attendance', 3, 1, 704, '设置考勤规则'),
('attendance:export', 'attendance', 'attendance', 3, 1, 705, '导出考勤'),

('equipment', NULL, 'equipment', 1, 0, 760, '设备巡检'),
('equipment:list', 'equipment', 'equipment', 2, 1, 761, '查看设备列表'),
('equipment:detail', 'equipment', 'equipment', 2, 1, 762, '查看设备详情'),
('equipment:create', 'equipment', 'equipment', 3, 1, 763, '新增设备'),
('equipment:update', 'equipment', 'equipment', 3, 1, 764, '编辑设备'),
('equipment:disable', 'equipment', 'equipment', 3, 1, 765, '停用设备'),
('equipment:inspection:list', 'equipment', 'equipment', 2, 1, 766, '查看巡检记录'),
('equipment:inspection:submit', 'equipment', 'equipment', 3, 1, 767, '提交巡检记录'),
('equipment:export', 'equipment', 'equipment', 3, 1, 768, '导出设备'),

('employee', NULL, 'employee', 1, 0, 800, '员工管理'),
('employee:list', 'employee', 'employee', 2, 1, 801, '查看员工列表'),
('employee:detail', 'employee', 'employee', 2, 1, 802, '查看员工详情'),
('employee:create', 'employee', 'employee', 3, 1, 803, '新增员工'),
('employee:update', 'employee', 'employee', 3, 1, 804, '编辑员工'),
('employee:status', 'employee', 'employee', 3, 1, 805, '调整员工状态'),
('employee:delete', 'employee', 'employee', 3, 1, 806, '删除员工'),
('employee:import', 'employee', 'employee', 3, 1, 807, '导入员工'),
('employee:export', 'employee', 'employee', 3, 1, 808, '导出员工'),
('employee:permission:manage', 'employee', 'employee', 3, 1, 809, '配置员工单独权限'),

('role', NULL, 'role', 1, 0, 820, '角色管理'),
('role:list', 'role', 'role', 2, 1, 821, '查看角色列表'),
('role:create', 'role', 'role', 3, 1, 822, '新增角色'),
('role:update', 'role', 'role', 3, 1, 823, '编辑角色'),
('role:delete', 'role', 'role', 3, 1, 824, '删除角色'),
('role:permission:list', 'role', 'role', 2, 1, 825, '查看角色权限'),
('role:permission:update', 'role', 'role', 3, 1, 826, '配置角色权限'),

('document', NULL, 'document', 1, 0, 900, '文档管理'),
('document:list', 'document', 'document', 2, 1, 901, '查看文档'),
('document:folder:create', 'document', 'document', 3, 1, 902, '新建文件夹'),
('document:file:upload', 'document', 'document', 3, 1, 903, '上传文件'),
('document:file:download', 'document', 'document', 3, 1, 904, '下载文件'),
('document:rename', 'document', 'document', 3, 1, 905, '重命名文档'),
('document:move', 'document', 'document', 3, 1, 906, '移动文档'),
('document:delete', 'document', 'document', 3, 1, 907, '删除文档'),
('document:export', 'document', 'document', 3, 1, 908, '导出文档');

DELIMITER $$

DROP PROCEDURE IF EXISTS hive_permission_v3_upsert_catalog$$
CREATE PROCEDURE hive_permission_v3_upsert_catalog()
BEGIN
    DECLARE pass_no int DEFAULT 0;

    INSERT INTO sys_permission
      (parent_id, perm_code, module_code, perm_type, assignable, status, sort, perm_name,
       create_time, update_time, is_deleted)
    SELECT NULL, c.perm_code, c.module_code, c.perm_type, c.assignable, 1, c.sort_no,
           c.perm_name, NOW(), NOW(), 0
    FROM permission_v3_catalog c
    WHERE c.parent_code IS NULL
    ON DUPLICATE KEY UPDATE
      parent_id = VALUES(parent_id), module_code = VALUES(module_code),
      perm_type = VALUES(perm_type), assignable = VALUES(assignable), status = 1,
      sort = VALUES(sort), perm_name = VALUES(perm_name), update_time = NOW(), is_deleted = 0;

    WHILE pass_no < 5 DO
        INSERT INTO sys_permission
          (parent_id, perm_code, module_code, perm_type, assignable, status, sort, perm_name,
           create_time, update_time, is_deleted)
        SELECT parent.id, c.perm_code, c.module_code, c.perm_type, c.assignable, 1,
               c.sort_no, c.perm_name, NOW(), NOW(), 0
        FROM permission_v3_catalog c
        JOIN sys_permission parent
          ON BINARY parent.perm_code = BINARY c.parent_code
         AND parent.is_deleted = 0
        WHERE c.parent_code IS NOT NULL
        ON DUPLICATE KEY UPDATE
          parent_id = VALUES(parent_id), module_code = VALUES(module_code),
          perm_type = VALUES(perm_type), assignable = VALUES(assignable), status = 1,
          sort = VALUES(sort), perm_name = VALUES(perm_name), update_time = NOW(), is_deleted = 0;

        SET pass_no = pass_no + 1;
    END WHILE;
END$$

DELIMITER ;

CALL hive_permission_v3_upsert_catalog();
DROP PROCEDURE hive_permission_v3_upsert_catalog;

DROP TEMPORARY TABLE IF EXISTS builtin_role_v3;
CREATE TEMPORARY TABLE builtin_role_v3 (
  role_code varchar(50) NOT NULL,
  role_name varchar(50) NOT NULL,
  PRIMARY KEY (role_code)
);

INSERT INTO builtin_role_v3 (role_code, role_name) VALUES
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

INSERT INTO sys_role
  (tenant_code, role_code, role_name, is_system, create_time, update_time, is_deleted)
SELECT t.tenant_code, b.role_code, b.role_name, 1, NOW(), NOW(), 0
FROM tenant t
CROSS JOIN builtin_role_v3 b
WHERE BINARY t.tenant_code <> BINARY 'super'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role existed
    WHERE BINARY existed.tenant_code = BINARY t.tenant_code
      AND BINARY existed.role_code = BINARY b.role_code
  );

UPDATE sys_role r
JOIN builtin_role_v3 b ON BINARY b.role_code = BINARY r.role_code
SET r.role_name = b.role_name,
    r.is_system = 1,
    r.is_deleted = 0,
    r.update_time = NOW()
WHERE BINARY r.tenant_code <> BINARY 'super';

DROP TEMPORARY TABLE IF EXISTS builtin_role_permission_v3;
CREATE TEMPORARY TABLE builtin_role_permission_v3 (
  role_code varchar(50) NOT NULL,
  perm_code varchar(100) NOT NULL,
  PRIMARY KEY (role_code, perm_code)
);

INSERT INTO builtin_role_permission_v3 (role_code, perm_code)
SELECT 'ADMIN', perm_code FROM permission_v3_catalog WHERE assignable = 1;

INSERT INTO builtin_role_permission_v3 (role_code, perm_code)
SELECT b.role_code, base.perm_code
FROM builtin_role_v3 b
CROSS JOIN (
  SELECT 'dashboard:view' AS perm_code UNION ALL
  SELECT 'notification:announcement:list' UNION ALL
  SELECT 'attendance:punch' UNION ALL
  SELECT 'attendance:record:list' UNION ALL
  SELECT 'approval:list' UNION ALL
  SELECT 'approval:leave:submit' UNION ALL
  SELECT 'approval:leave:detail' UNION ALL
  SELECT 'approval:finance:submit' UNION ALL
  SELECT 'approval:finance:detail' UNION ALL
  SELECT 'approval:resignation:submit' UNION ALL
  SELECT 'approval:resignation:detail' UNION ALL
  SELECT 'document:list'
) base
WHERE b.role_code <> 'ADMIN';

INSERT IGNORE INTO builtin_role_permission_v3 (role_code, perm_code) VALUES
('SALES_STAFF','customer:list'),('SALES_STAFF','customer:detail'),('SALES_STAFF','customer:create'),('SALES_STAFF','customer:update'),
('SALES_STAFF','price:list'),('SALES_STAFF','price:detail'),('SALES_STAFF','order:list'),('SALES_STAFF','order:detail'),
('SALES_STAFF','order:create'),('SALES_STAFF','order:update'),('SALES_STAFF','order:print'),('SALES_STAFF','order:scope:sales:self'),
('SALES_STAFF','order:status:budgeting:view'),('SALES_STAFF','order:status:budgeting:advance'),('SALES_STAFF','order:status:budgeting:cancel'),
('SALES_STAFF','order:status:budget-completed:view'),('SALES_STAFF','order:status:pending-confirm:view'),
('SALES_STAFF','order:status:pending-confirm:advance'),('SALES_STAFF','order:status:pending-confirm:cancel'),
('SALES_STAFF','order:status:pending-pay:view'),('SALES_STAFF','order:status:pending-material:view'),
('SALES_STAFF','order:status:producing:view'),('SALES_STAFF','order:status:pending-ship:view'),
('SALES_STAFF','order:status:shipped:view'),('SALES_STAFF','order:status:completed:view'),
('SALES_STAFF','order:status:pending-cancel:view'),('SALES_STAFF','order:status:cancelled:view'),

('SALES_MANAGER','customer:list'),('SALES_MANAGER','customer:detail'),('SALES_MANAGER','customer:create'),('SALES_MANAGER','customer:update'),
('SALES_MANAGER','customer:delete'),('SALES_MANAGER','customer:import'),('SALES_MANAGER','customer:export'),
('SALES_MANAGER','price:list'),('SALES_MANAGER','price:detail'),('SALES_MANAGER','price:create'),('SALES_MANAGER','price:update'),
('SALES_MANAGER','price:publish'),('SALES_MANAGER','price:delete'),('SALES_MANAGER','price:import'),('SALES_MANAGER','price:export'),
('SALES_MANAGER','order:list'),('SALES_MANAGER','order:detail'),('SALES_MANAGER','order:create'),('SALES_MANAGER','order:update'),
('SALES_MANAGER','order:print'),('SALES_MANAGER','order:warning:list'),('SALES_MANAGER','order:warning:setting'),
('SALES_MANAGER','order:scope:sales:department'),
('SALES_MANAGER','order:status:budgeting:view'),('SALES_MANAGER','order:status:budgeting:advance'),('SALES_MANAGER','order:status:budgeting:cancel'),
('SALES_MANAGER','order:status:budget-completed:view'),('SALES_MANAGER','order:status:pending-confirm:view'),
('SALES_MANAGER','order:status:pending-confirm:advance'),('SALES_MANAGER','order:status:pending-confirm:cancel'),
('SALES_MANAGER','order:status:pending-pay:view'),('SALES_MANAGER','order:status:pending-material:view'),
('SALES_MANAGER','order:status:producing:view'),('SALES_MANAGER','order:status:pending-ship:view'),
('SALES_MANAGER','order:status:shipped:view'),('SALES_MANAGER','order:status:completed:view'),
('SALES_MANAGER','order:status:pending-cancel:view'),('SALES_MANAGER','order:status:cancelled:view'),

('PRODUCTION_STAFF','order:list'),('PRODUCTION_STAFF','order:detail'),('PRODUCTION_STAFF','order:update'),('PRODUCTION_STAFF','order:print'),
('PRODUCTION_STAFF','order:scope:production:self'),('PRODUCTION_STAFF','order:status:pending-material:view'),
('PRODUCTION_STAFF','order:status:pending-material:advance'),('PRODUCTION_STAFF','order:status:pending-material:rollback'),
('PRODUCTION_STAFF','order:status:producing:view'),('PRODUCTION_STAFF','order:status:producing:advance'),
('PRODUCTION_STAFF','order:status:producing:rollback'),('PRODUCTION_STAFF','order:status:pending-ship:view'),
('PRODUCTION_STAFF','order:status:pending-ship:rollback'),('PRODUCTION_STAFF','order:status:completed:view'),
('PRODUCTION_STAFF','quality:list'),('PRODUCTION_STAFF','quality:create'),('PRODUCTION_STAFF','equipment:list'),
('PRODUCTION_STAFF','equipment:detail'),('PRODUCTION_STAFF','equipment:inspection:submit'),

('PRODUCTION_MANAGER','order:list'),('PRODUCTION_MANAGER','order:detail'),('PRODUCTION_MANAGER','order:update'),('PRODUCTION_MANAGER','order:print'),
('PRODUCTION_MANAGER','order:scope:production:department'),('PRODUCTION_MANAGER','order:status:pending-material:view'),
('PRODUCTION_MANAGER','order:status:pending-material:advance'),('PRODUCTION_MANAGER','order:status:pending-material:rollback'),
('PRODUCTION_MANAGER','order:status:producing:view'),('PRODUCTION_MANAGER','order:status:producing:advance'),
('PRODUCTION_MANAGER','order:status:producing:rollback'),('PRODUCTION_MANAGER','order:status:pending-ship:view'),
('PRODUCTION_MANAGER','order:status:pending-ship:rollback'),('PRODUCTION_MANAGER','order:status:completed:view'),
('PRODUCTION_MANAGER','quality:list'),('PRODUCTION_MANAGER','quality:detail'),('PRODUCTION_MANAGER','quality:create'),
('PRODUCTION_MANAGER','quality:update'),('PRODUCTION_MANAGER','quality:process'),('PRODUCTION_MANAGER','quality:export'),
('PRODUCTION_MANAGER','equipment:list'),('PRODUCTION_MANAGER','equipment:detail'),('PRODUCTION_MANAGER','equipment:inspection:list'),

('WAREHOUSE_STAFF','inventory:list'),('WAREHOUSE_STAFF','inventory:detail'),('WAREHOUSE_STAFF','inventory:barcode:search'),
('WAREHOUSE_STAFF','inventory:model:search'),('WAREHOUSE_STAFF','inventory:cloth:in'),('WAREHOUSE_STAFF','inventory:cloth:out'),
('WAREHOUSE_STAFF','print:receipt:list'),('WAREHOUSE_STAFF','print:receipt:detail'),('WAREHOUSE_STAFF','print:receipt:execute'),
('WAREHOUSE_STAFF','print:label:list'),('WAREHOUSE_STAFF','print:label:detail'),('WAREHOUSE_STAFF','order:list'),
('WAREHOUSE_STAFF','order:detail'),('WAREHOUSE_STAFF','order:scope:tenant'),('WAREHOUSE_STAFF','order:status:pending-material:view'),
('WAREHOUSE_STAFF','order:status:pending-ship:view'),('WAREHOUSE_STAFF','order:status:pending-ship:advance'),
('WAREHOUSE_STAFF','order:status:shipped:view'),

('WAREHOUSE_MANAGER','inventory:list'),('WAREHOUSE_MANAGER','inventory:detail'),('WAREHOUSE_MANAGER','inventory:warning:list'),
('WAREHOUSE_MANAGER','inventory:warning:setting'),('WAREHOUSE_MANAGER','inventory:record:list'),('WAREHOUSE_MANAGER','inventory:trend'),
('WAREHOUSE_MANAGER','inventory:barcode:search'),('WAREHOUSE_MANAGER','inventory:model:search'),('WAREHOUSE_MANAGER','inventory:cloth:in'),
('WAREHOUSE_MANAGER','inventory:cloth:out'),('WAREHOUSE_MANAGER','inventory:import'),('WAREHOUSE_MANAGER','inventory:export'),
('WAREHOUSE_MANAGER','print:receipt:list'),('WAREHOUSE_MANAGER','print:receipt:detail'),('WAREHOUSE_MANAGER','print:receipt:execute'),
('WAREHOUSE_MANAGER','print:receipt:update'),('WAREHOUSE_MANAGER','print:receipt:cancel'),('WAREHOUSE_MANAGER','print:label:list'),
('WAREHOUSE_MANAGER','print:label:detail'),('WAREHOUSE_MANAGER','print:label:create'),('WAREHOUSE_MANAGER','print:label:update'),
('WAREHOUSE_MANAGER','print:label:upload'),('WAREHOUSE_MANAGER','print:label:default'),('WAREHOUSE_MANAGER','print:label:disable'),
('WAREHOUSE_MANAGER','order:list'),('WAREHOUSE_MANAGER','order:detail'),('WAREHOUSE_MANAGER','order:scope:tenant'),
('WAREHOUSE_MANAGER','order:status:pending-material:view'),('WAREHOUSE_MANAGER','order:status:pending-ship:view'),
('WAREHOUSE_MANAGER','order:status:pending-ship:advance'),('WAREHOUSE_MANAGER','order:status:pending-ship:rollback'),
('WAREHOUSE_MANAGER','order:status:shipped:view'),

('QUALITY_STAFF','quality:list'),('QUALITY_STAFF','quality:detail'),('QUALITY_STAFF','quality:create'),
('QUALITY_STAFF','quality:update'),('QUALITY_STAFF','quality:attachment:upload'),('QUALITY_STAFF','quality:attachment:download'),
('QUALITY_MANAGER','quality:list'),('QUALITY_MANAGER','quality:detail'),('QUALITY_MANAGER','quality:create'),
('QUALITY_MANAGER','quality:update'),('QUALITY_MANAGER','quality:process'),('QUALITY_MANAGER','quality:audit'),
('QUALITY_MANAGER','quality:attachment:upload'),('QUALITY_MANAGER','quality:attachment:download'),('QUALITY_MANAGER','quality:export'),
('QUALITY_MANAGER','equipment:list'),('QUALITY_MANAGER','equipment:detail'),('QUALITY_MANAGER','equipment:inspection:list'),

('FINANCE_STAFF','approval:finance:list'),('FINANCE_STAFF','price:list'),('FINANCE_STAFF','price:detail'),
('FINANCE_STAFF','order:list'),('FINANCE_STAFF','order:detail'),('FINANCE_STAFF','order:scope:tenant'),
('FINANCE_STAFF','order:status:pending-pay:view'),('FINANCE_STAFF','order:status:pending-pay:advance'),
('FINANCE_STAFF','order:status:pending-material:view'),
('FINANCE_MANAGER','approval:finance:list'),('FINANCE_MANAGER','approval:finance:audit'),
('FINANCE_MANAGER','price:list'),('FINANCE_MANAGER','price:detail'),('FINANCE_MANAGER','price:create'),
('FINANCE_MANAGER','price:update'),('FINANCE_MANAGER','price:publish'),('FINANCE_MANAGER','price:delete'),
('FINANCE_MANAGER','price:import'),('FINANCE_MANAGER','price:export'),('FINANCE_MANAGER','order:list'),
('FINANCE_MANAGER','order:detail'),('FINANCE_MANAGER','order:scope:tenant'),('FINANCE_MANAGER','order:status:pending-pay:view'),
('FINANCE_MANAGER','order:status:pending-pay:advance'),('FINANCE_MANAGER','order:status:pending-pay:rollback'),
('FINANCE_MANAGER','order:status:pending-material:view'),

('HR_STAFF','employee:list'),('HR_STAFF','employee:detail'),('HR_STAFF','attendance:record:list'),
('HR_STAFF','approval:leave:list'),('HR_STAFF','approval:resignation:list'),
('HR_MANAGER','employee:list'),('HR_MANAGER','employee:detail'),('HR_MANAGER','employee:create'),('HR_MANAGER','employee:update'),
('HR_MANAGER','employee:status'),('HR_MANAGER','employee:delete'),('HR_MANAGER','employee:import'),('HR_MANAGER','employee:export'),
('HR_MANAGER','attendance:record:list'),('HR_MANAGER','attendance:rule:list'),('HR_MANAGER','attendance:rule:update'),
('HR_MANAGER','attendance:export'),('HR_MANAGER','approval:leave:list'),('HR_MANAGER','approval:leave:audit'),
('HR_MANAGER','approval:resignation:list'),('HR_MANAGER','approval:resignation:audit'),('HR_MANAGER','notification:announcement:publish'),

('INSTALLATION_STAFF','installation:list'),('INSTALLATION_STAFF','installation:detail'),('INSTALLATION_STAFF','installation:update'),
('INSTALLATION_STAFF','installation:attachment:upload'),('INSTALLATION_STAFF','installation:attachment:download'),
('INSTALLATION_STAFF','order:list'),('INSTALLATION_STAFF','order:detail'),('INSTALLATION_STAFF','order:scope:assigned'),
('INSTALLATION_STAFF','order:status:shipped:view'),('INSTALLATION_STAFF','order:status:completed:view'),
('INSTALLATION_MANAGER','installation:list'),('INSTALLATION_MANAGER','installation:detail'),('INSTALLATION_MANAGER','installation:update'),
('INSTALLATION_MANAGER','installation:attachment:upload'),('INSTALLATION_MANAGER','installation:attachment:download'),
('INSTALLATION_MANAGER','installation:export'),('INSTALLATION_MANAGER','order:list'),('INSTALLATION_MANAGER','order:detail'),
('INSTALLATION_MANAGER','order:scope:installation:department'),('INSTALLATION_MANAGER','order:status:shipped:view'),
('INSTALLATION_MANAGER','order:status:completed:view'),

('APPROVAL_MANAGER','approval:leave:list'),('APPROVAL_MANAGER','approval:leave:audit'),
('APPROVAL_MANAGER','approval:finance:list'),('APPROVAL_MANAGER','approval:finance:audit'),
('APPROVAL_MANAGER','approval:resignation:list'),('APPROVAL_MANAGER','approval:resignation:audit'),
('APPROVAL_MANAGER','approval:auditor:list'),('APPROVAL_MANAGER','approval:auditor:setting'),
('APPROVAL_MANAGER','order:list'),('APPROVAL_MANAGER','order:detail'),('APPROVAL_MANAGER','order:audit:shipment'),
('APPROVAL_MANAGER','order:audit:cancel'),('APPROVAL_MANAGER','order:scope:tenant'),
('APPROVAL_MANAGER','order:status:pending-ship:view'),('APPROVAL_MANAGER','order:status:pending-cancel:view'),
('APPROVAL_MANAGER','quality:list'),('APPROVAL_MANAGER','quality:detail'),('APPROVAL_MANAGER','quality:audit'),

('DOCUMENT_MANAGER','document:list'),('DOCUMENT_MANAGER','document:folder:create'),('DOCUMENT_MANAGER','document:file:upload'),
('DOCUMENT_MANAGER','document:file:download'),('DOCUMENT_MANAGER','document:rename'),('DOCUMENT_MANAGER','document:move'),
('DOCUMENT_MANAGER','document:delete'),('DOCUMENT_MANAGER','document:export'),

('EQUIPMENT_STAFF','equipment:list'),('EQUIPMENT_STAFF','equipment:detail'),('EQUIPMENT_STAFF','equipment:inspection:submit'),
('EQUIPMENT_MANAGER','equipment:list'),('EQUIPMENT_MANAGER','equipment:detail'),('EQUIPMENT_MANAGER','equipment:create'),
('EQUIPMENT_MANAGER','equipment:update'),('EQUIPMENT_MANAGER','equipment:disable'),('EQUIPMENT_MANAGER','equipment:inspection:list'),
('EQUIPMENT_MANAGER','equipment:inspection:submit'),('EQUIPMENT_MANAGER','equipment:export');

UPDATE sys_role_permission rp
JOIN sys_role r ON r.id = rp.role_id
JOIN builtin_role_v3 b ON BINARY b.role_code = BINARY r.role_code
SET rp.is_deleted = 1
WHERE BINARY r.tenant_code <> BINARY 'super'
  AND r.is_deleted = 0;

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT r.id, p.id, NOW(), 0
FROM sys_role r
JOIN builtin_role_permission_v3 matrix ON BINARY matrix.role_code = BINARY r.role_code
JOIN sys_permission p ON BINARY p.perm_code = BINARY matrix.perm_code
WHERE BINARY r.tenant_code <> BINARY 'super'
  AND r.is_deleted = 0
  AND p.is_deleted = 0
  AND p.status = 1
  AND p.assignable = 1
ON DUPLICATE KEY UPDATE is_deleted = 0;

UPDATE sys_permission p
SET p.assignable = 0,
    p.status = 0,
    p.is_deleted = 1,
    p.update_time = NOW()
WHERE p.is_deleted = 0
  AND (
    p.perm_code IN (
      'sales', 'production', 'dashboard:*', 'notification:announcement:*',
      'order:*', 'order:status:*', 'inventory:*', 'receipt', 'receipt:print:*',
      'label', 'label:template', 'label:template:*', 'badproduct', 'badproduct:*',
      'customer:*', 'price:*', 'approval:*', 'installation:*', 'attendance:*',
      'equipment:*', 'employee:*', 'role:*', 'document:*', 'table', 'table:export'
    )
    OR p.perm_code LIKE 'dashboard:ai%'
    OR (p.perm_code LIKE '%:*' AND p.perm_code NOT LIKE 'platform:%')
    OR p.perm_code IN (
      'customer:page', 'customer:add', 'inventory:record:recent',
      'receipt:print:list', 'receipt:print:detail', 'receipt:print:mark', 'receipt:print:cancel',
      'label:template:list', 'label:template:detail', 'label:template:save',
      'label:template:upload', 'label:template:default', 'label:template:disable',
      'badproduct:list', 'badproduct:save', 'badproduct:process',
      'approval:order:audit', 'document:breadcrumbs'
    )
  )
  AND NOT EXISTS (
    SELECT 1 FROM permission_v3_catalog active_catalog
    WHERE BINARY active_catalog.perm_code = BINARY p.perm_code
  );

UPDATE sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
SET rp.is_deleted = 1
WHERE p.is_deleted = 1 OR p.status = 0 OR p.assignable = 0;

UPDATE sys_user_permission up
JOIN sys_permission p ON p.id = up.permission_id
SET up.is_deleted = 1,
    up.update_time = NOW()
WHERE p.is_deleted = 1 OR p.status = 0 OR p.assignable = 0;

INSERT INTO sys_permission_catalog (id, catalog_version, update_time)
VALUES (1, 3, NOW())
ON DUPLICATE KEY UPDATE catalog_version = 3, update_time = NOW();

UPDATE `user`
SET permission_version = GREATEST(permission_version, 2)
WHERE tenant_code <> 'super';

DROP TEMPORARY TABLE IF EXISTS builtin_role_permission_v3;
DROP TEMPORARY TABLE IF EXISTS builtin_role_v3;
DROP TEMPORARY TABLE IF EXISTS permission_v3_catalog;
