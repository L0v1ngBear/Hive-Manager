-- Query performance indexes for high-frequency tenant-scoped reads.
-- Safe to run repeatedly: each index is created only when missing.

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

CALL hive_add_index_if_missing('user', 'idx_user_tenant_status_id', '(`tenant_code`,`status`,`id`)');
CALL hive_add_index_if_missing('user', 'idx_user_tenant_manager_status', '(`tenant_code`,`manager_id`,`status`)');

CALL hive_add_index_if_missing('sys_role', 'idx_role_tenant_code_deleted', '(`tenant_code`,`role_code`,`is_deleted`)');
CALL hive_add_index_if_missing('sys_role', 'idx_role_tenant_deleted_time', '(`tenant_code`,`is_deleted`,`create_time`)');
CALL hive_add_index_if_missing('sys_user_role', 'idx_user_role_tenant_user_deleted', '(`tenant_code`,`user_id`,`is_deleted`)');
CALL hive_add_index_if_missing('sys_user_role', 'idx_user_role_tenant_role_deleted', '(`tenant_code`,`role_id`,`is_deleted`)');
CALL hive_add_index_if_missing('sys_role_permission', 'idx_role_perm_role_deleted', '(`role_id`,`is_deleted`)');
CALL hive_add_index_if_missing('sys_role_permission', 'idx_role_perm_permission_deleted', '(`permission_id`,`is_deleted`)');

CALL hive_add_index_if_missing('finance_approval', 'idx_finance_tenant_apply_status_time', '(`tenant_code`,`apply_user_id`,`status`,`create_time`)');
CALL hive_add_index_if_missing('finance_approval', 'idx_finance_tenant_auditor_status_time', '(`tenant_code`,`auditor_id`,`status`,`create_time`)');

CALL hive_add_index_if_missing('user_leave', 'idx_leave_tenant_apply_status_time', '(`tenant_code`,`apply_user_id`,`status`,`create_time`)');
CALL hive_add_index_if_missing('user_leave', 'idx_leave_tenant_auditor_status_time', '(`tenant_code`,`auditor_id`,`status`,`create_time`)');
CALL hive_add_index_if_missing('user_leave', 'idx_leave_tenant_apply_range_status', '(`tenant_code`,`apply_user_id`,`start_time`,`end_time`,`status`)');

CALL hive_add_index_if_missing('inventory_record', 'idx_inventory_tenant_operator_time', '(`tenant_code`,`operator_id`,`create_time`)');
CALL hive_add_index_if_missing('outbound_order', 'idx_outbound_tenant_operator_time', '(`tenant_code`,`operator_id`,`create_time`)');
CALL hive_add_index_if_missing('outbound_order', 'idx_outbound_tenant_operator_print_time', '(`tenant_code`,`operator_id`,`print_status`,`create_time`)');
CALL hive_add_index_if_missing('bad_product_record', 'idx_bad_product_tenant_creator_time', '(`tenant_code`,`creator_id`,`create_time`)');
CALL hive_add_index_if_missing('bad_product_record', 'idx_bad_product_tenant_creator_status_time', '(`tenant_code`,`creator_id`,`status`,`create_time`)');

CALL hive_add_index_if_missing('sales_order', 'idx_sales_order_tenant_update', '(`tenant_code`,`update_time`)');
CALL hive_add_index_if_missing('sales_order', 'idx_sales_order_tenant_invoice_time', '(`tenant_code`,`is_invoice`,`create_time`)');
CALL hive_add_index_if_missing('sales_order', 'idx_sales_order_tenant_creator_status_update', '(`tenant_code`,`creator`,`status`,`update_time`)');
CALL hive_add_index_if_missing('sales_order', 'idx_sales_order_tenant_updater_status_update', '(`tenant_code`,`updater`,`status`,`update_time`)');
CALL hive_add_index_if_missing('sales_order_status_log', 'idx_sales_status_log_tenant_order_time', '(`tenant_code`,`order_id`,`create_time`)');

CALL hive_add_index_if_missing('production_order', 'idx_production_order_tenant_delivery', '(`tenant_code`,`delivery_date`)');
CALL hive_add_index_if_missing('production_order', 'idx_production_order_tenant_creator_status_update', '(`tenant_code`,`creator`,`status`,`update_time`)');
CALL hive_add_index_if_missing('production_order', 'idx_production_order_tenant_updater_status_update', '(`tenant_code`,`updater`,`status`,`update_time`)');
CALL hive_add_index_if_missing('production_order_status_log', 'idx_production_status_log_tenant_order_time', '(`tenant_code`,`order_id`,`create_time`)');

CALL hive_add_index_if_missing('customer', 'idx_customer_tenant_create_time', '(`tenant_code`,`create_time`)');
CALL hive_add_index_if_missing('price_sku', 'idx_price_sku_tenant_deleted_status_update', '(`tenant_code`,`is_deleted`,`status`,`update_time`)');
CALL hive_add_index_if_missing('price_sku', 'idx_price_sku_tenant_deleted_model', '(`tenant_code`,`is_deleted`,`model_code`)');

DROP PROCEDURE IF EXISTS hive_add_index_if_missing;
