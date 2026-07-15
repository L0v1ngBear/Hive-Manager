-- Fast-path indexes for the management dashboard and AI overview reads.
-- Keep this migration append-only; it is safe to run repeatedly.

DROP PROCEDURE IF EXISTS hive_add_index_if_missing;

DELIMITER $$
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

CALL hive_add_index_if_missing('sales_order', 'idx_sales_order_tenant_create_time', '(`tenant_code`,`create_time`)');
CALL hive_add_index_if_missing('sales_order', 'idx_sales_order_tenant_customer_time', '(`tenant_code`,`customer_name`,`create_time`)');
CALL hive_add_index_if_missing('sales_order', 'idx_sales_order_tenant_delivery_status', '(`tenant_code`,`delivery_date`,`status`,`update_time`)');

CALL hive_add_index_if_missing('production_order', 'idx_prod_order_tenant_create_time', '(`tenant_code`,`create_time`)');
CALL hive_add_index_if_missing('production_order', 'idx_prod_order_tenant_status', '(`tenant_code`,`status`)');

CALL hive_add_index_if_missing('cloth', 'idx_cloth_tenant_remaining', '(`tenant_code`,`remaining_meters`)');
CALL hive_add_index_if_missing('cloth', 'idx_cloth_tenant_model_remaining_update', '(`tenant_code`,`model_code`,`remaining_meters`,`update_time`)');
CALL hive_add_index_if_missing('cloth', 'idx_cloth_tenant_del_model_remaining', '(`tenant_code`,`del_flag`,`model_code`,`remaining_meters`)');
CALL hive_add_index_if_missing('cloth', 'idx_cloth_tenant_del_bad', '(`tenant_code`,`del_flag`,`is_bad`)');

CALL hive_add_index_if_missing('outbound_order', 'idx_outbound_tenant_print_update', '(`tenant_code`,`order_status`,`print_status`,`update_time`,`id`)');
CALL hive_add_index_if_missing('inventory_statics', 'idx_inventory_statics_tenant_date', '(`tenant_code`,`stat_date`)');
CALL hive_add_index_if_missing('attendance_record', 'idx_attendance_tenant_punch_update', '(`tenant_code`,`punch_id`,`update_time`,`id`)');

CALL hive_add_index_if_missing('user_leave', 'idx_leave_tenant_auditor_status', '(`tenant_code`,`auditor_id`,`status`)');
CALL hive_add_index_if_missing('finance_approval', 'idx_finance_tenant_auditor_status', '(`tenant_code`,`auditor_id`,`status`)');

CALL hive_add_index_if_missing('bad_product_record', 'idx_bad_product_tenant_time_type', '(`tenant_code`,`create_time`,`type`)');
CALL hive_add_index_if_missing('bad_product_record', 'idx_bad_product_tenant_status', '(`tenant_code`,`status`)');
CALL hive_add_index_if_missing('customer', 'idx_customer_tenant_create_time', '(`tenant_code`,`create_time`)');

CALL hive_add_index_if_missing('ai_advice_training_sample', 'idx_ai_sample_tenant_update', '(`tenant_code`,`update_time`)');
CALL hive_add_index_if_missing('ai_advice_training_sample', 'idx_ai_sample_tenant_category_feedback', '(`tenant_code`,`category`,`feedback_type`,`update_time`)');
CALL hive_add_index_if_missing('behavior_event', 'idx_behavior_tenant_module_time', '(`tenant_code`,`module`,`create_time`)');

DROP PROCEDURE IF EXISTS hive_add_index_if_missing;
