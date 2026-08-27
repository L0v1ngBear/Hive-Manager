-- Barcodes are scoped to a tenant everywhere in the application.  Replace
-- the historic global unique key so one tenant cannot block another tenant's
-- legitimate barcode while retaining uniqueness inside each tenant.
SET @database_name = DATABASE();

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @database_name
          AND table_name = 'cloth'
          AND index_name = 'uk_barcode'
          AND non_unique = 0
    ),
    'ALTER TABLE cloth DROP INDEX uk_barcode',
    'SELECT ''uk_barcode absent'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @database_name
          AND table_name = 'cloth'
          AND index_name = 'uk_cloth_tenant_barcode'
          AND non_unique = 0
    ),
    'SELECT ''uk_cloth_tenant_barcode exists''',
    'ALTER TABLE cloth ADD UNIQUE INDEX uk_cloth_tenant_barcode (tenant_code, barcode)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
