-- V20260518_004_mini_document_employee_permissions.sql
-- Purpose:
-- 1. Mini-program now exposes document management to field users.
-- 2. The built-in EMPLOYEE role needs safe document permissions, otherwise the new entry opens with 403.
-- 3. This migration only adds permission bindings; it does not modify business data.

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_mini_document_employee_perm (
    perm_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (perm_code)
) ENGINE=MEMORY;

TRUNCATE TABLE tmp_mini_document_employee_perm;

INSERT INTO tmp_mini_document_employee_perm (perm_code) VALUES
('document:list'),
('document:breadcrumbs'),
('document:file:upload'),
('document:folder:create');

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT r.id, p.id, NOW(), 0
FROM sys_role r
INNER JOIN sys_permission p
INNER JOIN tmp_mini_document_employee_perm allow_perm
    ON BINARY allow_perm.perm_code = BINARY p.perm_code
WHERE BINARY r.role_code = BINARY 'EMPLOYEE'
  AND IFNULL(r.is_deleted, 0) = 0
  AND IFNULL(p.is_deleted, 0) = 0
ON DUPLICATE KEY UPDATE is_deleted = 0;

DROP TEMPORARY TABLE IF EXISTS tmp_mini_document_employee_perm;
