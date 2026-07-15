-- 订单品牌字段：只新增字段和查询索引，不修改已有订单数据。
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
    'brand_name',
    '`brand_name` VARCHAR(120) NULL COMMENT ''品牌'' AFTER `project_name`'
);

CALL hive_add_column_if_missing(
    'production_order',
    'brand_name',
    '`brand_name` VARCHAR(120) NULL COMMENT ''品牌'' AFTER `project_name`'
);

CALL hive_add_index_if_missing(
    'sales_order',
    'idx_sales_order_tenant_brand',
    '(`tenant_code`,`brand_name`)'
);

CALL hive_add_index_if_missing(
    'production_order',
    'idx_production_order_tenant_brand',
    '(`tenant_code`,`brand_name`)'
);

DROP PROCEDURE IF EXISTS hive_add_column_if_missing;
DROP PROCEDURE IF EXISTS hive_add_index_if_missing;
