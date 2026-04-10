CREATE TABLE IF NOT EXISTS price_sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    model_code VARCHAR(128) NOT NULL COMMENT '面料型号',
    batch_no VARCHAR(64) NULL COMMENT '批号',
    category VARCHAR(64) NULL COMMENT '面料分类',
    spec VARCHAR(255) NULL COMMENT '规格说明',
    base_price DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '基准价',
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    effective_date DATE NOT NULL COMMENT '生效日期',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-已过期 1-生效中 2-计划中',
    image_url VARCHAR(500) NULL COMMENT '图片地址',
    remark VARCHAR(500) NULL COMMENT '备注',
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_price_sku_model (tenant_code, model_code),
    KEY idx_price_sku_status (tenant_code, status),
    KEY idx_price_sku_effective (tenant_code, effective_date)
);

CREATE TABLE IF NOT EXISTS price_tier_price (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    sku_id BIGINT NOT NULL,
    tier_code VARCHAR(32) NOT NULL COMMENT '客户等级编码',
    tier_name VARCHAR(64) NOT NULL COMMENT '客户等级名称',
    fixed_price DECIMAL(12,2) NULL COMMENT '一口价',
    discount_rate DECIMAL(8,2) NULL COMMENT '折扣率，例如95表示95折',
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_price_tier (tenant_code, sku_id, tier_code),
    KEY idx_price_tier_sku (sku_id)
);

CREATE TABLE IF NOT EXISTS price_customer_override (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    sku_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    customer_name VARCHAR(128) NULL,
    price DECIMAL(12,2) NOT NULL COMMENT '客户特价',
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_price_override (tenant_code, sku_id, customer_id),
    KEY idx_price_override_customer (tenant_code, customer_id)
);

CREATE TABLE IF NOT EXISTS price_change_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    sku_id BIGINT NOT NULL,
    model_code VARCHAR(128) NOT NULL,
    old_price DECIMAL(12,2) NULL,
    new_price DECIMAL(12,2) NOT NULL,
    operator_user_id BIGINT NULL,
    remark VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_price_log_sku (tenant_code, sku_id),
    KEY idx_price_log_model (tenant_code, model_code)
);