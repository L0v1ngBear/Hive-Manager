-- 售后补发件可能独立于原订单发货，需单独记录承运商以查询物流轨迹。
ALTER TABLE `after_sales_ticket`
    ADD COLUMN `logistics_company` VARCHAR(80) DEFAULT NULL COMMENT '售后发货物流公司' AFTER `waybill_no`;
