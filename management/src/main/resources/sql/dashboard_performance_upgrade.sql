CREATE INDEX idx_sales_order_tenant_create_time
    ON sales_order (tenant_code, create_time);

CREATE INDEX idx_production_order_tenant_create_time
    ON production_order (tenant_code, create_time);

CREATE INDEX idx_cloth_tenant_remaining
    ON cloth (tenant_code, remaining_meters);

CREATE INDEX idx_cloth_tenant_model_remaining_update
    ON cloth (tenant_code, model_code, remaining_meters, update_time);

CREATE INDEX idx_inventory_statics_tenant_stat_date
    ON inventory_statics (tenant_code, stat_date);

CREATE INDEX idx_attendance_record_tenant_punch_update
    ON attendance_record (tenant_code, punch_id, update_time, id);
