-- V20260519_002_equipment_inspection.sql
-- Purpose:
-- 1. Add fixed equipment inspection QR-code workflow.
-- 2. Keep QR payload stable so every equipment code only needs to be printed once.
-- 3. Seed management and mini-program permissions, and grant safe mini permissions to the built-in EMPLOYEE role.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `equipment_device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户编码',
  `equipment_code` VARCHAR(80) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备固定巡检码',
  `equipment_name` VARCHAR(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备名称',
  `equipment_type` VARCHAR(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备类型',
  `location` VARCHAR(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备位置',
  `responsible_person` VARCHAR(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '负责人',
  `inspection_cycle_days` INT NOT NULL DEFAULT 7 COMMENT '巡检周期天数',
  `last_inspection_time` DATETIME DEFAULT NULL COMMENT '最近巡检时间',
  `status` VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled/disabled',
  `remark` VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_equipment_tenant_code` (`tenant_code`, `equipment_code`),
  KEY `idx_equipment_tenant_status_update` (`tenant_code`, `status`, `update_time`),
  KEY `idx_equipment_tenant_name` (`tenant_code`, `equipment_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备档案表';

CREATE TABLE IF NOT EXISTS `equipment_inspection_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户编码',
  `equipment_id` BIGINT NOT NULL COMMENT '设备ID',
  `equipment_code` VARCHAR(80) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备固定巡检码',
  `equipment_name` VARCHAR(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备名称快照',
  `inspection_result` VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'normal' COMMENT '巡检结果：normal/abnormal',
  `abnormal_desc` VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '异常说明',
  `photo_url` VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '现场图片',
  `remark` VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `inspector_user_id` BIGINT DEFAULT NULL COMMENT '巡检人用户ID',
  `inspector_name` VARCHAR(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '巡检人姓名',
  `inspection_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '巡检时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_equip_record_tenant_equipment_time` (`tenant_code`, `equipment_id`, `inspection_time`),
  KEY `idx_equip_record_tenant_code_time` (`tenant_code`, `equipment_code`, `inspection_time`),
  KEY `idx_equip_record_tenant_result_time` (`tenant_code`, `inspection_result`, `inspection_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备巡检记录表';

INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_type`, `sort`, `perm_name`, `create_time`, `update_time`, `is_deleted`)
SELECT 0, 'equipment', 1, 2100, '设备巡检', NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission` existed
    WHERE BINARY existed.`perm_code` = BINARY 'equipment'
);

UPDATE `sys_permission`
SET `parent_id` = 0,
    `perm_type` = 1,
    `sort` = 2100,
    `perm_name` = '设备巡检',
    `update_time` = NOW(),
    `is_deleted` = 0
WHERE BINARY `perm_code` = BINARY 'equipment';

INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_type`, `sort`, `perm_name`, `create_time`, `update_time`, `is_deleted`)
SELECT parent.`id`, seed.`perm_code`, 3, seed.`sort_no`, seed.`perm_name`, NOW(), NOW(), 0
FROM `sys_permission` parent
JOIN (
    SELECT 'equipment:list' AS perm_code, 1 AS sort_no, '查看设备档案' AS perm_name
    UNION ALL SELECT 'equipment:detail', 2, '查看设备详情'
    UNION ALL SELECT 'equipment:save', 3, '维护设备档案'
    UNION ALL SELECT 'equipment:inspection:list', 4, '查看巡检记录'
    UNION ALL SELECT 'equipment:inspection:submit', 5, '提交巡检记录'
) seed
WHERE BINARY parent.`perm_code` = BINARY 'equipment'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_permission` existed
      WHERE BINARY existed.`perm_code` = BINARY seed.`perm_code`
  );

UPDATE `sys_permission`
SET `is_deleted` = 0,
    `update_time` = NOW()
WHERE BINARY `perm_code` = BINARY 'equipment:list'
   OR BINARY `perm_code` = BINARY 'equipment:detail'
   OR BINARY `perm_code` = BINARY 'equipment:save'
   OR BINARY `perm_code` = BINARY 'equipment:inspection:list'
   OR BINARY `perm_code` = BINARY 'equipment:inspection:submit';

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`, `create_time`, `is_deleted`)
SELECT source.`role_id`, target_permission.`id`, NOW(), 0
FROM (
    SELECT DISTINCT rp.`role_id`
    FROM `sys_role_permission` rp
    INNER JOIN `sys_permission` owned_permission
      ON owned_permission.`id` = rp.`permission_id`
     AND IFNULL(owned_permission.`is_deleted`, 0) = 0
    WHERE IFNULL(rp.`is_deleted`, 0) = 0
      AND (
          BINARY owned_permission.`perm_code` = BINARY '*'
          OR BINARY owned_permission.`perm_code` = BINARY '*:*'
          OR BINARY owned_permission.`perm_code` = BINARY 'equipment'
      )
) source
INNER JOIN `sys_permission` target_permission
  ON IFNULL(target_permission.`is_deleted`, 0) = 0
 AND (
      BINARY target_permission.`perm_code` = BINARY 'equipment:list'
      OR BINARY target_permission.`perm_code` = BINARY 'equipment:detail'
      OR BINARY target_permission.`perm_code` = BINARY 'equipment:save'
      OR BINARY target_permission.`perm_code` = BINARY 'equipment:inspection:list'
      OR BINARY target_permission.`perm_code` = BINARY 'equipment:inspection:submit'
 )
ON DUPLICATE KEY UPDATE `is_deleted` = 0;

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`, `create_time`, `is_deleted`)
SELECT r.`id`, p.`id`, NOW(), 0
FROM `sys_role` r
INNER JOIN `sys_permission` p
  ON IFNULL(p.`is_deleted`, 0) = 0
 AND (
      BINARY p.`perm_code` = BINARY 'equipment:list'
      OR BINARY p.`perm_code` = BINARY 'equipment:inspection:submit'
 )
WHERE BINARY r.`role_code` = BINARY 'EMPLOYEE'
  AND IFNULL(r.`is_deleted`, 0) = 0
ON DUPLICATE KEY UPDATE `is_deleted` = 0;
