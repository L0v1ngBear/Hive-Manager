-- 销售订单开票标记升级脚本。
-- 执行前请确认连接的是 hive 业务库。
ALTER TABLE `sales_order`
    ADD COLUMN `is_invoice` tinyint NOT NULL DEFAULT 0 COMMENT '是否开票：0-否，1-是';
