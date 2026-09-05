-- 售后工单：补充统一登记日期，以及更换电机时厂家返还给客户的独立物流信息。
-- 已有 logistics_company / waybill_no 保持表示客户退回厂家的物流，不迁移或覆盖历史值。
SET @after_sales_registration_date_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sales_ticket' AND COLUMN_NAME = 'registration_date'
);
SET @after_sales_registration_date_sql := IF(
  @after_sales_registration_date_exists = 0,
  'ALTER TABLE after_sales_ticket ADD COLUMN registration_date DATE NULL AFTER scheduled_time',
  'SELECT 1'
);
PREPARE after_sales_registration_date_stmt FROM @after_sales_registration_date_sql;
EXECUTE after_sales_registration_date_stmt;
DEALLOCATE PREPARE after_sales_registration_date_stmt;

SET @after_sales_manufacturer_return_company_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sales_ticket' AND COLUMN_NAME = 'manufacturer_return_logistics_company'
);
SET @after_sales_manufacturer_return_company_sql := IF(
  @after_sales_manufacturer_return_company_exists = 0,
  'ALTER TABLE after_sales_ticket ADD COLUMN manufacturer_return_logistics_company VARCHAR(80) NULL AFTER logistics_company',
  'SELECT 1'
);
PREPARE after_sales_manufacturer_return_company_stmt FROM @after_sales_manufacturer_return_company_sql;
EXECUTE after_sales_manufacturer_return_company_stmt;
DEALLOCATE PREPARE after_sales_manufacturer_return_company_stmt;

SET @after_sales_manufacturer_return_waybill_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sales_ticket' AND COLUMN_NAME = 'manufacturer_return_waybill_no'
);
SET @after_sales_manufacturer_return_waybill_sql := IF(
  @after_sales_manufacturer_return_waybill_exists = 0,
  'ALTER TABLE after_sales_ticket ADD COLUMN manufacturer_return_waybill_no VARCHAR(100) NULL AFTER manufacturer_return_logistics_company',
  'SELECT 1'
);
PREPARE after_sales_manufacturer_return_waybill_stmt FROM @after_sales_manufacturer_return_waybill_sql;
EXECUTE after_sales_manufacturer_return_waybill_stmt;
DEALLOCATE PREPARE after_sales_manufacturer_return_waybill_stmt;
