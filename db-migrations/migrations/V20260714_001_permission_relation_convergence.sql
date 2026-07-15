-- Converge role and user permission relations after Permission Catalog V3.
-- Invalid relations are soft-deleted so authorization cannot inherit retired permissions.

UPDATE sys_role_permission rp
LEFT JOIN sys_role r ON r.id = rp.role_id
LEFT JOIN sys_permission p ON p.id = rp.permission_id
SET rp.is_deleted = 1
WHERE rp.is_deleted = 0
  AND (
    r.id IS NULL OR r.is_deleted <> 0 OR
    p.id IS NULL OR p.is_deleted <> 0 OR p.status <> 1 OR p.assignable <> 1
  );

UPDATE sys_user_permission up
LEFT JOIN sys_permission p ON p.id = up.permission_id
SET up.is_deleted = 1,
    up.update_time = NOW()
WHERE up.is_deleted = 0
  AND (
    p.id IS NULL OR p.is_deleted <> 0 OR p.status <> 1 OR p.assignable <> 1
  );

UPDATE sys_user_permission up
LEFT JOIN `user` u
  ON u.id = up.user_id
 AND BINARY u.tenant_code = BINARY up.tenant_code
SET up.is_deleted = 1,
    up.update_time = NOW()
WHERE up.is_deleted = 0
  AND u.id IS NULL;

UPDATE sys_user_permission
SET is_deleted = 1,
    update_time = NOW()
WHERE is_deleted = 0
  AND (effect IS NULL OR effect NOT IN ('GRANT', 'DENY'));
