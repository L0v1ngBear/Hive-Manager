-- Manual post-migration cleanup for the retired TENANT_002 test tenant.
-- Never add this file to migration_manifest.txt.
SET NAMES utf8mb4;
SET @hive_target_tenant := 'TENANT_002';

DROP PROCEDURE IF EXISTS `manual_remove_tenant_002`;
DELIMITER $$
CREATE PROCEDURE `manual_remove_tenant_002`()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE tenant_table VARCHAR(64);
  DECLARE previous_foreign_key_checks INT DEFAULT 1;
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
      AND column_item.TABLE_NAME <> 'tenant'
    ORDER BY column_item.TABLE_NAME;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    SET FOREIGN_KEY_CHECKS = previous_foreign_key_checks;
    RESIGNAL;
  END;

  IF NOT (BINARY @hive_target_tenant = BINARY 'TENANT_002') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Refusing cleanup for an unexpected tenant';
  END IF;

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
      SET MESSAGE_TEXT = 'Refusing cleanup because a tenant-owned table is not InnoDB';
  END IF;

  SET previous_foreign_key_checks = @@FOREIGN_KEY_CHECKS;
  START TRANSACTION;
  SET FOREIGN_KEY_CHECKS = 0;

  DELETE role_permission
  FROM `sys_role_permission` role_permission
  INNER JOIN `sys_role` role_item ON role_item.`id` = role_permission.`role_id`
  WHERE BINARY role_item.`tenant_code` = BINARY 'TENANT_002';

  OPEN tenant_tables;
  tenant_loop: LOOP
    FETCH tenant_tables INTO tenant_table;
    IF done = 1 THEN
      LEAVE tenant_loop;
    END IF;
    IF tenant_table NOT REGEXP '^[A-Za-z0-9_]+$' THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unsafe tenant table identifier';
    END IF;
    SET @cleanup_sql = CONCAT(
      'DELETE FROM `', tenant_table,
      '` WHERE BINARY `tenant_code` = BINARY ''TENANT_002'''
    );
    PREPARE cleanup_stmt FROM @cleanup_sql;
    EXECUTE cleanup_stmt;
    DEALLOCATE PREPARE cleanup_stmt;
  END LOOP;
  CLOSE tenant_tables;

  DELETE FROM `tenant` WHERE BINARY `tenant_code` = BINARY 'TENANT_002';
  COMMIT;
  SET FOREIGN_KEY_CHECKS = previous_foreign_key_checks;
END$$
DELIMITER ;

CALL `manual_remove_tenant_002`();
DROP PROCEDURE IF EXISTS `manual_remove_tenant_002`;
