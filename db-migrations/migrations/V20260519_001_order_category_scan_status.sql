-- V20260519_001 订单小项类型与扫码流转支撑字段
-- 只新增字段和索引，不删除、不覆盖已有业务数据；历史订单默认归入 bulk（大货）。

DROP PROCEDURE IF EXISTS hive_add_column_if_missing;
DROP PROCEDURE IF EXISTS hive_add_index_if_missing;

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

CALL hive_add_column_if_missing(
    'sales_order',
    'order_category',
    '`order_category` VARCHAR(32) NOT NULL DEFAULT ''bulk'' COMMENT ''订单小项：sample_room-样板间，bulk-大货，replenishment-补单'' AFTER `status`'
);

CALL hive_add_column_if_missing(
    'production_order',
    'order_category',
    '`order_category` VARCHAR(32) NOT NULL DEFAULT ''bulk'' COMMENT ''订单小项：sample_room-样板间，bulk-大货，replenishment-补单'' AFTER `status`'
);

CALL hive_add_index_if_missing(
    'sales_order',
    'idx_sales_order_tenant_category_status',
    '(`tenant_code`,`order_category`,`status`,`update_time`)'
);

CALL hive_add_index_if_missing(
    'production_order',
    'idx_production_order_tenant_category_status',
    '(`tenant_code`,`order_category`,`status`,`update_time`)'
);

DROP PROCEDURE IF EXISTS hive_add_column_if_missing;
DROP PROCEDURE IF EXISTS hive_add_index_if_missing;
