CREATE TABLE IF NOT EXISTS inventory_setting (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  tenant_code VARCHAR(50) NOT NULL COMMENT 'Tenant code',
  warning_threshold_meters DECIMAL(18,2) NOT NULL DEFAULT 100.00 COMMENT 'Inventory low-stock warning threshold in meters',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (id),
  UNIQUE KEY uk_inventory_setting_tenant (tenant_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Inventory warning settings';

INSERT INTO inventory_setting (tenant_code, warning_threshold_meters, create_time, update_time)
SELECT t.tenant_code, 100.00, NOW(), NOW()
FROM tenant t
WHERE t.tenant_code IS NOT NULL
  AND t.tenant_code <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM inventory_setting existed
      WHERE BINARY existed.tenant_code = BINARY t.tenant_code
  );

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, create_time, update_time, is_deleted)
SELECT parent.id, 'inventory:warning:setting', 3, 95, '库存预警设置', NOW(), NOW(), 0
FROM sys_permission parent
WHERE BINARY parent.perm_code = BINARY 'inventory'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission existed
      WHERE BINARY existed.perm_code = BINARY 'inventory:warning:setting'
  );

UPDATE sys_permission
SET perm_name = '库存预警设置',
    perm_type = 3,
    sort = 95,
    update_time = NOW(),
    is_deleted = 0
WHERE BINARY perm_code = BINARY 'inventory:warning:setting';

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
          OR BINARY owned_permission.perm_code = BINARY 'inventory'
          OR BINARY owned_permission.perm_code = BINARY 'inventory:*'
          OR BINARY owned_permission.perm_code = BINARY 'inventory:cloth:in'
          OR BINARY owned_permission.perm_code = BINARY 'inventory:cloth:out'
      )
) source
INNER JOIN sys_permission target_permission
  ON BINARY target_permission.perm_code = BINARY 'inventory:warning:setting'
 AND IFNULL(target_permission.is_deleted, 0) = 0
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_permission existed
    WHERE existed.role_id = source.role_id
      AND existed.permission_id = target_permission.id
      AND IFNULL(existed.is_deleted, 0) = 0
);
