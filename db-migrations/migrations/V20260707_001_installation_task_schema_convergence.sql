-- Forward-only convergence for installation_task.
-- Do not edit V20260705_004 after it has run online.

SET @database_name = DATABASE();

CREATE TABLE IF NOT EXISTS `installation_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(64) NOT NULL,
  `order_id` varchar(64) NOT NULL,
  `order_status` varchar(40) NOT NULL DEFAULT 'completed',
  `installation_status` varchar(40) NOT NULL DEFAULT 'production_completed',
  `customer_name` varchar(120) DEFAULT NULL,
  `customer_phone` varchar(40) DEFAULT NULL,
  `project_name` varchar(160) DEFAULT NULL,
  `brand_name` varchar(120) DEFAULT NULL,
  `order_category` varchar(40) DEFAULT NULL,
  `goods_desc` varchar(500) DEFAULT NULL,
  `total_quantity` int DEFAULT NULL,
  `delivery_date` varchar(40) DEFAULT NULL,
  `express_company` varchar(120) DEFAULT NULL,
  `express_no` varchar(120) DEFAULT NULL,
  `is_invoice` tinyint NOT NULL DEFAULT 0,
  `creator` varchar(80) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `order_attachment_name` varchar(255) DEFAULT NULL,
  `order_attachment_url` varchar(500) DEFAULT NULL,
  `order_attachment_size` bigint DEFAULT NULL,
  `construction_personnel` varchar(120) DEFAULT NULL,
  `construction_phone` varchar(40) DEFAULT NULL,
  `construction_remark` varchar(500) DEFAULT NULL,
  `special_exception_note` varchar(1000) DEFAULT NULL,
  `attachment_name` varchar(255) DEFAULT NULL,
  `attachment_url` varchar(500) DEFAULT NULL,
  `attachment_size` bigint DEFAULT NULL,
  `order_completed_time` datetime DEFAULT NULL,
  `accepted_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_installation_task_order` (`tenant_code`, `order_id`),
  KEY `idx_installation_task_status` (`tenant_code`, `installation_status`, `update_time`),
  KEY `idx_installation_task_customer` (`tenant_code`, `customer_name`, `project_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'installation_task' AND column_name = 'express_company') = 0,
  'ALTER TABLE installation_task ADD COLUMN express_company varchar(120) DEFAULT NULL AFTER delivery_date',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'installation_task' AND column_name = 'express_no') = 0,
  'ALTER TABLE installation_task ADD COLUMN express_no varchar(120) DEFAULT NULL AFTER express_company',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'installation_task' AND column_name = 'construction_personnel') = 0,
  'ALTER TABLE installation_task ADD COLUMN construction_personnel varchar(120) DEFAULT NULL AFTER order_attachment_size',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'installation_task' AND column_name = 'construction_phone') = 0,
  'ALTER TABLE installation_task ADD COLUMN construction_phone varchar(40) DEFAULT NULL AFTER construction_personnel',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'installation_task' AND column_name = 'construction_remark') = 0,
  'ALTER TABLE installation_task ADD COLUMN construction_remark varchar(500) DEFAULT NULL AFTER construction_phone',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'installation_task' AND column_name = 'special_exception_note') = 0,
  'ALTER TABLE installation_task ADD COLUMN special_exception_note varchar(1000) DEFAULT NULL AFTER construction_remark',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @database_name AND table_name = 'installation_task' AND index_name = 'idx_installation_task_status') = 0,
  'ALTER TABLE installation_task ADD INDEX idx_installation_task_status (tenant_code, installation_status, update_time)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @database_name AND table_name = 'installation_task' AND index_name = 'idx_installation_task_customer') = 0,
  'ALTER TABLE installation_task ADD INDEX idx_installation_task_customer (tenant_code, customer_name, project_name)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
