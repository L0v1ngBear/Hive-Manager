-- Add configurable AND/OR semantics to approval defaults and snapshot the rule on each approval candidate set.
-- Existing configurations and in-flight approvals remain AND for backward compatibility.

SET @database_name := DATABASE();

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE approval_default_auditor ADD COLUMN approval_mode VARCHAR(8) NOT NULL DEFAULT ''AND'' COMMENT ''AND all approve, OR any approves'' AFTER auditor_ids',
    'SELECT ''approval_default_auditor approval_mode exists'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'approval_default_auditor'
    AND COLUMN_NAME = 'approval_mode'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE approval_auditor_candidate ADD COLUMN approval_mode VARCHAR(8) NOT NULL DEFAULT ''AND'' COMMENT ''snapshot: AND all approve, OR any approves'' AFTER auditor_id',
    'SELECT ''approval_auditor_candidate approval_mode exists'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'approval_auditor_candidate'
    AND COLUMN_NAME = 'approval_mode'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE approval_default_auditor
SET approval_mode = 'AND'
WHERE approval_mode IS NULL OR approval_mode NOT IN ('AND', 'OR');

UPDATE approval_auditor_candidate
SET approval_mode = 'AND'
WHERE approval_mode IS NULL OR approval_mode NOT IN ('AND', 'OR');
