-- 售后工单负责人：保留指派快照，避免员工改名后历史工单失去可读名称。
SET @after_sales_assignee_user_id_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sales_ticket' AND COLUMN_NAME = 'assignee_user_id'
);
SET @after_sales_assignee_user_id_sql := IF(
  @after_sales_assignee_user_id_exists = 0,
  'ALTER TABLE after_sales_ticket ADD COLUMN assignee_user_id BIGINT NULL AFTER attachment_urls_json',
  'SELECT 1'
);
PREPARE after_sales_assignee_user_id_stmt FROM @after_sales_assignee_user_id_sql;
EXECUTE after_sales_assignee_user_id_stmt;
DEALLOCATE PREPARE after_sales_assignee_user_id_stmt;

SET @after_sales_assignee_name_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sales_ticket' AND COLUMN_NAME = 'assignee_name'
);
SET @after_sales_assignee_name_sql := IF(
  @after_sales_assignee_name_exists = 0,
  'ALTER TABLE after_sales_ticket ADD COLUMN assignee_name VARCHAR(80) NULL AFTER assignee_user_id',
  'SELECT 1'
);
PREPARE after_sales_assignee_name_stmt FROM @after_sales_assignee_name_sql;
EXECUTE after_sales_assignee_name_stmt;
DEALLOCATE PREPARE after_sales_assignee_name_stmt;

SET @after_sales_assignee_index_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sales_ticket' AND INDEX_NAME = 'idx_after_sales_ticket_assignee'
);
SET @after_sales_assignee_index_sql := IF(
  @after_sales_assignee_index_exists = 0,
  'ALTER TABLE after_sales_ticket ADD KEY idx_after_sales_ticket_assignee (tenant_code, assignee_user_id, status)',
  'SELECT 1'
);
PREPARE after_sales_assignee_index_stmt FROM @after_sales_assignee_index_sql;
EXECUTE after_sales_assignee_index_stmt;
DEALLOCATE PREPARE after_sales_assignee_index_stmt;
