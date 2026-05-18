INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, sort, is_deleted)
SELECT 1700, 0, 'badproduct', '质量管理', 1, 1700, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'badproduct');

INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, sort, is_deleted)
SELECT 1799, 1700, 'badproduct:*', '质量管理-全部权限', 3, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'badproduct:*');

INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, sort, is_deleted)
SELECT 1701, 1700, 'badproduct:list', '查看质量记录列表', 3, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'badproduct:list');

INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, sort, is_deleted)
SELECT 1702, 1700, 'badproduct:save', '登记/编辑质量记录', 3, 2, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'badproduct:save');

INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, sort, is_deleted)
SELECT 1703, 1700, 'badproduct:process', '处理质量记录', 3, 3, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'badproduct:process');
