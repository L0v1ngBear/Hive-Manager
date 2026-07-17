CREATE TABLE `sales_order_shipment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(50) NOT NULL,
  `order_id` varchar(50) NOT NULL,
  `logistics_company` varchar(100) NOT NULL,
  `tracking_no` varchar(100) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `updater_name` varchar(100) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_shipment_tracking` (`tenant_code`,`order_id`,`tracking_no`),
  KEY `idx_order_shipment_order` (`tenant_code`,`order_id`,`sort_order`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `sales_order`
  DROP COLUMN `express_company`,
  DROP COLUMN `express_no`;
