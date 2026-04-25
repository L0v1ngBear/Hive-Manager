-- 核心链路性能索引：库存、订单、出库单、运维日志
DROP PROCEDURE IF EXISTS add_index_if_absent;
DELIMITER //
CREATE PROCEDURE add_index_if_absent(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_sql TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND INDEX_NAME = p_index
  ) THEN
    SET @ddl = p_sql;
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL add_index_if_absent('cloth', 'idx_cloth_tenant_barcode', 'CREATE INDEX idx_cloth_tenant_barcode ON cloth(tenant_code, barcode)');
CALL add_index_if_absent('cloth', 'idx_cloth_tenant_model_status', 'CREATE INDEX idx_cloth_tenant_model_status ON cloth(tenant_code, model_code, status)');
CALL add_index_if_absent('cloth', 'idx_cloth_tenant_status_update', 'CREATE INDEX idx_cloth_tenant_status_update ON cloth(tenant_code, status, update_time)');
CALL add_index_if_absent('cloth', 'idx_cloth_tenant_remaining', 'CREATE INDEX idx_cloth_tenant_remaining ON cloth(tenant_code, remaining_meters)');

CALL add_index_if_absent('inventory_record', 'idx_inventory_record_tenant_time', 'CREATE INDEX idx_inventory_record_tenant_time ON inventory_record(tenant_code, create_time)');
CALL add_index_if_absent('inventory_record', 'idx_inventory_record_tenant_type_time', 'CREATE INDEX idx_inventory_record_tenant_type_time ON inventory_record(tenant_code, operate_type, create_time)');
CALL add_index_if_absent('inventory_record', 'idx_inventory_record_tenant_model_time', 'CREATE INDEX idx_inventory_record_tenant_model_time ON inventory_record(tenant_code, model_code, create_time)');

CALL add_index_if_absent('sales_order', 'idx_sales_order_tenant_status_update', 'CREATE INDEX idx_sales_order_tenant_status_update ON sales_order(tenant_code, status, update_time)');
CALL add_index_if_absent('sales_order', 'idx_sales_order_tenant_customer', 'CREATE INDEX idx_sales_order_tenant_customer ON sales_order(tenant_code, customer_name)');
CALL add_index_if_absent('sales_order', 'idx_sales_order_tenant_delivery', 'CREATE INDEX idx_sales_order_tenant_delivery ON sales_order(tenant_code, delivery_date)');

CALL add_index_if_absent('production_order', 'idx_production_order_tenant_status_update', 'CREATE INDEX idx_production_order_tenant_status_update ON production_order(tenant_code, status, update_time)');
CALL add_index_if_absent('production_order', 'idx_production_order_tenant_sales', 'CREATE INDEX idx_production_order_tenant_sales ON production_order(tenant_code, sales_order_id)');

CALL add_index_if_absent('outbound_order', 'idx_outbound_order_tenant_print_status', 'CREATE INDEX idx_outbound_order_tenant_print_status ON outbound_order(tenant_code, print_status, order_status, create_time)');
CALL add_index_if_absent('outbound_order', 'idx_outbound_order_tenant_biz', 'CREATE INDEX idx_outbound_order_tenant_biz ON outbound_order(tenant_code, biz_order_no)');
CALL add_index_if_absent('outbound_item', 'idx_outbound_item_tenant_order', 'CREATE INDEX idx_outbound_item_tenant_order ON outbound_item(tenant_code, order_id)');
CALL add_index_if_absent('outbound_item', 'idx_outbound_item_request', 'CREATE INDEX idx_outbound_item_request ON outbound_item(request_id)');

CALL add_index_if_absent('operation_log', 'idx_operation_log_tenant_level_time', 'CREATE INDEX idx_operation_log_tenant_level_time ON operation_log(tenant_code, log_level, create_time)');
CALL add_index_if_absent('operation_log', 'idx_operation_log_success_time', 'CREATE INDEX idx_operation_log_success_time ON operation_log(success, create_time)');

DROP PROCEDURE IF EXISTS add_index_if_absent;
