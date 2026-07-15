-- V20260514_001_default_employee_role_permissions.sql
-- Purpose:
-- 1. Keep the built-in EMPLOYEE role available for every tenant.
-- 2. Give newly joined mini-app users a safe baseline permission set.
-- 3. Remove broad/admin permissions from the built-in EMPLOYEE role only.

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_default_employee_perm (
    perm_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (perm_code)
) ENGINE=MEMORY;

TRUNCATE TABLE tmp_default_employee_perm;

INSERT INTO tmp_default_employee_perm (perm_code) VALUES
('attendance:punch'),
('attendance:record:list'),
('approval:leave:submit'),
('approval:leave:detail'),
('approval:finance:submit'),
('approval:finance:detail'),
('production:order:list'),
('production:order:detail'),
('production:order:log'),
('production:order:status'),
('sales:order:list'),
('sales:order:detail'),
('sales:order:status'),
('inventory:barcode:search'),
('inventory:model:search'),
('inventory:record:recent'),
('inventory:warning:list'),
('inventory:cloth:in'),
('inventory:cloth:out'),
('badproduct:list'),
('badproduct:save'),
('customer:page'),
('customer:detail'),
('label:template:list'),
('label:template:detail'),
('label:template:default');

INSERT INTO sys_role (tenant_code, role_code, role_name, is_system, create_time, update_time, is_deleted)
SELECT t.tenant_code, 'EMPLOYEE', '普通员工', 1, NOW(), NOW(), 0
FROM tenant t
WHERE t.tenant_code IS NOT NULL
  AND t.tenant_code <> ''
  AND IFNULL(t.deleted, 0) = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role r
      WHERE r.tenant_code = t.tenant_code
        AND BINARY r.role_code = BINARY 'EMPLOYEE'
        AND IFNULL(r.is_deleted, 0) = 0
  );

UPDATE sys_role
SET role_name = '普通员工',
    is_system = 1,
    update_time = NOW()
WHERE BINARY role_code = BINARY 'EMPLOYEE'
  AND IFNULL(is_deleted, 0) = 0;

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT r.id, p.id, NOW(), 0
FROM sys_role r
INNER JOIN sys_permission p
INNER JOIN tmp_default_employee_perm allow_perm
    ON BINARY allow_perm.perm_code = BINARY p.perm_code
WHERE BINARY r.role_code = BINARY 'EMPLOYEE'
  AND IFNULL(r.is_deleted, 0) = 0
  AND IFNULL(p.is_deleted, 0) = 0
ON DUPLICATE KEY UPDATE is_deleted = 0;

UPDATE sys_role_permission rp
INNER JOIN sys_role r ON r.id = rp.role_id
INNER JOIN sys_permission p ON p.id = rp.permission_id
LEFT JOIN tmp_default_employee_perm allow_perm
    ON BINARY allow_perm.perm_code = BINARY p.perm_code
SET rp.is_deleted = 1
WHERE BINARY r.role_code = BINARY 'EMPLOYEE'
  AND IFNULL(r.is_deleted, 0) = 0
  AND IFNULL(rp.is_deleted, 0) = 0
  AND allow_perm.perm_code IS NULL
  AND (
      BINARY p.perm_code IN (
          BINARY '*',
          BINARY '*:*',
          BINARY 'attendance:*',
          BINARY 'approval:*',
          BINARY 'sales:order:*',
          BINARY 'production:order:*',
          BINARY 'inventory:*',
          BINARY 'badproduct:*',
          BINARY 'customer:*',
          BINARY 'document:*',
          BINARY 'label:template:save',
          BINARY 'label:template:upload',
          BINARY 'label:template:disable',
          BINARY 'approval:leave:audit',
          BINARY 'approval:finance:audit'
      )
      OR BINARY p.perm_code LIKE BINARY 'platform:%'
      OR BINARY p.perm_code LIKE BINARY 'employee:%'
      OR BINARY p.perm_code LIKE BINARY 'role:%'
      OR BINARY p.perm_code LIKE BINARY 'price:%'
      OR BINARY p.perm_code LIKE BINARY 'dashboard:ai:%'
      OR BINARY p.perm_code LIKE BINARY 'receipt:%'
  );

INSERT INTO sys_user_role (user_id, tenant_code, role_id, create_time, is_deleted)
SELECT u.id, u.tenant_code, r.id, NOW(), 0
FROM `user` u
INNER JOIN sys_role r
    ON r.tenant_code = u.tenant_code
   AND BINARY r.role_code = BINARY 'EMPLOYEE'
   AND IFNULL(r.is_deleted, 0) = 0
WHERE u.tenant_code IS NOT NULL
  AND u.tenant_code <> ''
  AND IFNULL(u.status, 1) IN (1, 2)
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user_role ur
      INNER JOIN sys_role existing_role
          ON existing_role.id = ur.role_id
         AND existing_role.tenant_code = ur.tenant_code
         AND IFNULL(existing_role.is_deleted, 0) = 0
      WHERE ur.user_id = u.id
        AND ur.tenant_code = u.tenant_code
        AND IFNULL(ur.is_deleted, 0) = 0
  );

DROP TEMPORARY TABLE IF EXISTS tmp_default_employee_perm;
