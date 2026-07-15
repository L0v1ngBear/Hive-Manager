CREATE TABLE IF NOT EXISTS approval_default_auditor (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  tenant_code VARCHAR(50) NOT NULL COMMENT '租户编码',
  approval_type VARCHAR(40) NOT NULL COMMENT '审批类型：ORDER/FINANCE/LEAVE/RESIGNATION',
  auditor_id BIGINT NOT NULL COMMENT '默认审批负责人用户ID',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_approval_default_tenant_type (tenant_code, approval_type),
  KEY idx_approval_default_auditor (tenant_code, auditor_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批默认负责人配置表';
