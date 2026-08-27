-- 售后配件建档补充单价，库存仍由入库/出库流水维护。
ALTER TABLE `after_sales_part`
  ADD COLUMN `unit_price` DECIMAL(12,2) DEFAULT NULL COMMENT '配件单价' AFTER `model_spec`;
