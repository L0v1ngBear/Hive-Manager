-- Multi-row order notes and an exact material-approval permission.
-- Existing order remark data is intentionally not migrated because the formal launch resets business data.

CREATE TABLE IF NOT EXISTS `sales_order_note` (
  `id` bigint NOT NULL,
  `tenant_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `creator_user_id` bigint NOT NULL,
  `creator_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updater_user_id` bigint NOT NULL,
  `updater_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sales_order_note_order` (`tenant_code`, `order_id`, `update_time`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @database_name = DATABASE();
SET @drop_sales_order_remark = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @database_name
     AND table_name = 'sales_order'
     AND column_name = 'remark') > 0,
  'ALTER TABLE `sales_order` DROP COLUMN `remark`',
  'SELECT ''sales_order.remark already removed'''
);
PREPARE stmt FROM @drop_sales_order_remark;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO sys_permission
  (parent_id, perm_code, module_code, perm_type, assignable, status, sort, perm_name,
   create_time, update_time, is_deleted)
SELECT p.id, 'order:note', 'order', 'GROUP', 0, 1, 208, '订单备注', NOW(), NOW(), 0
FROM sys_permission p
WHERE BINARY p.perm_code = BINARY 'order' AND p.is_deleted = 0
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), module_code = 'order', perm_type = 'GROUP', assignable = 0,
  status = 1, sort = 208, perm_name = '订单备注', update_time = NOW(), is_deleted = 0;

INSERT INTO sys_permission
  (parent_id, perm_code, module_code, perm_type, assignable, status, sort, perm_name,
   create_time, update_time, is_deleted)
SELECT p.id, leaf.perm_code, 'order', leaf.perm_type, 1, 1, leaf.sort_no, leaf.perm_name,
       NOW(), NOW(), 0
FROM sys_permission p
JOIN (
  SELECT 'order:note:view' AS perm_code, 'ENTRY' AS perm_type, 2081 AS sort_no, '查看订单备注' AS perm_name
  UNION ALL SELECT 'order:note:create', 'ACTION', 2082, '新增订单备注'
  UNION ALL SELECT 'order:note:update', 'ACTION', 2083, '修改订单备注'
) leaf
WHERE BINARY p.perm_code = BINARY 'order:note' AND p.is_deleted = 0
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), module_code = 'order', perm_type = VALUES(perm_type), assignable = 1,
  status = 1, sort = VALUES(sort), perm_name = VALUES(perm_name), update_time = NOW(), is_deleted = 0;

INSERT INTO sys_permission
  (parent_id, perm_code, module_code, perm_type, assignable, status, sort, perm_name,
   create_time, update_time, is_deleted)
SELECT p.id, 'order:audit:material', 'order', 'ACTION', 1, 1, 220, '审核备料申请',
       NOW(), NOW(), 0
FROM sys_permission p
WHERE BINARY p.perm_code = BINARY 'order:audit' AND p.is_deleted = 0
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), module_code = 'order', perm_type = 'ACTION', assignable = 1,
  status = 1, sort = 220, perm_name = '审核备料申请', update_time = NOW(), is_deleted = 0;

DROP TEMPORARY TABLE IF EXISTS order_note_role_grant;
CREATE TEMPORARY TABLE order_note_role_grant (
  role_code varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  perm_code varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (role_code, perm_code)
);

INSERT INTO order_note_role_grant (role_code, perm_code)
SELECT r.role_code, 'order:note:view'
FROM sys_role r
WHERE r.is_deleted = 0
  AND BINARY r.tenant_code <> BINARY 'super'
  AND r.role_code IN (
    'ADMIN', 'SALES_STAFF', 'SALES_MANAGER', 'PRODUCTION_STAFF', 'PRODUCTION_MANAGER',
    'WAREHOUSE_STAFF', 'WAREHOUSE_MANAGER', 'FINANCE_STAFF', 'FINANCE_MANAGER',
    'INSTALLATION_STAFF', 'INSTALLATION_MANAGER', 'APPROVAL_MANAGER'
  )
GROUP BY r.role_code;

INSERT IGNORE INTO order_note_role_grant (role_code, perm_code)
SELECT r.role_code, p.perm_code
FROM sys_role r
CROSS JOIN (
  SELECT 'order:note:create' AS perm_code
  UNION ALL SELECT 'order:note:update'
) p
WHERE r.is_deleted = 0
  AND BINARY r.tenant_code <> BINARY 'super'
  AND r.role_code IN ('ADMIN', 'SALES_STAFF', 'SALES_MANAGER', 'PRODUCTION_STAFF', 'PRODUCTION_MANAGER')
GROUP BY r.role_code, p.perm_code;

INSERT IGNORE INTO order_note_role_grant (role_code, perm_code) VALUES
('ADMIN', 'order:audit:material'),
('APPROVAL_MANAGER', 'order:audit:material');

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT r.id, p.id, NOW(), 0
FROM sys_role r
JOIN order_note_role_grant g ON BINARY g.role_code = BINARY r.role_code
JOIN sys_permission p ON BINARY p.perm_code = BINARY g.perm_code
WHERE r.is_deleted = 0
  AND p.is_deleted = 0
  AND p.status = 1
  AND p.assignable = 1
ON DUPLICATE KEY UPDATE is_deleted = 0;

UPDATE sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
SET rp.is_deleted = 1
WHERE BINARY p.perm_code = BINARY 'approval:order:audit';

UPDATE sys_user_permission up
JOIN sys_permission p ON p.id = up.permission_id
SET up.is_deleted = 1,
    up.update_time = NOW()
WHERE BINARY p.perm_code = BINARY 'approval:order:audit';

UPDATE sys_permission
SET assignable = 0,
    status = 0,
    is_deleted = 1,
    update_time = NOW()
WHERE BINARY perm_code = BINARY 'approval:order:audit';

UPDATE `user`
SET permission_version = permission_version + 1
WHERE BINARY tenant_code <> BINARY 'super';

DROP TEMPORARY TABLE IF EXISTS order_note_role_grant;
