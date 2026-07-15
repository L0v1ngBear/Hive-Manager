-- Forward-only convergence for retired delivery_date order fields.
-- Do not modify historical migrations that introduced delivery_date indexes.

SET @database_name = DATABASE();

DROP PROCEDURE IF EXISTS hive_converge_sales_order_cancel_reason;

DELIMITER $$
CREATE PROCEDURE hive_converge_sales_order_cancel_reason()
BEGIN
    DECLARE v_cancel_reason_needs_convergence INT DEFAULT 0;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = @database_name
          AND table_name = 'sales_order'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = @database_name
              AND table_name = 'sales_order'
              AND column_name = 'cancel_reason'
        ) THEN
            ALTER TABLE `sales_order`
                ADD COLUMN `cancel_reason` varchar(500) DEFAULT NULL;
        END IF;

        SELECT COUNT(*)
        INTO v_cancel_reason_needs_convergence
        FROM information_schema.columns
        WHERE table_schema = @database_name
          AND table_name = 'sales_order'
          AND column_name = 'cancel_reason'
          AND (
            data_type <> 'varchar'
            OR character_maximum_length <> 500
            OR is_nullable <> 'YES'
            OR column_default IS NOT NULL
          );

        IF v_cancel_reason_needs_convergence > 0 THEN
            IF EXISTS (
                SELECT 1
                FROM `sales_order`
                WHERE `cancel_reason` IS NOT NULL
                  AND CHAR_LENGTH(CAST(`cancel_reason` AS CHAR)) > 500
            ) THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Existing cancel_reason value exceeds VARCHAR(500): sales_order';
            END IF;

            ALTER TABLE `sales_order`
                MODIFY COLUMN `cancel_reason` varchar(500) DEFAULT NULL;
        END IF;
    END IF;
END$$
DELIMITER ;

CALL hive_converge_sales_order_cancel_reason();

DROP PROCEDURE IF EXISTS hive_converge_sales_order_cancel_reason;

DROP PROCEDURE IF EXISTS hive_migrate_delivery_date_to_information_channel;

