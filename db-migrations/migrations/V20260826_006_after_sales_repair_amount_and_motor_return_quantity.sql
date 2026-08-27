-- 售后工单：上门维修登记金额，更换电机登记旧电机退还数量；旧数据默认数量为 0。
SET @after_sales_repair_amount_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sales_ticket' AND COLUMN_NAME = 'repair_amount'
);
SET @after_sales_repair_amount_sql := IF(
  @after_sales_repair_amount_exists = 0,
  'ALTER TABLE after_sales_ticket ADD COLUMN repair_amount DECIMAL(12,2) NULL AFTER return_old_motor',
  'SELECT 1'
);
PREPARE after_sales_repair_amount_stmt FROM @after_sales_repair_amount_sql;
EXECUTE after_sales_repair_amount_stmt;
DEALLOCATE PREPARE after_sales_repair_amount_stmt;

SET @after_sales_return_motor_quantity_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sales_ticket' AND COLUMN_NAME = 'return_old_motor_quantity'
);
SET @after_sales_return_motor_quantity_sql := IF(
  @after_sales_return_motor_quantity_exists = 0,
  'ALTER TABLE after_sales_ticket ADD COLUMN return_old_motor_quantity INT NOT NULL DEFAULT 0 AFTER return_old_motor',
  'SELECT 1'
);
PREPARE after_sales_return_motor_quantity_stmt FROM @after_sales_return_motor_quantity_sql;
EXECUTE after_sales_return_motor_quantity_stmt;
DEALLOCATE PREPARE after_sales_return_motor_quantity_stmt;
