ALTER TABLE sales_order_detail
    ADD COLUMN tenant_code VARCHAR(50) NULL COMMENT '租户编码' AFTER order_id;

UPDATE sales_order_detail detail
INNER JOIN sales_order sale ON sale.order_id = detail.order_id
SET detail.tenant_code = sale.tenant_code
WHERE detail.tenant_code IS NULL OR detail.tenant_code = '';

ALTER TABLE sales_order_detail
    MODIFY COLUMN tenant_code VARCHAR(50) NOT NULL COMMENT '租户编码';

CREATE INDEX idx_sales_order_detail_tenant_order
    ON sales_order_detail (tenant_code, order_id);
