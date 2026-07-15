INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, create_time, update_time, is_deleted)
SELECT 0, 'notification', 1, 1900, '企业通知公告', NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission existed
    WHERE BINARY existed.perm_code = BINARY 'notification'
);

UPDATE sys_permission
SET perm_name = '企业通知公告',
    perm_type = 1,
    sort = 1900,
    update_time = NOW(),
    is_deleted = 0
WHERE BINARY perm_code = BINARY 'notification';

INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, create_time, update_time, is_deleted)
SELECT parent.id, seed.perm_code, 3, seed.sort_no, seed.perm_name, NOW(), NOW(), 0
FROM sys_permission parent
JOIN (
    SELECT 'notification:announcement:list' AS perm_code, 1 AS sort_no, '查看企业通知公告' AS perm_name
    UNION ALL
    SELECT 'notification:announcement:publish' AS perm_code, 2 AS sort_no, '发布企业通知公告' AS perm_name
) seed
WHERE BINARY parent.perm_code = BINARY 'notification'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission existed
      WHERE BINARY existed.perm_code = BINARY seed.perm_code
  );

UPDATE sys_permission
SET is_deleted = 0,
    update_time = NOW()
WHERE BINARY perm_code = BINARY 'notification:announcement:list'
   OR BINARY perm_code = BINARY 'notification:announcement:publish';

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
          OR BINARY owned_permission.perm_code = BINARY 'dashboard:*'
          OR BINARY owned_permission.perm_code = BINARY 'notification'
      )
) source
INNER JOIN sys_permission target_permission
  ON (
      BINARY target_permission.perm_code = BINARY 'notification:announcement:list'
      OR BINARY target_permission.perm_code = BINARY 'notification:announcement:publish'
  )
 AND IFNULL(target_permission.is_deleted, 0) = 0
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_permission existed
    WHERE existed.role_id = source.role_id
      AND existed.permission_id = target_permission.id
      AND IFNULL(existed.is_deleted, 0) = 0
);
