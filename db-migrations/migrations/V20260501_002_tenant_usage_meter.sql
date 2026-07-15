-- Durable commercial usage metering.
-- AI advice generation quota is counted here so Redis restarts do not reset paid limits.

CREATE TABLE IF NOT EXISTS `tenant_usage_meter` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_code` VARCHAR(50) NOT NULL COMMENT 'Tenant code',
  `meter_type` VARCHAR(50) NOT NULL COMMENT 'Usage meter type, e.g. AI_ADVICE',
  `period_key` VARCHAR(20) NOT NULL COMMENT 'Billing period, e.g. 202605',
  `used_count` INT NOT NULL DEFAULT 0 COMMENT 'Used count in this period',
  `limit_count` INT DEFAULT NULL COMMENT 'Configured limit in this period',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_usage_meter` (`tenant_code`, `meter_type`, `period_key`),
  KEY `idx_tenant_usage_period` (`meter_type`, `period_key`, `used_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Tenant commercial usage meter';
