ALTER TABLE user
    ADD COLUMN manager_name VARCHAR(64) NULL COMMENT '直属主管姓名' AFTER manager_id;

UPDATE user u
LEFT JOIN user leader
    ON leader.id = u.manager_id
   AND leader.tenant_code = u.tenant_code
SET u.manager_name = leader.name
WHERE (u.manager_name IS NULL OR u.manager_name = '')
  AND u.manager_id IS NOT NULL;
