ALTER TABLE outbound_order
    ADD COLUMN order_status TINYINT NOT NULL DEFAULT 0 COMMENT '0-出库中 1-待打印 2-已完成 3-作废' AFTER customer_name;

UPDATE outbound_order
SET order_status = CASE
    WHEN print_status = 1 THEN 2
    ELSE 1
END
WHERE order_status = 0;

ALTER TABLE outbound_order
    ADD KEY idx_outbound_print_queue (tenant_code, order_status, print_status, update_time);