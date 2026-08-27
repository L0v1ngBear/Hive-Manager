-- 支持货拉拉、自送、客户自提等没有可查询运单号的发货方式。
-- 已有记录默认保留为 tracked，原有快递物流展示和查询逻辑保持不变。
ALTER TABLE `sales_order_shipment`
  MODIFY COLUMN `tracking_no` varchar(100) NULL,
  ADD COLUMN `delivery_mode` varchar(32) NOT NULL DEFAULT 'tracked' AFTER `tracking_no`;
