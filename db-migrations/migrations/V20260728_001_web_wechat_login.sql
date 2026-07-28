-- Web WeChat OAuth identities are bound explicitly to existing tenant users.
-- Only a keyed subject hash is stored; raw openid/unionid and OAuth tokens are
-- never persisted.

CREATE TABLE IF NOT EXISTS user_wechat_identity (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_code VARCHAR(50) NOT NULL COMMENT '租户编码',
    user_id BIGINT NOT NULL COMMENT '员工用户ID',
    subject_hash CHAR(64) NOT NULL COMMENT '微信网站身份HMAC摘要',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wechat_identity_subject_tenant (subject_hash, tenant_code),
    UNIQUE KEY uk_wechat_identity_user_tenant (user_id, tenant_code),
    KEY idx_wechat_identity_tenant_user (tenant_code, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='网页微信登录身份绑定';
