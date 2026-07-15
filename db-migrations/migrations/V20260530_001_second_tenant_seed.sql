-- Seed the second bounded tenant.
-- Idempotent and data-safe: creates missing baseline records only, then restores disabled baseline records.
SET NAMES utf8mb4;

INSERT INTO `tenant` (
    `tenant_code`, `tenant_name`, `tenant_type`, `contact_person`, `contact_phone`, `password`,
    `status`, `package_code`, `package_name`, `subscription_status`, `subscription_start_time`,
    `subscription_end_time`, `max_users`, `max_ai_advice_per_month`, `max_storage_mb`,
    `feature_flags`, `creator`, `deleted`
)
SELECT
    'TENANT_002', '第二业务主体', 1, '管理员', '13900020001', NULL,
    1, 'STANDARD', '标准版', 'ACTIVE', NOW(),
    DATE_ADD(NOW(), INTERVAL 1 YEAR), 80, 600, 10240,
    JSON_OBJECT(
        'aiAdvice', true,
        'advancedAi', true,
        'document', true,
        'inventory', true,
        'salesOrder', true,
        'productionOrder', true,
        'attendance', true,
        'approval', true,
        'equipmentInspection', true,
        'labelPrint', true
    ),
    NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM `tenant` existed
    WHERE BINARY existed.`tenant_code` = BINARY 'TENANT_002'
);

UPDATE `tenant`
SET `status` = 1,
    `subscription_status` = 'ACTIVE',
    `deleted` = 0,
    `update_time` = NOW()
WHERE BINARY `tenant_code` = BINARY 'TENANT_002';

INSERT INTO `user` (
    `tenant_code`, `name`, `login_name`, `phone`, `password`, `must_change_password`,
    `department_name`, `position`, `manager_id`, `manager_name`, `status`, `role_level`,
    `phone_hash`, `phone_mask`
)
SELECT
    'TENANT_002', '第二租户管理员', 'tenant2_admin', '13900020001',
    '$2a$10$IVlMKrR4CnyWG.IwSnLAtOhJFRnU9fibb0tXEVSpE9XUfhF.AeoEq',
    1, '管理部', '系统管理员', NULL, NULL, 1, 3,
    LOWER(SHA2('13900020001', 256)), '139****0001'
WHERE NOT EXISTS (
    SELECT 1 FROM `user` existed
    WHERE BINARY existed.`tenant_code` = BINARY 'TENANT_002'
      AND BINARY existed.`login_name` = BINARY 'tenant2_admin'
);

UPDATE `user`
SET `name` = '第二租户管理员',
    `department_name` = '管理部',
    `position` = '系统管理员',
    `status` = 1,
    `role_level` = 3,
    `phone_hash` = COALESCE(NULLIF(`phone_hash`, ''), LOWER(SHA2('13900020001', 256))),
    `phone_mask` = COALESCE(NULLIF(`phone_mask`, ''), '139****0001'),
    `update_time` = NOW()
WHERE BINARY `tenant_code` = BINARY 'TENANT_002'
  AND BINARY `login_name` = BINARY 'tenant2_admin';

INSERT INTO `emp_department` (
    `tenant_code`, `dept_name`, `dept_code`, `parent_id`, `leader_name`,
    `sort_no`, `status`, `is_deleted`, `create_time`, `update_time`
)
SELECT 'TENANT_002', '管理部', 'D001', NULL, '第二租户管理员', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM `emp_department` existed
    WHERE BINARY existed.`tenant_code` = BINARY 'TENANT_002'
      AND BINARY existed.`dept_code` = BINARY 'D001'
);

UPDATE `emp_department`
SET `dept_name` = '管理部',
    `leader_name` = COALESCE(NULLIF(`leader_name`, ''), '第二租户管理员'),
    `status` = 1,
    `is_deleted` = 0,
    `update_time` = NOW()
WHERE BINARY `tenant_code` = BINARY 'TENANT_002'
  AND BINARY `dept_code` = BINARY 'D001';

