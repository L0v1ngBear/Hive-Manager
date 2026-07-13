-- 组织架构模块升级脚本，可重复执行。
CREATE TABLE IF NOT EXISTS `emp_department` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `tenant_code` varchar(64) NOT NULL,
    `dept_name` varchar(64) NOT NULL,
    `dept_code` varchar(64) NOT NULL,
    `parent_id` bigint NULL,
    `leader_name` varchar(64) NULL,
    `sort_no` int NOT NULL DEFAULT 0,
    `status` tinyint NOT NULL DEFAULT 1,
    `is_deleted` tinyint NOT NULL DEFAULT 0,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_department_code` (`tenant_code`, `dept_code`),
    KEY `idx_emp_department_parent` (`tenant_code`, `parent_id`)
) COMMENT='组织部门表';

SET @parent_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_department' AND COLUMN_NAME = 'parent_id'
);
SET @parent_sql := IF(@parent_exists = 0, 'ALTER TABLE `emp_department` ADD COLUMN `parent_id` bigint NULL COMMENT ''上级部门ID'' AFTER `dept_code`', 'SELECT ''parent_id exists''');
PREPARE stmt FROM @parent_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @leader_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_department' AND COLUMN_NAME = 'leader_name'
);
SET @leader_sql := IF(@leader_exists = 0, 'ALTER TABLE `emp_department` ADD COLUMN `leader_name` varchar(64) NULL COMMENT ''部门负责人名称'' AFTER `parent_id`', 'SELECT ''leader_name exists''');
PREPARE stmt FROM @leader_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
