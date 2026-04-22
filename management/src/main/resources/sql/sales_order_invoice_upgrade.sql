-- 销售订单开票标记升级脚本。
-- 执行前请确认连接的是 hive 业务库；脚本可重复执行。
SET @column_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sales_order'
      AND COLUMN_NAME = 'is_invoice'
);

SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE `sales_order` ADD COLUMN `is_invoice` tinyint NOT NULL DEFAULT 0 COMMENT ''是否开票：0-否，1-是''',
    'SELECT ''sales_order.is_invoice already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
