-- Tenant custom page fields 2.0.
-- Structure-only migration: add metadata type and business-row JSON storage without touching existing data.

SET @database_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE table_schema = @database_name AND table_name = 'tenant_field_config' AND column_name = 'field_type') = 0,
  'ALTER TABLE `tenant_field_config` ADD COLUMN `field_type` varchar(30) NOT NULL DEFAULT ''text'' COMMENT ''字段类型：text/number/date/datetime/select'' AFTER `sort_no`',
  'SELECT ''tenant_field_config.field_type exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE table_schema = @database_name AND table_name = 'cloth' AND column_name = 'custom_fields_json') = 0,
  'ALTER TABLE `cloth` ADD COLUMN `custom_fields_json` json DEFAULT NULL COMMENT ''租户自定义库存字段'' AFTER `is_bad`',
  'SELECT ''cloth.custom_fields_json exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
