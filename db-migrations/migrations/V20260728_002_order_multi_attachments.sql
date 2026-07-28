-- Add a bounded JSON collection for sales-order attachments while retaining the
-- original scalar columns as a first-attachment compatibility mirror.

SET @database_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*)
     FROM information_schema.columns
    WHERE table_schema = @database_name
      AND table_name = 'sales_order'
      AND column_name = 'attachments_json') = 0,
  'ALTER TABLE sales_order ADD COLUMN attachments_json JSON DEFAULT NULL COMMENT ''订单附件集合'' AFTER attachment_size',
  'SELECT ''sales_order.attachments_json exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sales_order
SET attachments_json = JSON_ARRAY(
  JSON_OBJECT(
    'fileName', COALESCE(NULLIF(TRIM(attachment_name), ''), '订单附件'),
    'fileUrl', attachment_url,
    'fileSize', attachment_size
  )
)
WHERE attachments_json IS NULL
  AND attachment_url IS NOT NULL
  AND TRIM(attachment_url) <> '';
