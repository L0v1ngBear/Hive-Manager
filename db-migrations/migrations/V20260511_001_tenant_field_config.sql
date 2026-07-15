-- Tenant field customization foundation.
-- This migration only creates a structure table and does not modify existing business data.

CREATE TABLE IF NOT EXISTS `tenant_field_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` VARCHAR(50) NOT NULL COMMENT '租户编码',
  `module_code` VARCHAR(64) NOT NULL COMMENT '模块编码，如 inventory/receipt/employee',
  `field_key` VARCHAR(80) NOT NULL COMMENT '字段编码',
  `field_label` VARCHAR(80) NOT NULL COMMENT '租户侧字段名称',
  `visible_flag` TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示：0否 1是',
  `required_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '是否必填：0否 1是',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `options_json` JSON DEFAULT NULL COMMENT '字段选项配置',
  `remark` VARCHAR(300) DEFAULT NULL COMMENT '配置备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_field_config` (`tenant_code`, `module_code`, `field_key`),
  KEY `idx_tenant_field_config_module` (`tenant_code`, `module_code`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户字段级定制配置';
