CREATE TABLE IF NOT EXISTS tenant_manual (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    content MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
    updater_id BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_manual_tenant (tenant_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='企业自定义使用手册';
