-- 售后受理信息补充：保留既有 service_address 作为服务地址，新增实际地址与开业时间。
SET @after_sales_actual_address_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sales_ticket' AND COLUMN_NAME = 'actual_address'
);
SET @after_sales_actual_address_sql := IF(
  @after_sales_actual_address_exists = 0,
  'ALTER TABLE after_sales_ticket ADD COLUMN actual_address VARCHAR(500) NULL AFTER service_address',
  'SELECT 1'
);
PREPARE after_sales_actual_address_stmt FROM @after_sales_actual_address_sql;
EXECUTE after_sales_actual_address_stmt;
DEALLOCATE PREPARE after_sales_actual_address_stmt;

SET @after_sales_opening_date_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sales_ticket' AND COLUMN_NAME = 'opening_date'
);
SET @after_sales_opening_date_sql := IF(
  @after_sales_opening_date_exists = 0,
  'ALTER TABLE after_sales_ticket ADD COLUMN opening_date DATE NULL AFTER actual_address',
  'SELECT 1'
);
PREPARE after_sales_opening_date_stmt FROM @after_sales_opening_date_sql;
EXECUTE after_sales_opening_date_stmt;
DEALLOCATE PREPARE after_sales_opening_date_stmt;
