-- Ensure installation task upserts remain one row per tenant order.
-- If duplicate business rows already exist, ALTER TABLE fails without deleting data.

SET @database_name = DATABASE();

SET @valid_order_unique_index = (
  SELECT COUNT(*)
  FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = @database_name
      AND table_name = 'installation_task'
      AND non_unique = 0
      AND index_name <> 'PRIMARY'
    GROUP BY index_name
    HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') = 'tenant_code,order_id'
  ) valid_indexes
);

SET @target_index_name = IF(
  (SELECT COUNT(*)
   FROM information_schema.statistics
   WHERE table_schema = @database_name
     AND table_name = 'installation_task'
     AND index_name = 'uk_installation_task_order') = 0,
  'uk_installation_task_order',
  'uk_installation_task_order_v20260710'
);

SET @sql = IF(
  @valid_order_unique_index = 0,
  CONCAT(
    'ALTER TABLE installation_task ADD UNIQUE KEY `',
    @target_index_name,
    '` (`tenant_code`, `order_id`)'
  ),
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
