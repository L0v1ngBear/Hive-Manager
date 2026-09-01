-- 售后工单允许不关联销售订单；客户快照仍由工单保存，并同步进入客户管理。
ALTER TABLE `after_sales_ticket`
  MODIFY COLUMN `order_id` VARCHAR(64) NULL,
  MODIFY COLUMN `project_name` VARCHAR(200) NULL;
