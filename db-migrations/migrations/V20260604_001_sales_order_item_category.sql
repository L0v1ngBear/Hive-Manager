-- Convert sales order item weight into business category text.
-- Do not edit historical migrations; this file is the forward schema change.

SET @sales_order_detail_weight_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sales_order_detail'
      AND COLUMN_NAME = 'weight'
);

SET @sales_order_detail_weight_is_text := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sales_order_detail'
      AND COLUMN_NAME = 'weight'
      AND DATA_TYPE IN ('char', 'varchar', 'tinytext', 'text', 'mediumtext', 'longtext')
);

SET @sales_order_detail_weight_sql := IF(
    @sales_order_detail_weight_exists = 1 AND @sales_order_detail_weight_is_text = 0,
    'ALTER TABLE sales_order_detail MODIFY COLUMN weight VARCHAR(64) NULL COMMENT ''商品类别''',
    'SELECT ''sales_order_detail.weight already text or missing'''
);

PREPARE sales_order_detail_weight_stmt FROM @sales_order_detail_weight_sql;
EXECUTE sales_order_detail_weight_stmt;
DEALLOCATE PREPARE sales_order_detail_weight_stmt;
