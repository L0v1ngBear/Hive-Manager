-- 租户管理属于开发者维护入口，普通企业角色不得分配平台租户权限。
UPDATE sys_role_permission rp
INNER JOIN sys_role r
    ON r.id = rp.role_id
INNER JOIN sys_permission p
    ON p.id = rp.permission_id
SET rp.is_deleted = 1
WHERE IFNULL(r.tenant_code, '') <> 'super'
  AND IFNULL(rp.is_deleted, 0) = 0
  AND IFNULL(p.is_deleted, 0) = 0
  AND (
      p.perm_code = 'platform:tenant'
      OR p.perm_code LIKE 'platform:tenant:%'
  );

-- 历史环境中如果存在平台租户管理权限，继续保留权限定义本身，由后端开发者校验兜底。
UPDATE sys_permission
SET perm_name = CASE perm_code
    WHEN 'platform:tenant' THEN '租户管理'
    WHEN 'platform:tenant:view' THEN '查看租户'
    WHEN 'platform:tenant:create' THEN '创建租户'
    WHEN 'platform:tenant:license' THEN '授权配置'
    WHEN 'platform:tenant:*' THEN '租户管理全部权限'
    ELSE perm_name
END
WHERE perm_code = 'platform:tenant'
   OR perm_code LIKE 'platform:tenant:%';
