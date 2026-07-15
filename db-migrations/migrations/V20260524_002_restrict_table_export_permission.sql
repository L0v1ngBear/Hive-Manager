-- V20260524_002_restrict_table_export_permission.sql
-- Purpose:
-- Tighten the generic browser-table export permission after the endpoint starts
-- validating the concrete source module. Existing non-admin roles should use
-- module-specific list permissions; table:export is reserved for administrators.

UPDATE sys_permission
SET perm_type = 3,
    sort = 9800,
    is_deleted = 0
WHERE BINARY perm_code = BINARY 'table:export';

UPDATE sys_role_permission rp
INNER JOIN sys_permission p
    ON p.id = rp.permission_id
   AND BINARY p.perm_code = BINARY 'table:export'
INNER JOIN sys_role r
    ON r.id = rp.role_id
SET rp.is_deleted = 1
WHERE IFNULL(rp.is_deleted, 0) = 0
  AND IFNULL(r.is_deleted, 0) = 0
  AND BINARY UPPER(COALESCE(r.role_code, '')) NOT IN (BINARY 'ADMIN', BINARY 'SUPER_ADMIN', BINARY 'PLATFORM_ADMIN');

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT r.id, p.id, NOW(), 0
FROM sys_role r
INNER JOIN sys_permission p
    ON BINARY p.perm_code = BINARY 'table:export'
   AND IFNULL(p.is_deleted, 0) = 0
WHERE IFNULL(r.is_deleted, 0) = 0
  AND BINARY UPPER(COALESCE(r.role_code, '')) IN (BINARY 'ADMIN', BINARY 'SUPER_ADMIN', BINARY 'PLATFORM_ADMIN')
ON DUPLICATE KEY UPDATE is_deleted = 0;
