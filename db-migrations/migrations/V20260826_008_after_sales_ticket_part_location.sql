-- 售后工单配件明细记录配件存放地点；允许旧工单为空，避免影响历史数据。
SET @after_sales_ticket_part_location_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'after_sales_ticket_part'
      AND COLUMN_NAME = 'part_location'
);
SET @after_sales_ticket_part_location_sql := IF(
    @after_sales_ticket_part_location_exists = 0,
    'ALTER TABLE after_sales_ticket_part ADD COLUMN part_location VARCHAR(20) NULL AFTER quantity',
    'SELECT 1'
);
PREPARE after_sales_ticket_part_location_stmt FROM @after_sales_ticket_part_location_sql;
EXECUTE after_sales_ticket_part_location_stmt;
DEALLOCATE PREPARE after_sales_ticket_part_location_stmt;
