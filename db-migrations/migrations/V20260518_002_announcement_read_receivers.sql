-- 企业公告已读/未读人员状态查询索引。
-- 只追加索引，不修改已有公告或通知数据。

DROP PROCEDURE IF EXISTS hive_add_index_if_missing;

DELIMITER $$
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

CALL hive_add_index_if_missing(
    'notification_record',
    'idx_notification_announcement_receiver',
    '(`tenant_code`,`biz_type`,`biz_id`,`receiver_user_id`,`read_flag`)'
);

DROP PROCEDURE IF EXISTS hive_add_index_if_missing;