INSERT INTO `emp_position` (
    `tenant_code`, `position_name`, `position_code`, `department_id`,
    `sort_no`, `status`, `is_deleted`, `create_time`, `update_time`
)
SELECT 'TENANT_002', '系统管理员', 'P001', d.`id`, 1, 1, 0, NOW(), NOW()
FROM `emp_department` d
WHERE BINARY d.`tenant_code` = BINARY 'TENANT_002'
  AND BINARY d.`dept_code` = BINARY 'D001'
  AND IFNULL(d.`is_deleted`, 0) = 0
  AND NOT EXISTS (
      SELECT 1 FROM `emp_position` existed
      WHERE BINARY existed.`tenant_code` = BINARY 'TENANT_002'
        AND BINARY existed.`position_code` = BINARY 'P001'
  );

UPDATE `emp_position` p
JOIN `emp_department` d
  ON BINARY d.`tenant_code` = BINARY p.`tenant_code`
 AND BINARY d.`dept_code` = BINARY 'D001'
 AND IFNULL(d.`is_deleted`, 0) = 0
SET p.`position_name` = '系统管理员',
    p.`department_id` = d.`id`,
    p.`status` = 1,
    p.`is_deleted` = 0,
    p.`update_time` = NOW()
WHERE BINARY p.`tenant_code` = BINARY 'TENANT_002'
  AND BINARY p.`position_code` = BINARY 'P001';

INSERT INTO `emp_employee_ext` (
    `user_id`, `tenant_code`, `emp_no`, `email`, `employee_type`,
    `entry_date`, `avatar_url`, `remark`, `is_deleted`, `create_time`, `update_time`
)
SELECT u.`id`, 'TENANT_002', 'EMP9901', NULL, 'FULL_TIME',
       CURDATE(), NULL, '第二租户初始管理员', 0, NOW(), NOW()
FROM `user` u
WHERE BINARY u.`tenant_code` = BINARY 'TENANT_002'
  AND BINARY u.`login_name` = BINARY 'tenant2_admin'
  AND NOT EXISTS (
      SELECT 1 FROM `emp_employee_ext` existed
      WHERE existed.`user_id` = u.`id`
  );

UPDATE `emp_employee_ext` ext
JOIN `user` u ON u.`id` = ext.`user_id`
SET ext.`tenant_code` = 'TENANT_002',
    ext.`emp_no` = COALESCE(NULLIF(ext.`emp_no`, ''), 'EMP9901'),
    ext.`employee_type` = COALESCE(NULLIF(ext.`employee_type`, ''), 'FULL_TIME'),
    ext.`entry_date` = COALESCE(ext.`entry_date`, CURDATE()),
    ext.`remark` = COALESCE(NULLIF(ext.`remark`, ''), '第二租户初始管理员'),
    ext.`is_deleted` = 0,
    ext.`update_time` = NOW()
WHERE BINARY u.`tenant_code` = BINARY 'TENANT_002'
  AND BINARY u.`login_name` = BINARY 'tenant2_admin';

INSERT INTO `sys_role` (`tenant_code`, `role_code`, `role_name`, `is_system`, `create_time`, `update_time`, `is_deleted`)
SELECT 'TENANT_002', role_seed.`role_code`, role_seed.`role_name`, 1, NOW(), NOW(), 0
FROM (
    SELECT 'ADMIN' AS `role_code`, '系统管理员' AS `role_name`
    UNION ALL SELECT 'EMPLOYEE', '普通员工'
) role_seed
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role` existed
    WHERE BINARY existed.`tenant_code` = BINARY 'TENANT_002'
      AND BINARY existed.`role_code` = BINARY role_seed.`role_code`
      AND IFNULL(existed.`is_deleted`, 0) = 0
);

UPDATE `sys_role`
SET `role_name` = CASE
        WHEN BINARY `role_code` = BINARY 'ADMIN' THEN '系统管理员'
        WHEN BINARY `role_code` = BINARY 'EMPLOYEE' THEN '普通员工'
        ELSE `role_name`
    END,
    `is_system` = 1,
    `is_deleted` = 0,
    `update_time` = NOW()
WHERE BINARY `tenant_code` = BINARY 'TENANT_002'
  AND BINARY `role_code` IN (BINARY 'ADMIN', BINARY 'EMPLOYEE');

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`, `create_time`, `is_deleted`)
SELECT r.`id`, p.`id`, NOW(), 0
FROM `sys_role` r
JOIN `sys_permission` p
WHERE BINARY r.`tenant_code` = BINARY 'TENANT_002'
  AND BINARY r.`role_code` = BINARY 'ADMIN'
  AND IFNULL(r.`is_deleted`, 0) = 0
  AND IFNULL(p.`is_deleted`, 0) = 0
  AND BINARY p.`perm_code` NOT IN (BINARY 'super', BINARY 'developer:super')
