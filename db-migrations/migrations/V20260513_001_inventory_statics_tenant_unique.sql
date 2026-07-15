-- Inventory daily snapshots must be unique per tenant and date.
-- This migration only changes indexes; it does not update or delete business data.

SET NAMES utf8mb4;

SET @old_unique_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'inventory_statics'
      AND index_name = 'uk_stat_date'
);

SET @ddl := IF(
    @old_unique_exists > 0,
    'ALTER TABLE `inventory_statics` DROP INDEX `uk_stat_date`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @tenant_unique_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'inventory_statics'
      AND index_name = 'uk_inventory_statics_tenant_date'
);

SET @ddl := IF(
    @tenant_unique_exists = 0,
    'ALTER TABLE `inventory_statics` ADD UNIQUE KEY `uk_inventory_statics_tenant_date` (`tenant_code`, `stat_date`)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
