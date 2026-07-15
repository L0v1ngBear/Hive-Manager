-- 图纸预算订单增加独立未更新预警天数。
-- 只新增配置字段，并以原默认预警天数兜底，不修改订单业务数据。
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
    'drawing_budget_stale_warning_days',
    '`drawing_budget_stale_warning_days` INT NOT NULL DEFAULT 3 COMMENT ''图纸预算订单未更新预警天数'' AFTER `replenishment_stale_warning_days`'
);

UPDATE order_setting
SET drawing_budget_stale_warning_days = IFNULL(drawing_budget_stale_warning_days, stale_warning_days),
    update_time = update_time
WHERE tenant_code IS NOT NULL;

DROP PROCEDURE IF EXISTS hive_add_column_if_missing;
