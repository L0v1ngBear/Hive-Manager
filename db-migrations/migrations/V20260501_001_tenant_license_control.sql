-- Tenant commercial license foundation.
-- Safe to run repeatedly: columns, indexes and permission seed are created only when missing.

DROP PROCEDURE IF EXISTS hive_add_column_if_missing;
DROP PROCEDURE IF EXISTS hive_add_index_if_missing;

DELIMITER $$
CREATE PROCEDURE hive_add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition VARCHAR(1000)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND column_name = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE hive_add_index_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_columns VARCHAR(1000)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD INDEX `', p_index_name, '` ', p_index_columns);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL hive_add_column_if_missing('tenant', 'package_code', 'VARCHAR(50) NOT NULL DEFAULT ''STANDARD'' COMMENT ''Commercial package code'' AFTER `status`');
CALL hive_add_column_if_missing('tenant', 'package_name', 'VARCHAR(50) NOT NULL DEFAULT ''Standard'' COMMENT ''Commercial package name'' AFTER `package_code`');
CALL hive_add_column_if_missing('tenant', 'subscription_status', 'VARCHAR(30) NOT NULL DEFAULT ''ACTIVE'' COMMENT ''Subscription status: TRIAL/ACTIVE/EXPIRED/SUSPENDED'' AFTER `package_name`');
CALL hive_add_column_if_missing('tenant', 'subscription_start_time', 'DATETIME DEFAULT NULL COMMENT ''Subscription start time'' AFTER `subscription_status`');
CALL hive_add_column_if_missing('tenant', 'subscription_end_time', 'DATETIME DEFAULT NULL COMMENT ''Subscription end time'' AFTER `subscription_start_time`');
CALL hive_add_column_if_missing('tenant', 'max_users', 'INT NOT NULL DEFAULT 30 COMMENT ''Maximum active employee users'' AFTER `subscription_end_time`');
CALL hive_add_column_if_missing('tenant', 'max_ai_advice_per_month', 'INT NOT NULL DEFAULT 300 COMMENT ''Monthly AI advice quota'' AFTER `max_users`');
CALL hive_add_column_if_missing('tenant', 'max_storage_mb', 'INT NOT NULL DEFAULT 5120 COMMENT ''Storage quota in MB'' AFTER `max_ai_advice_per_month`');
CALL hive_add_column_if_missing('tenant', 'feature_flags', 'JSON DEFAULT NULL COMMENT ''Feature switches in JSON'' AFTER `max_storage_mb`');

UPDATE tenant
SET package_code = COALESCE(NULLIF(package_code, ''), 'STANDARD'),
    package_name = COALESCE(NULLIF(package_name, ''), 'Standard'),
    subscription_status = COALESCE(NULLIF(subscription_status, ''), 'ACTIVE'),
    subscription_start_time = COALESCE(subscription_start_time, create_time, NOW()),
    subscription_end_time = COALESCE(subscription_end_time, DATE_ADD(NOW(), INTERVAL 1 YEAR)),
    max_users = CASE WHEN max_users IS NULL OR max_users < 0 THEN 30 ELSE max_users END,
    max_ai_advice_per_month = CASE WHEN max_ai_advice_per_month IS NULL OR max_ai_advice_per_month < 0 THEN 300 ELSE max_ai_advice_per_month END,
    max_storage_mb = CASE WHEN max_storage_mb IS NULL OR max_storage_mb < 0 THEN 5120 ELSE max_storage_mb END,
    feature_flags = COALESCE(feature_flags, JSON_OBJECT('aiAdvice', TRUE, 'advancedAi', FALSE))
WHERE deleted = 0;

CALL hive_add_index_if_missing('tenant', 'idx_tenant_subscription', '(`status`, `subscription_status`, `subscription_end_time`)');
CALL hive_add_index_if_missing('tenant', 'idx_tenant_package', '(`package_code`)');

INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_type`, `sort`, `perm_name`, `is_deleted`)
SELECT parent.`id`, 'platform:tenant:license', 3, 1113, 'Tenant License', 0
FROM `sys_permission` parent
WHERE parent.`perm_code` = 'platform:tenant'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_permission` existed
    WHERE existed.`perm_code` = 'platform:tenant:license'
  );

DROP PROCEDURE IF EXISTS hive_add_column_if_missing;
DROP PROCEDURE IF EXISTS hive_add_index_if_missing;
