-- V20260506_001_mini_wechat_standalone_login.sql
-- 支持小程序微信一键登录先创建“未加入组织”的用户，再由用户主动加入组织。

DELIMITER $$

DROP PROCEDURE IF EXISTS hive_modify_user_tenant_nullable $$
CREATE PROCEDURE hive_modify_user_tenant_nullable()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user'
          AND COLUMN_NAME = 'tenant_code'
          AND IS_NULLABLE = 'NO'
    ) THEN
        ALTER TABLE `user`
            MODIFY COLUMN `tenant_code` VARCHAR(50) NULL COMMENT '租户编码，微信一键登录未加入组织时为空';
    END IF;
END $$

DROP PROCEDURE IF EXISTS hive_add_index_if_missing $$
CREATE PROCEDURE hive_add_index_if_missing(
    IN p_table_name VARCHAR(128),
    IN p_index_name VARCHAR(128),
    IN p_index_ddl VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD INDEX `', p_index_name, '` ', p_index_ddl);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

CALL hive_modify_user_tenant_nullable() $$
CALL hive_add_index_if_missing('user', 'idx_user_phone_hash_status', '(`phone_hash`, `status`, `id`)') $$
CALL hive_add_index_if_missing('user', 'idx_user_tenant_phone_hash', '(`tenant_code`, `phone_hash`)') $$

DROP PROCEDURE IF EXISTS hive_modify_user_tenant_nullable $$
DROP PROCEDURE IF EXISTS hive_add_index_if_missing $$

DELIMITER ;
