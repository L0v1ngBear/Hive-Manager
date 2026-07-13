ALTER TABLE user
    ADD COLUMN login_name VARCHAR(64) NULL COMMENT '登录账号' AFTER name,
    ADD COLUMN password VARCHAR(128) NULL COMMENT '登录密码' AFTER phone;

UPDATE user
SET login_name = phone
WHERE login_name IS NULL OR login_name = '';

UPDATE user
SET password = '$2a$10$Ahqja4UeEp4qJVCI.9.AROfQA53In8fF8hGGbp9oDsHo/Z3.afG3C'
WHERE password IS NULL OR password = '';

ALTER TABLE user
    MODIFY COLUMN login_name VARCHAR(64) NOT NULL COMMENT '登录账号',
    MODIFY COLUMN password VARCHAR(128) NOT NULL COMMENT '登录密码';

ALTER TABLE user
    ADD UNIQUE KEY uk_user_tenant_login_name (tenant_code, login_name);
