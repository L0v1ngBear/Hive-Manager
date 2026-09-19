-- Additive: existing tickets, parts and follow-ups remain unchanged.
CREATE TABLE after_sales_treatment (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_code VARCHAR(64) NOT NULL,
  ticket_id BIGINT NOT NULL,
  ticket_no VARCHAR(64) NOT NULL,
  sequence_no INT NOT NULL,
  request_key VARCHAR(64) NOT NULL,
  request_hash VARCHAR(64) NOT NULL,
  treatment_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  version INT NOT NULL DEFAULT 0,
  original_opening_date DATE NULL,
  creator_user_id BIGINT NULL,
  creator_name VARCHAR(100) NULL,
  create_time DATETIME(6) NOT NULL,
  update_time DATETIME(6) NOT NULL,
  completed_time DATETIME(6) NULL,
  resolution VARCHAR(2000) NULL,
  follow_up_time DATETIME(6) NULL,
  follow_up_operator_name VARCHAR(100) NULL,
  details_json LONGTEXT NOT NULL,
  parts_json LONGTEXT NOT NULL,
  follow_up_json LONGTEXT NULL,
  UNIQUE KEY uk_treatment_sequence (tenant_code, ticket_id, sequence_no),
  UNIQUE KEY uk_treatment_request (tenant_code, ticket_id, request_key),
  KEY idx_treatment_todo (tenant_code, status, ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE after_sales_part_stock_record ADD COLUMN treatment_id BIGINT NULL COMMENT '售后处理记录ID，历史流水为空';
ALTER TABLE after_sales_ticket ADD COLUMN treatment_closures_json LONGTEXT NULL COMMENT '追加处理后的历次结案说明，不覆盖原处理结果';
