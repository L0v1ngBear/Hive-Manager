INSERT INTO sys_permission (parent_id, perm_code, perm_type, sort, perm_name, is_deleted)
SELECT 500, 'customer:update', 3, 4, '编辑客户', 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_permission
    WHERE perm_code = 'customer:update'
);
