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

CREATE TABLE IF NOT EXISTS emp_employee_ext (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    tenant_code VARCHAR(64) NOT NULL,
    emp_no VARCHAR(32) NULL,
    email VARCHAR(128) NULL,
    employee_type VARCHAR(32) NULL,
    entry_date DATE NULL,
    avatar_url VARCHAR(255) NULL,
    remark VARCHAR(500) NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_emp_employee_ext_user (user_id),
    UNIQUE KEY uk_emp_employee_ext_emp_no (tenant_code, emp_no)
);

CREATE TABLE IF NOT EXISTS emp_department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    dept_name VARCHAR(64) NOT NULL,
    dept_code VARCHAR(64) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_emp_department_code (tenant_code, dept_code)
);

CREATE TABLE IF NOT EXISTS emp_position (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    position_name VARCHAR(64) NOT NULL,
    position_code VARCHAR(64) NOT NULL,
    department_id BIGINT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_emp_position_code (tenant_code, position_code),
    KEY idx_emp_position_department (department_id)
);

CREATE TABLE IF NOT EXISTS emp_employee_change_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    employee_id BIGINT NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    before_json JSON NULL,
    after_json JSON NULL,
    operator_user_id BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_emp_change_log_employee (employee_id),
    KEY idx_emp_change_log_tenant (tenant_code)
);