DELIMITER $$
CREATE PROCEDURE hive_migrate_delivery_date_to_information_channel(IN p_table_name VARCHAR(64))
BEGIN
    DECLARE v_information_channel_needs_convergence INT DEFAULT 0;
    DECLARE v_delivery_data_type VARCHAR(64);
    DECLARE v_error_message VARCHAR(128);
    DECLARE v_history_expression LONGTEXT;
    DECLARE v_merged_expression LONGTEXT;
    DECLARE v_drop_index_clauses LONGTEXT;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = @database_name
          AND table_name = p_table_name
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = @database_name
              AND table_name = p_table_name
              AND column_name = 'information_channel'
        ) THEN
            SET @ddl = CONCAT(
                'ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
                '` ADD COLUMN `information_channel` varchar(100) DEFAULT NULL'
            );
            PREPARE stmt FROM @ddl;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;

        SELECT COUNT(*)
        INTO v_information_channel_needs_convergence
        FROM information_schema.columns
        WHERE table_schema = @database_name
          AND table_name = p_table_name
          AND column_name = 'information_channel'
          AND (
            data_type <> 'varchar'
            OR character_maximum_length <> 100
            OR is_nullable <> 'YES'
            OR column_default IS NOT NULL
          );

        IF v_information_channel_needs_convergence > 0 THEN
            SET @oversized_information_channel_count = 0;
            SET @ddl = CONCAT(
                'SELECT COUNT(*) INTO @oversized_information_channel_count FROM `',
                REPLACE(p_table_name, '`', '``'),
                '` WHERE `information_channel` IS NOT NULL ',
                'AND CHAR_LENGTH(CAST(`information_channel` AS CHAR)) > 100'
            );
            PREPARE stmt FROM @ddl;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;

            IF @oversized_information_channel_count > 0 THEN
                SET v_error_message = CONCAT(
                    'Existing information_channel value exceeds VARCHAR(100): ',
                    p_table_name
                );
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_error_message;
            END IF;
        END IF;

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = @database_name
              AND table_name = p_table_name
              AND column_name = 'delivery_date'
        ) THEN
            SELECT data_type
            INTO v_delivery_data_type
            FROM information_schema.columns
            WHERE table_schema = @database_name
              AND table_name = p_table_name
              AND column_name = 'delivery_date'
            LIMIT 1;

            IF v_delivery_data_type IN ('datetime', 'timestamp') THEN
                SET v_history_expression =
                    'CONCAT(''历史交付日期：'', DATE_FORMAT(`delivery_date`, ''%Y-%m-%d %H:%i:%s''))';
            ELSEIF v_delivery_data_type = 'date' THEN
                SET v_history_expression =
                    'CONCAT(''历史交付日期：'', DATE_FORMAT(`delivery_date`, ''%Y-%m-%d''))';
            ELSE
                SET v_history_expression = CONCAT(
                    'CASE ',
                    'WHEN CAST(`delivery_date` AS CHAR) REGEXP ',
                    '''^[0-9]{4}-[0-9]{2}-[0-9]{2}[ T][0-9]{2}:[0-9]{2}:[0-9]{2}$'' ',
                    'AND STR_TO_DATE(REPLACE(CAST(`delivery_date` AS CHAR), ''T'', '' ''), ',
                    '''%Y-%m-%d %H:%i:%s'') IS NOT NULL ',
                    'THEN CONCAT(''历史交付日期：'', DATE_FORMAT(',
                    'STR_TO_DATE(REPLACE(CAST(`delivery_date` AS CHAR), ''T'', '' ''), ',
                    '''%Y-%m-%d %H:%i:%s''), ''%Y-%m-%d %H:%i:%s'')) ',
                    'WHEN CAST(`delivery_date` AS CHAR) REGEXP ',
                    '''^[0-9]{4}-[0-9]{2}-[0-9]{2}$'' ',
                    'AND STR_TO_DATE(CAST(`delivery_date` AS CHAR), ''%Y-%m-%d'') IS NOT NULL ',
                    'THEN CONCAT(''历史交付日期：'', DATE_FORMAT(',
                    'STR_TO_DATE(CAST(`delivery_date` AS CHAR), ''%Y-%m-%d''), ''%Y-%m-%d'')) ',
                    'ELSE CONCAT(''历史交付日期：'', CAST(`delivery_date` AS CHAR)) END'
                );
            END IF;

            SET v_merged_expression = CONCAT(
                'CASE ',
                'WHEN `information_channel` IS NULL ',
                'OR TRIM(CAST(`information_channel` AS CHAR)) = '''' ',
                'THEN ', v_history_expression, ' ',
                'WHEN CAST(`information_channel` AS CHAR) = ', v_history_expression, ' ',
                'OR RIGHT(CAST(`information_channel` AS CHAR), ',
                'CHAR_LENGTH(CONCAT(''；'', ', v_history_expression, '))) ',
                '= CONCAT(''；'', ', v_history_expression, ') ',
                'THEN CAST(`information_channel` AS CHAR) ',
                'ELSE CONCAT(CAST(`information_channel` AS CHAR), ''；'', ',
                v_history_expression, ') END'
            );

            SET @delivery_conflict_count = 0;
            SET @ddl = CONCAT(
                'SELECT COUNT(*) INTO @delivery_conflict_count FROM `',
                REPLACE(p_table_name, '`', '``'), '` ',
                'WHERE `delivery_date` IS NOT NULL ',
                'AND TRIM(CAST(`delivery_date` AS CHAR)) <> '''' ',
                'AND CHAR_LENGTH(', v_merged_expression, ') > 100'
            );
            PREPARE stmt FROM @ddl;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;

            IF @delivery_conflict_count > 0 THEN
                SET v_error_message = CONCAT(
                    'Historical delivery data cannot fit in information_channel VARCHAR(100): ',
                    p_table_name
                );
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_error_message;
            END IF;

            IF EXISTS (
                SELECT 1
                FROM information_schema.statistics
                WHERE table_schema = @database_name
                  AND table_name = p_table_name
                  AND column_name = 'delivery_date'
                  AND index_name = 'PRIMARY'
            ) THEN
                SET v_error_message = CONCAT(
                    'delivery_date is part of PRIMARY KEY; manual intervention required: ',
                    p_table_name
                );
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_error_message;
            END IF;
        END IF;

        IF v_information_channel_needs_convergence > 0 THEN
            SET @ddl = CONCAT(
                'ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
                '` MODIFY COLUMN `information_channel` varchar(100) DEFAULT NULL'
            );
            PREPARE stmt FROM @ddl;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;

        IF v_delivery_data_type IS NOT NULL THEN
            SET @ddl = CONCAT(
                'UPDATE `', REPLACE(p_table_name, '`', '``'),
                '` SET `information_channel` = ', v_merged_expression, ' ',
                'WHERE `delivery_date` IS NOT NULL ',
                'AND TRIM(CAST(`delivery_date` AS CHAR)) <> '''''
            );
            PREPARE stmt FROM @ddl;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;

            SET SESSION group_concat_max_len = 65535;

            SELECT GROUP_CONCAT(
                DISTINCT CONCAT(
                    'DROP INDEX `', REPLACE(index_name, '`', '``'), '`'
                )
                ORDER BY index_name SEPARATOR ', '
            )
            INTO v_drop_index_clauses
            FROM information_schema.statistics
            WHERE table_schema = @database_name
              AND table_name = p_table_name
              AND column_name = 'delivery_date'
              AND index_name <> 'PRIMARY';

            SET @ddl = CONCAT(
                'ALTER TABLE `', REPLACE(p_table_name, '`', '``'), '` ',
                IF(v_drop_index_clauses IS NULL, '', CONCAT(v_drop_index_clauses, ', ')),
                'DROP COLUMN `delivery_date`'
            );
            PREPARE stmt FROM @ddl;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END$$
DELIMITER ;

CALL hive_migrate_delivery_date_to_information_channel('sales_order');
CALL hive_migrate_delivery_date_to_information_channel('production_order');
CALL hive_migrate_delivery_date_to_information_channel('installation_task');

DROP PROCEDURE IF EXISTS hive_migrate_delivery_date_to_information_channel;
