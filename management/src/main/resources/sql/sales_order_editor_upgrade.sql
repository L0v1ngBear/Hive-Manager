ALTER TABLE sales_order
    ADD COLUMN project_name VARCHAR(100) NULL COMMENT '项目名称' AFTER customer_phone;

ALTER TABLE sales_order_detail
    ADD COLUMN weight DECIMAL(10,2) NULL COMMENT '克重' AFTER model_code;
