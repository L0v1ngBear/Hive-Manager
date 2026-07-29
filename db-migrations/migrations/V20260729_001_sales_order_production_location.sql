-- Add an optional production location to sales orders.
-- Existing orders remain NULL so the release is compatible with retained data.

SET @database_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*)
     FROM information_schema.columns
    WHERE table_schema = @database_name
      AND table_name = 'sales_order'
      AND column_name = 'production_location') = 0,
  'ALTER TABLE sales_order ADD COLUMN production_location VARCHAR(16) DEFAULT NULL COMMENT ''生产地点'' AFTER information_channel',
  'SELECT ''sales_order.production_location exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
