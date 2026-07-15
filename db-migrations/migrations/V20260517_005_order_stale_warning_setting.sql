CREATE TABLE IF NOT EXISTS order_setting (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  tenant_code VARCHAR(50) NOT NULL COMMENT 'Tenant code',
  stale_warning_days INT NOT NULL DEFAULT 3 COMMENT 'Warn when an active order has not been updated for N days',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_setting_tenant (tenant_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Order warning settings';

INSERT INTO order_setting (tenant_code, stale_warning_days, create_time, update_time)
SELECT t.tenant_code, 3, NOW(), NOW()
FROM tenant t
WHERE t.tenant_code IS NOT NULL
  AND t.tenant_code <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM order_setting existed
      WHERE BINARY existed.tenant_code = BINARY t.tenant_code
  );

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, create_time, update_time, is_deleted)
SELECT parent.id, 'order:warning:setting', 3, 96, '订单预警设置', NOW(), NOW(), 0
FROM sys_permission parent
WHERE BINARY parent.perm_code = BINARY 'sales:order'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission existed
      WHERE BINARY existed.perm_code = BINARY 'order:warning:setting'
  );

UPDATE sys_permission
SET perm_name = '订单预警设置',
    perm_type = 3,
    sort = 96,
    update_time = NOW(),
    is_deleted = 0
WHERE BINARY perm_code = BINARY 'order:warning:setting';

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT source.role_id, target_permission.id, NOW(), 0
FROM (
    SELECT DISTINCT rp.role_id
    FROM sys_role_permission rp
    INNER JOIN sys_permission owned_permission
      ON owned_permission.id = rp.permission_id
     AND IFNULL(owned_permission.is_deleted, 0) = 0
    WHERE IFNULL(rp.is_deleted, 0) = 0
      AND (
          BINARY owned_permission.perm_code = BINARY '*'
          OR BINARY owned_permission.perm_code = BINARY '*:*'
          OR BINARY owned_permission.perm_code = BINARY 'sales:order'
          OR BINARY owned_permission.perm_code = BINARY 'sales:order:*'
          OR BINARY owned_permission.perm_code = BINARY 'sales:order:list'
          OR BINARY owned_permission.perm_code = BINARY 'sales:order:status'
          OR BINARY owned_permission.perm_code = BINARY 'production:order'
          OR BINARY owned_permission.perm_code = BINARY 'production:order:*'
          OR BINARY owned_permission.perm_code = BINARY 'production:order:list'
          OR BINARY owned_permission.perm_code = BINARY 'production:order:status'
      )
) source
INNER JOIN sys_permission target_permission
  ON BINARY target_permission.perm_code = BINARY 'order:warning:setting'
 AND IFNULL(target_permission.is_deleted, 0) = 0
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_permission existed
    WHERE existed.role_id = source.role_id
      AND existed.permission_id = target_permission.id
      AND IFNULL(existed.is_deleted, 0) = 0
);
