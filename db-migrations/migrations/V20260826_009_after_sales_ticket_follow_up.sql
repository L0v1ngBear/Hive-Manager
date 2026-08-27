-- 售后工单结案后的客户回访记录；全部字段允许空，兼容已结案的历史工单。
SET @after_sales_follow_up_time_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sales_ticket' AND COLUMN_NAME = 'follow_up_time'
);
SET @after_sales_follow_up_time_sql := IF(
  @after_sales_follow_up_time_exists = 0,
  'ALTER TABLE after_sales_ticket ADD COLUMN follow_up_time DATETIME NULL AFTER assignee_name',
  'SELECT 1'
);
PREPARE after_sales_follow_up_time_stmt FROM @after_sales_follow_up_time_sql;
EXECUTE after_sales_follow_up_time_stmt;
DEALLOCATE PREPARE after_sales_follow_up_time_stmt;

SET @after_sales_follow_up_operator_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sales_ticket' AND COLUMN_NAME = 'follow_up_operator_name'
);
SET @after_sales_follow_up_operator_sql := IF(
  @after_sales_follow_up_operator_exists = 0,
  'ALTER TABLE after_sales_ticket ADD COLUMN follow_up_operator_name VARCHAR(80) NULL AFTER follow_up_time',
  'SELECT 1'
);
PREPARE after_sales_follow_up_operator_stmt FROM @after_sales_follow_up_operator_sql;
EXECUTE after_sales_follow_up_operator_stmt;
DEALLOCATE PREPARE after_sales_follow_up_operator_stmt;

SET @after_sales_follow_up_satisfaction_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sales_ticket' AND COLUMN_NAME = 'follow_up_satisfaction'
);
SET @after_sales_follow_up_satisfaction_sql := IF(
  @after_sales_follow_up_satisfaction_exists = 0,
  'ALTER TABLE after_sales_ticket ADD COLUMN follow_up_satisfaction VARCHAR(20) NULL AFTER follow_up_operator_name',
  'SELECT 1'
);
PREPARE after_sales_follow_up_satisfaction_stmt FROM @after_sales_follow_up_satisfaction_sql;
EXECUTE after_sales_follow_up_satisfaction_stmt;
DEALLOCATE PREPARE after_sales_follow_up_satisfaction_stmt;

SET @after_sales_follow_up_resolved_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sales_ticket' AND COLUMN_NAME = 'follow_up_resolved'
);
SET @after_sales_follow_up_resolved_sql := IF(
  @after_sales_follow_up_resolved_exists = 0,
  'ALTER TABLE after_sales_ticket ADD COLUMN follow_up_resolved TINYINT NOT NULL DEFAULT 0 AFTER follow_up_satisfaction',
  'SELECT 1'
);
PREPARE after_sales_follow_up_resolved_stmt FROM @after_sales_follow_up_resolved_sql;
EXECUTE after_sales_follow_up_resolved_stmt;
DEALLOCATE PREPARE after_sales_follow_up_resolved_stmt;

SET @after_sales_follow_up_content_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sales_ticket' AND COLUMN_NAME = 'follow_up_content'
);
SET @after_sales_follow_up_content_sql := IF(
  @after_sales_follow_up_content_exists = 0,
  'ALTER TABLE after_sales_ticket ADD COLUMN follow_up_content TEXT NULL AFTER follow_up_resolved',
  'SELECT 1'
);
PREPARE after_sales_follow_up_content_stmt FROM @after_sales_follow_up_content_sql;
EXECUTE after_sales_follow_up_content_stmt;
DEALLOCATE PREPARE after_sales_follow_up_content_stmt;
