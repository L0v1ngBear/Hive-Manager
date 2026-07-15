-- V20260525_001_table_export_null_role_permission_fix.sql
-- Purpose:
-- Previous table export permission tightening must also cover historical roles
-- whose role_code is NULL or blank. Keep this idempotent because older servers
-- may already have executed V20260524_002 before the NULL guard was added.

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
