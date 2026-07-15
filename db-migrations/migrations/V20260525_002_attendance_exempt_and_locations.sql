-- Attendance exempt employees and multiple punch locations.
-- This migration only adds structures and backfills active locations from existing tenant_attendance_rule.

DROP PROCEDURE IF EXISTS hive_add_column_if_missing;
DROP PROCEDURE IF EXISTS hive_add_index_if_missing;

DELIMITER $$
CREATE PROCEDURE hive_add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_ddl TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND column_name = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_column_ddl);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE hive_add_index_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_columns VARCHAR(1000)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD INDEX `', p_index_name, '` ', p_index_columns);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL hive_add_column_if_missing(
    'user',
    'attendance_required',
    '`attendance_required` TINYINT NOT NULL DEFAULT 1 COMMENT ''Attendance required: 1 yes, 0 no'' AFTER `status`'
);

CALL hive_add_index_if_missing(
    'user',
    'idx_user_attendance_required',
    '(`tenant_code`,`status`,`attendance_required`)'
);

CREATE TABLE IF NOT EXISTS `tenant_attendance_location` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_code` VARCHAR(50) NOT NULL COMMENT 'Tenant code',
  `location_name` VARCHAR(100) NOT NULL DEFAULT 'Company punch location' COMMENT 'Location name',
  `latitude` DOUBLE NOT NULL COMMENT 'Latitude',
  `longitude` DOUBLE NOT NULL COMMENT 'Longitude',
  `address` VARCHAR(255) DEFAULT NULL COMMENT 'Address',
  `radius` DOUBLE NOT NULL DEFAULT 300 COMMENT 'Allowed radius in meters',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 1 enabled, 0 disabled',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  KEY `idx_attendance_location_tenant_status` (`tenant_code`, `status`, `sort_order`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Attendance punch locations';

CREATE TABLE IF NOT EXISTS `employee_attendance_location` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_code` VARCHAR(50) NOT NULL COMMENT 'Tenant code',
  `user_id` BIGINT NOT NULL COMMENT 'Employee user id',
  `attendance_location_id` BIGINT NOT NULL COMMENT 'Attendance location id',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_attendance_location` (`tenant_code`, `user_id`, `attendance_location_id`),
  KEY `idx_employee_attendance_location_user` (`tenant_code`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Employee assigned attendance locations';

INSERT INTO `tenant_attendance_location` (
    `tenant_code`, `location_name`, `latitude`, `longitude`, `address`, `radius`,
    `status`, `sort_order`, `create_time`, `update_time`
)
SELECT
    rule_data.`tenant_code`,
    IFNULL(NULLIF(rule_data.`address`, ''), 'Company punch location') AS `location_name`,
    rule_data.`latitude`,
    rule_data.`longitude`,
    rule_data.`address`,
    IFNULL(NULLIF(rule_data.`radius`, 0), 300) AS `radius`,
    1,
    1,
    NOW(),
    NOW()
FROM `tenant_attendance_rule` rule_data
WHERE rule_data.`tenant_code` IS NOT NULL
  AND rule_data.`tenant_code` <> ''
  AND rule_data.`latitude` IS NOT NULL
  AND rule_data.`longitude` IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `tenant_attendance_location` existed
      WHERE BINARY existed.`tenant_code` = BINARY rule_data.`tenant_code`
        AND existed.`status` = 1
  );

DROP PROCEDURE IF EXISTS hive_add_column_if_missing;
DROP PROCEDURE IF EXISTS hive_add_index_if_missing;
