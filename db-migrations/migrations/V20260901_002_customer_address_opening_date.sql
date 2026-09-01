ALTER TABLE `customer`
  ADD COLUMN `customer_address` VARCHAR(500) NULL COMMENT '客户地址' AFTER `construction_area`,
  ADD COLUMN `opening_date` DATE NULL COMMENT '开业时间' AFTER `customer_address`;
