-- V20260517_004_customer_project_owner.sql
-- Add project owner to customer cooperation projects. Schema-only, preserves existing data.

SET @database_name := DATABASE();

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE customer_project ADD COLUMN project_owner VARCHAR(80) DEFAULT NULL COMMENT ''项目负责人'' AFTER construction_area',
    'SELECT ''customer_project.project_owner exists'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'customer_project'
    AND COLUMN_NAME = 'project_owner'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
