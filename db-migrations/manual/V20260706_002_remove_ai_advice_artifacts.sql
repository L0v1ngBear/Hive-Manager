-- Remove retired AI advice/commercial assistant artifacts.
-- This is a manually confirmed cleanup file and is not part of automatic migrations.

SET @database_name = DATABASE();

DELETE FROM sys_user_permission
WHERE permission_id IN (
  SELECT id FROM sys_permission WHERE perm_code LIKE 'dashboard:ai%'
);

DELETE FROM sys_role_permission
WHERE permission_id IN (
  SELECT id FROM sys_permission WHERE perm_code LIKE 'dashboard:ai%'
);

DELETE FROM sys_role_permission
WHERE role_id IN (
  SELECT id FROM sys_role WHERE role_code = 'AI_MANAGER'
);

DELETE FROM sys_user_role
WHERE role_id IN (
  SELECT id FROM sys_role WHERE role_code = 'AI_MANAGER'
);

DELETE FROM sys_permission
WHERE perm_code LIKE 'dashboard:ai%';

DELETE FROM sys_role
WHERE role_code = 'AI_MANAGER';

UPDATE tenant
SET feature_flags = JSON_REMOVE(feature_flags, '$.aiAdvice', '$.advancedAi')
WHERE feature_flags IS NOT NULL
  AND JSON_VALID(feature_flags);

DELETE FROM tenant_usage_meter
WHERE meter_type = 'AI_ADVICE';

DROP TABLE IF EXISTS ai_advice_snapshot;
DROP TABLE IF EXISTS ai_advice_training_sample;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @database_name
     AND table_name = 'tenant'
     AND column_name = 'max_ai_advice_per_month') = 1,
  'ALTER TABLE tenant DROP COLUMN max_ai_advice_per_month',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables
   WHERE table_schema = 'xxl_job'
     AND table_name = 'xxl_job_info') = 1,
  'DELETE FROM xxl_job.xxl_job_info WHERE executor_handler = ''aiAdviceSnapshotRefreshJob''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
