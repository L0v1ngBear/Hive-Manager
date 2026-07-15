-- V20260605_001_approval_auditor_decisions.sql
-- Track each selected approver's own decision in approval_auditor_candidate.

SET @database_name := DATABASE();

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE approval_auditor_candidate ADD COLUMN audit_status TINYINT NOT NULL DEFAULT 0 COMMENT ''0 pending, 1 approved, 2 rejected'' AFTER status',
    'SELECT ''approval_auditor_candidate audit_status exists'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'approval_auditor_candidate'
    AND COLUMN_NAME = 'audit_status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE approval_auditor_candidate ADD COLUMN audit_comment VARCHAR(300) DEFAULT NULL COMMENT ''approver comment'' AFTER audit_status',
    'SELECT ''approval_auditor_candidate audit_comment exists'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'approval_auditor_candidate'
    AND COLUMN_NAME = 'audit_comment'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE approval_auditor_candidate ADD COLUMN audit_time DATETIME DEFAULT NULL COMMENT ''approver decision time'' AFTER audit_comment',
    'SELECT ''approval_auditor_candidate audit_time exists'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'approval_auditor_candidate'
    AND COLUMN_NAME = 'audit_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'CREATE INDEX idx_approval_auditor_pending ON approval_auditor_candidate (tenant_code, approval_type, approval_code, status, audit_status, auditor_id)',
    'SELECT ''idx_approval_auditor_pending exists'''
  )
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'approval_auditor_candidate'
    AND INDEX_NAME = 'idx_approval_auditor_pending'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
