-- V20260518_001_parallel_approval_auditors.sql
-- Add parallel approval candidates for leave, finance, and resignation approvals.
-- Existing rows are intentionally not backfilled; only new approval flows use this structure.

CREATE TABLE IF NOT EXISTS approval_auditor_candidate (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  tenant_code VARCHAR(50) NOT NULL COMMENT 'Tenant code',
  approval_type VARCHAR(30) NOT NULL COMMENT 'Approval type: LEAVE/FINANCE/RESIGNATION',
  approval_code VARCHAR(64) NOT NULL COMMENT 'Approval business code',
  auditor_id BIGINT NOT NULL COMMENT 'Candidate auditor user id',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 active, 2 closed',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (id),
  KEY idx_approval_auditor_user (tenant_code, auditor_id, status, approval_type),
  KEY idx_approval_auditor_biz (tenant_code, approval_type, approval_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Parallel approval auditor candidates';

SET @database_name := DATABASE();

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE user_leave ADD COLUMN auditor_ids VARCHAR(500) DEFAULT NULL COMMENT ''当前候选审批人ID列表'' AFTER auditor_id',
    'SELECT ''user_leave auditor_ids exists'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'user_leave'
    AND COLUMN_NAME = 'auditor_ids'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE finance_approval ADD COLUMN auditor_ids VARCHAR(500) DEFAULT NULL COMMENT ''当前候选审批人ID列表'' AFTER auditor_id',
    'SELECT ''finance_approval auditor_ids exists'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'finance_approval'
    AND COLUMN_NAME = 'auditor_ids'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE employee_resignation_approval ADD COLUMN auditor_ids VARCHAR(500) DEFAULT NULL COMMENT ''当前候选审批人ID列表'' AFTER auditor_id',
    'SELECT ''employee_resignation_approval auditor_ids exists'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'employee_resignation_approval'
    AND COLUMN_NAME = 'auditor_ids'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
