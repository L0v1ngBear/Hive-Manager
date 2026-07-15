-- 订单未更新预警拆分为样板间/大货/补单三类设置。
-- 只新增配置字段并以原 stale_warning_days 作为默认值，不修改业务订单数据。
DROP PROCEDURE IF EXISTS hive_add_column_if_missing;

DELIMITER $$
CREATE PROCEDURE hive_add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_ddl TEXT
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
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_column_ddl);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL hive_add_column_if_missing(
    'order_setting',
    'sample_room_stale_warning_days',
    '`sample_room_stale_warning_days` INT NOT NULL DEFAULT 3 COMMENT ''样板间订单未更新预警天数'' AFTER `stale_warning_days`'
);

CALL hive_add_column_if_missing(
    'order_setting',
    'bulk_stale_warning_days',
    '`bulk_stale_warning_days` INT NOT NULL DEFAULT 3 COMMENT ''大货订单未更新预警天数'' AFTER `sample_room_stale_warning_days`'
);

CALL hive_add_column_if_missing(
    'order_setting',
    'replenishment_stale_warning_days',
    '`replenishment_stale_warning_days` INT NOT NULL DEFAULT 3 COMMENT ''补单订单未更新预警天数'' AFTER `bulk_stale_warning_days`'
);

UPDATE order_setting
SET sample_room_stale_warning_days = IFNULL(sample_room_stale_warning_days, stale_warning_days),
    bulk_stale_warning_days = IFNULL(bulk_stale_warning_days, stale_warning_days),
    replenishment_stale_warning_days = IFNULL(replenishment_stale_warning_days, stale_warning_days),
    update_time = update_time
WHERE tenant_code IS NOT NULL;

DROP PROCEDURE IF EXISTS hive_add_column_if_missing;
