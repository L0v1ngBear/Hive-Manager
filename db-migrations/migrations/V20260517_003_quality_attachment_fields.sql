-- V20260517_003_quality_attachment_fields.sql
-- Add optional attachment metadata to quality / after-sales records and finance approvals.

SET @database_name := DATABASE();

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE bad_product_record ADD COLUMN attachment_name VARCHAR(180) DEFAULT NULL COMMENT ''附件名称'' AFTER improvement_plan',
    'SELECT ''attachment_name exists'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'bad_product_record'
    AND COLUMN_NAME = 'attachment_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE finance_approval ADD COLUMN attachment_name VARCHAR(180) DEFAULT NULL COMMENT ''附件名称'' AFTER reason',
    'SELECT ''finance attachment_name exists'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'finance_approval'
    AND COLUMN_NAME = 'attachment_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE finance_approval MODIFY COLUMN attachment_url VARCHAR(512) DEFAULT NULL COMMENT ''附件地址''',
    'SELECT ''finance attachment_url missing'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'finance_approval'
    AND COLUMN_NAME = 'attachment_url'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE finance_approval ADD COLUMN attachment_size BIGINT DEFAULT NULL COMMENT ''附件大小，字节'' AFTER attachment_url',
    'SELECT ''finance attachment_size exists'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'finance_approval'
    AND COLUMN_NAME = 'attachment_size'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE bad_product_record ADD COLUMN attachment_url VARCHAR(512) DEFAULT NULL COMMENT ''附件地址'' AFTER attachment_name',
    'SELECT ''attachment_url exists'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'bad_product_record'
    AND COLUMN_NAME = 'attachment_url'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE bad_product_record ADD COLUMN attachment_size BIGINT DEFAULT NULL COMMENT ''附件大小，字节'' AFTER attachment_url',
    'SELECT ''attachment_size exists'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @database_name
    AND TABLE_NAME = 'bad_product_record'
    AND COLUMN_NAME = 'attachment_size'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
