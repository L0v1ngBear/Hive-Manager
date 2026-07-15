-- Add editable outbound print fields and audit trace.
-- Printing corrections are business records: save them before printing and keep before/after snapshots.

SET @database_name = DATABASE();

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'outbound_order' AND column_name = 'project_name') = 0,
  'ALTER TABLE outbound_order ADD COLUMN project_name varchar(128) DEFAULT NULL COMMENT ''Project name shown on printed receipt'' AFTER customer_name',
  'SELECT ''project_name exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'outbound_order' AND column_name = 'print_date') = 0,
  'ALTER TABLE outbound_order ADD COLUMN print_date date DEFAULT NULL COMMENT ''Date shown on printed receipt'' AFTER project_name',
  'SELECT ''print_date exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'outbound_order' AND column_name = 'logistics_company') = 0,
  'ALTER TABLE outbound_order ADD COLUMN logistics_company varchar(64) DEFAULT NULL COMMENT ''Logistics company shown on printed receipt'' AFTER print_date',
  'SELECT ''logistics_company exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'outbound_order' AND column_name = 'logistics_no') = 0,
  'ALTER TABLE outbound_order ADD COLUMN logistics_no varchar(128) DEFAULT NULL COMMENT ''Logistics number shown on printed receipt'' AFTER logistics_company',
  'SELECT ''logistics_no exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'outbound_order' AND column_name = 'print_operator_name') = 0,
  'ALTER TABLE outbound_order ADD COLUMN print_operator_name varchar(64) DEFAULT NULL COMMENT ''Operator name shown on printed receipt'' AFTER logistics_no',
  'SELECT ''print_operator_name exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'outbound_order' AND column_name = 'print_edit_count') = 0,
  'ALTER TABLE outbound_order ADD COLUMN print_edit_count int NOT NULL DEFAULT 0 COMMENT ''Manual print edit count'' AFTER print_operator_name',
  'SELECT ''print_edit_count exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'outbound_item' AND column_name = 'remark') = 0,
  'ALTER TABLE outbound_item ADD COLUMN remark varchar(255) DEFAULT NULL COMMENT ''Line remark shown on printed receipt'' AFTER total_amount',
  'SELECT ''outbound_item remark exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'outbound_item' AND column_name = 'request_id') = 0,
  'ALTER TABLE outbound_item ADD COLUMN request_id varchar(64) DEFAULT NULL COMMENT ''Idempotent source request id'' AFTER remark',
  'SELECT ''outbound_item request_id exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `outbound_print_edit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_code` varchar(64) NOT NULL COMMENT 'Tenant code',
  `order_id` bigint NOT NULL COMMENT 'Outbound order id',
  `order_no_before` varchar(64) DEFAULT NULL COMMENT 'Order number before edit',
  `order_no_after` varchar(64) DEFAULT NULL COMMENT 'Order number after edit',
  `operator_user_id` bigint DEFAULT NULL COMMENT 'Operator user id',
  `before_json` json DEFAULT NULL COMMENT 'Before edit snapshot',
  `after_json` json DEFAULT NULL COMMENT 'After edit snapshot',
  `edit_reason` varchar(255) DEFAULT NULL COMMENT 'Edit reason',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (`id`),
  KEY `idx_outbound_print_edit_order` (`tenant_code`, `order_id`, `create_time`),
  KEY `idx_outbound_print_edit_order_no` (`tenant_code`, `order_no_after`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Outbound print edit audit log';

UPDATE outbound_order
SET print_date = COALESCE(print_date, DATE(create_time)),
    print_edit_count = COALESCE(print_edit_count, 0),
    update_time = update_time
WHERE print_date IS NULL
   OR print_edit_count IS NULL;
