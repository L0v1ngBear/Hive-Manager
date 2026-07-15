-- V20260622_003_approval_default_auditor_multi.sql
-- Allow each approval type to have multiple default approvers while keeping auditor_id as the compatible primary approver.

SET @database_name := DATABASE();

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE approval_default_auditor ADD COLUMN auditor_ids VARCHAR(500) DEFAULT NULL COMMENT ''default approver id list, comma separated'' AFTER auditor_id',
    'SELECT ''approval_default_auditor auditor_ids exists'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'approval_default_auditor'
    AND COLUMN_NAME = 'auditor_ids'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
