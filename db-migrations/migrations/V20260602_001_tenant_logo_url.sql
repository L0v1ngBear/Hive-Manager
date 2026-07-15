-- Add company logo URL to tenant profile.
-- Keep this migration idempotent because deployment packages may be retried.

SET @tenant_logo_url_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tenant'
      AND COLUMN_NAME = 'logo_url'
);

SET @tenant_logo_url_sql := IF(
    @tenant_logo_url_exists = 0,
    'ALTER TABLE tenant ADD COLUMN logo_url VARCHAR(500) NULL COMMENT ''公司Logo地址'' AFTER tenant_name',
    'SELECT ''tenant.logo_url exists'''
);

PREPARE tenant_logo_url_stmt FROM @tenant_logo_url_sql;
EXECUTE tenant_logo_url_stmt;
DEALLOCATE PREPARE tenant_logo_url_stmt;