ON DUPLICATE KEY UPDATE `is_deleted` = 0;

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`, `create_time`, `is_deleted`)
SELECT employee_role.`id`, p.`id`, NOW(), 0
FROM `sys_role` employee_role
JOIN `sys_permission` p
WHERE BINARY employee_role.`tenant_code` = BINARY 'TENANT_002'
  AND BINARY employee_role.`role_code` = BINARY 'EMPLOYEE'
  AND IFNULL(employee_role.`is_deleted`, 0) = 0
  AND IFNULL(p.`is_deleted`, 0) = 0
  AND BINARY p.`perm_code` IN (
      BINARY 'attendance:punch',
      BINARY 'attendance:record:list',
      BINARY 'approval:leave:submit',
      BINARY 'approval:leave:detail',
      BINARY 'approval:finance:submit',
      BINARY 'approval:finance:detail',
      BINARY 'approval:resignation:submit',
      BINARY 'approval:resignation:detail',
      BINARY 'equipment:list',
      BINARY 'equipment:detail',
      BINARY 'equipment:inspection:submit',
      BINARY 'document:list',
      BINARY 'document:breadcrumbs',
      BINARY 'notification:announcement:list'
  )
ON DUPLICATE KEY UPDATE `is_deleted` = 0;

INSERT INTO `sys_user_role` (`user_id`, `tenant_code`, `role_id`, `create_time`, `is_deleted`)
SELECT u.`id`, 'TENANT_002', r.`id`, NOW(), 0
FROM `user` u
JOIN `sys_role` r
  ON BINARY r.`tenant_code` = BINARY u.`tenant_code`
 AND BINARY r.`role_code` = BINARY 'ADMIN'
 AND IFNULL(r.`is_deleted`, 0) = 0
WHERE BINARY u.`tenant_code` = BINARY 'TENANT_002'
  AND BINARY u.`login_name` = BINARY 'tenant2_admin'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_user_role` existed
      WHERE existed.`user_id` = u.`id`
        AND BINARY existed.`tenant_code` = BINARY 'TENANT_002'
        AND existed.`role_id` = r.`id`
        AND IFNULL(existed.`is_deleted`, 0) = 0
  );

UPDATE `sys_user_role` ur
JOIN `user` u ON u.`id` = ur.`user_id`
JOIN `sys_role` r ON r.`id` = ur.`role_id`
SET ur.`is_deleted` = 0
WHERE BINARY u.`tenant_code` = BINARY 'TENANT_002'
  AND BINARY u.`login_name` = BINARY 'tenant2_admin'
  AND BINARY ur.`tenant_code` = BINARY 'TENANT_002'
  AND BINARY r.`role_code` = BINARY 'ADMIN';

INSERT INTO `tenant_attendance_rule` (
    `tenant_code`, `tenant_name`, `status`, `latitude`, `longitude`, `address`, `radius`,
    `work_start_time`, `work_end_time`, `off_work_start_time`, `off_work_end_time`,
    `over_time_start_time`, `over_time_end_time`, `late_tolerance_minutes`,
    `early_tolerance_minutes`, `work_days`, `enable_gps`, `enable_wifi`, `wifi_ssid`
)
SELECT 'TENANT_002', '第二业务主体', 0, NULL, NULL, NULL, 300,
       '09:00:00', '18:00:00', '17:30:00', '18:30:00',
       '18:30:00', '22:00:00', 0, 0, '1,2,3,4,5', 1, 0, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM `tenant_attendance_rule` existed
    WHERE BINARY existed.`tenant_code` = BINARY 'TENANT_002'
);

UPDATE `tenant_attendance_rule`
SET `tenant_name` = COALESCE(NULLIF(`tenant_name`, ''), '第二业务主体'),
    `radius` = COALESCE(`radius`, 300)
WHERE BINARY `tenant_code` = BINARY 'TENANT_002';
