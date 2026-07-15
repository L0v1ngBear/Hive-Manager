-- V20260524_001_table_export_permission.sql
-- Purpose:
-- 1. Protect the generic browser-table Excel export endpoint with an explicit permission.
-- 2. Grant the generic export permission only to administrator roles.

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, is_deleted)
SELECT 0, 'table:export', 3, 9800, '导出列表数据', 0
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_permission existed
    WHERE BINARY existed.perm_code = BINARY 'table:export'
);

UPDATE sys_permission
SET perm_name = '导出列表数据',
    perm_type = 3,
    sort = 9800,
    is_deleted = 0
WHERE BINARY perm_code = BINARY 'table:export';

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT r.id, p.id, NOW(), 0
FROM sys_role r
INNER JOIN sys_permission p
    ON BINARY p.perm_code = BINARY 'table:export'
   AND IFNULL(p.is_deleted, 0) = 0
WHERE IFNULL(r.is_deleted, 0) = 0
  AND BINARY UPPER(r.role_code) IN (BINARY 'ADMIN', BINARY 'SUPER_ADMIN', BINARY 'PLATFORM_ADMIN')
ON DUPLICATE KEY UPDATE is_deleted = 0;
