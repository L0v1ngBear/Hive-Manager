-- Optional recipient snapshot; existing orders and customer contacts are unchanged.
ALTER TABLE sales_order
  ADD COLUMN recipient_name VARCHAR(100) NULL COMMENT '收件人',
  ADD COLUMN recipient_phone_suffix VARCHAR(4) NULL COMMENT '收件人手机号后四位';
