-- Manual reset for TENANT_001 business test data.
-- Never add this file to migration_manifest.txt or an automatic release path.
SET NAMES utf8mb4;
SET @hive_reset_tenant := 'TENANT_001';

DROP PROCEDURE IF EXISTS `manual_reset_tenant_001_business_data`;
DELIMITER $$
CREATE PROCEDURE `manual_reset_tenant_001_business_data`()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE tenant_table VARCHAR(64);
  DECLARE previous_foreign_key_checks INT DEFAULT 1;
  DECLARE preserved_admin_count INT DEFAULT 0;
  DECLARE active_system_role_count INT DEFAULT 0;
  DECLARE nontransactional_table_count INT DEFAULT 0;
  DECLARE tenant_tables CURSOR FOR
    SELECT column_item.TABLE_NAME
    FROM information_schema.COLUMNS column_item
    INNER JOIN information_schema.TABLES table_item
      ON table_item.TABLE_SCHEMA = column_item.TABLE_SCHEMA
     AND table_item.TABLE_NAME = column_item.TABLE_NAME
     AND table_item.TABLE_TYPE = 'BASE TABLE'
    WHERE column_item.TABLE_SCHEMA = DATABASE()
      AND column_item.COLUMN_NAME = 'tenant_code'
      AND column_item.TABLE_NAME NOT IN (
        'tenant', 'user', 'sys_role', 'sys_user_role', 'sys_user_permission',
        'emp_employee_ext', 'emp_department', 'emp_position'
      )
    ORDER BY column_item.TABLE_NAME;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    SET FOREIGN_KEY_CHECKS = previous_foreign_key_checks;
    RESIGNAL;
  END;

  IF NOT (BINARY @hive_reset_tenant = BINARY 'TENANT_001') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Refusing reset for an unexpected tenant';
  END IF;
  IF DATABASE() IS NULL OR DATABASE() = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Refusing reset without a selected business database';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_hive_preserved_admin_user`;
  CREATE TEMPORARY TABLE `tmp_hive_preserved_admin_user` (
    `user_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`)
  ) ENGINE=MEMORY;

  INSERT IGNORE INTO `tmp_hive_preserved_admin_user` (`user_id`)
  SELECT DISTINCT user_role.`user_id`
  FROM `sys_user_role` user_role
  INNER JOIN `sys_role` role_item
    ON role_item.`id` = user_role.`role_id`
   AND BINARY role_item.`tenant_code` = BINARY 'TENANT_001'
   AND role_item.`is_system` = 1
   AND IFNULL(role_item.`is_deleted`, 0) = 0
   AND BINARY role_item.`role_code` = BINARY 'ADMIN'
  INNER JOIN `user` user_item
    ON user_item.`id` = user_role.`user_id`
   AND BINARY user_item.`tenant_code` = BINARY 'TENANT_001'
   AND IFNULL(user_item.`status`, 1) = 1
  WHERE BINARY user_role.`tenant_code` = BINARY 'TENANT_001'
    AND IFNULL(user_role.`is_deleted`, 0) = 0;

  SELECT COUNT(*) INTO preserved_admin_count
  FROM `tmp_hive_preserved_admin_user`;
  IF preserved_admin_count < 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Refusing reset because no active administrator would remain';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_hive_preserved_system_role`;
  CREATE TEMPORARY TABLE `tmp_hive_preserved_system_role` (
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`role_id`)
  ) ENGINE=MEMORY;

  INSERT IGNORE INTO `tmp_hive_preserved_system_role` (`role_id`)
  SELECT role_item.`id`
  FROM `sys_role` role_item
  WHERE BINARY role_item.`tenant_code` = BINARY 'TENANT_001'
    AND role_item.`is_system` = 1
    AND IFNULL(role_item.`is_deleted`, 0) = 0;

  SELECT COUNT(*) INTO active_system_role_count
  FROM `tmp_hive_preserved_system_role`;
  IF active_system_role_count < 20 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Refusing reset because the built-in role catalog is incomplete';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_hive_preserved_department`;
  CREATE TEMPORARY TABLE `tmp_hive_preserved_department` (
    `department_name` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (`department_name`)
  ) ENGINE=MEMORY;

  INSERT IGNORE INTO `tmp_hive_preserved_department` (`department_name`)
  SELECT DISTINCT TRIM(user_item.`department_name`)
  FROM `user` user_item
  INNER JOIN `tmp_hive_preserved_admin_user` preserved_admin
    ON preserved_admin.`user_id` = user_item.`id`
  WHERE user_item.`department_name` IS NOT NULL
    AND TRIM(user_item.`department_name`) <> '';

  DROP TEMPORARY TABLE IF EXISTS `tmp_hive_preserved_position`;
  CREATE TEMPORARY TABLE `tmp_hive_preserved_position` (
    `position_name` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (`position_name`)
  ) ENGINE=MEMORY;

  INSERT IGNORE INTO `tmp_hive_preserved_position` (`position_name`)
  SELECT DISTINCT TRIM(user_item.`position`)
  FROM `user` user_item
  INNER JOIN `tmp_hive_preserved_admin_user` preserved_admin
    ON preserved_admin.`user_id` = user_item.`id`
  WHERE user_item.`position` IS NOT NULL
    AND TRIM(user_item.`position`) <> '';

  SELECT COUNT(*) INTO nontransactional_table_count
  FROM information_schema.TABLES table_item
  WHERE table_item.TABLE_SCHEMA = DATABASE()
    AND table_item.TABLE_TYPE = 'BASE TABLE'
    AND (
      table_item.TABLE_NAME = 'sys_role_permission'
      OR EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS column_item
        WHERE column_item.TABLE_SCHEMA = table_item.TABLE_SCHEMA
          AND column_item.TABLE_NAME = table_item.TABLE_NAME
          AND column_item.COLUMN_NAME = 'tenant_code'
      )
    )
    AND (table_item.ENGINE IS NULL OR UPPER(table_item.ENGINE) <> 'INNODB');
  IF nontransactional_table_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Refusing reset because an affected table is not InnoDB';
  END IF;

  SET previous_foreign_key_checks = @@FOREIGN_KEY_CHECKS;
  START TRANSACTION;
  SET FOREIGN_KEY_CHECKS = 0;

  DELETE role_permission
  FROM `sys_role_permission` role_permission
  INNER JOIN `sys_role` role_item ON role_item.`id` = role_permission.`role_id`
  LEFT JOIN `tmp_hive_preserved_system_role` preserved_role
    ON preserved_role.`role_id` = role_item.`id`
  WHERE BINARY role_item.`tenant_code` = BINARY 'TENANT_001'
    AND preserved_role.`role_id` IS NULL;

  OPEN tenant_tables;
  tenant_loop: LOOP
    FETCH tenant_tables INTO tenant_table;
    IF done = 1 THEN
      LEAVE tenant_loop;
    END IF;
    IF tenant_table NOT REGEXP '^[A-Za-z0-9_]+$' THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unsafe tenant table identifier';
    END IF;
    SET @reset_sql = CONCAT(
      'DELETE FROM `', tenant_table,
      '` WHERE BINARY `tenant_code` = BINARY ''TENANT_001'''
    );
    PREPARE reset_stmt FROM @reset_sql;
    EXECUTE reset_stmt;
    DEALLOCATE PREPARE reset_stmt;
  END LOOP;
  CLOSE tenant_tables;

  DELETE FROM `sys_user_permission`
  WHERE BINARY `tenant_code` = BINARY 'TENANT_001';

  DELETE user_role
  FROM `sys_user_role` user_role
  LEFT JOIN `sys_role` role_item ON role_item.`id` = user_role.`role_id`
  LEFT JOIN `tmp_hive_preserved_admin_user` preserved_admin
    ON preserved_admin.`user_id` = user_role.`user_id`
  LEFT JOIN `tmp_hive_preserved_system_role` preserved_role
    ON preserved_role.`role_id` = user_role.`role_id`
  WHERE BINARY user_role.`tenant_code` = BINARY 'TENANT_001'
    AND NOT (
      preserved_admin.`user_id` IS NOT NULL
      AND preserved_role.`role_id` IS NOT NULL
      AND role_item.`is_system` = 1
      AND IFNULL(role_item.`is_deleted`, 0) = 0
      AND BINARY role_item.`role_code` = BINARY 'ADMIN'
      AND IFNULL(user_role.`is_deleted`, 0) = 0
    );

  DELETE employee_ext
  FROM `emp_employee_ext` employee_ext
  LEFT JOIN `tmp_hive_preserved_admin_user` preserved_admin
    ON preserved_admin.`user_id` = employee_ext.`user_id`
  WHERE BINARY employee_ext.`tenant_code` = BINARY 'TENANT_001'
    AND preserved_admin.`user_id` IS NULL;

  DELETE position_item
  FROM `emp_position` position_item
  LEFT JOIN `tmp_hive_preserved_position` preserved_position
    ON BINARY preserved_position.`position_name` = BINARY position_item.`position_name`
  WHERE BINARY position_item.`tenant_code` = BINARY 'TENANT_001'
    AND preserved_position.`position_name` IS NULL;

  DELETE department_item
  FROM `emp_department` department_item
  LEFT JOIN `tmp_hive_preserved_department` preserved_department
    ON BINARY preserved_department.`department_name` = BINARY department_item.`dept_name`
  WHERE BINARY department_item.`tenant_code` = BINARY 'TENANT_001'
    AND preserved_department.`department_name` IS NULL;

  DELETE user_item
  FROM `user` user_item
  LEFT JOIN `tmp_hive_preserved_admin_user` preserved_admin
    ON preserved_admin.`user_id` = user_item.`id`
  WHERE BINARY user_item.`tenant_code` = BINARY 'TENANT_001'
    AND preserved_admin.`user_id` IS NULL;

  UPDATE `user` user_item
  INNER JOIN `tmp_hive_preserved_admin_user` preserved_admin
    ON preserved_admin.`user_id` = user_item.`id`
  LEFT JOIN `tmp_hive_preserved_admin_user` preserved_manager
    ON preserved_manager.`user_id` = user_item.`manager_id`
  SET user_item.`manager_id` = NULL,
      user_item.`manager_name` = NULL,
      user_item.`update_time` = NOW()
  WHERE BINARY user_item.`tenant_code` = BINARY 'TENANT_001'
    AND user_item.`manager_id` IS NOT NULL
    AND preserved_manager.`user_id` IS NULL;

  DELETE role_item
  FROM `sys_role` role_item
  LEFT JOIN `tmp_hive_preserved_system_role` preserved_role
    ON preserved_role.`role_id` = role_item.`id`
  WHERE BINARY role_item.`tenant_code` = BINARY 'TENANT_001'
    AND preserved_role.`role_id` IS NULL;

  COMMIT;
  SET FOREIGN_KEY_CHECKS = previous_foreign_key_checks;

  DROP TEMPORARY TABLE IF EXISTS `tmp_hive_preserved_position`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_hive_preserved_department`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_hive_preserved_system_role`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_hive_preserved_admin_user`;
END$$
DELIMITER ;

CALL `manual_reset_tenant_001_business_data`();
DROP PROCEDURE IF EXISTS `manual_reset_tenant_001_business_data`;
