-- V20260515_001_wechat_subscribe_user.sql
-- Purpose:
-- 1. Ensure WeChat mini-program subscribe-message authorization records exist online.
-- 2. This is schema-only and does not modify existing business data.

CREATE TABLE IF NOT EXISTS wechat_subscribe_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_code VARCHAR(50) NOT NULL COMMENT '租户编码',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    openid VARCHAR(128) NOT NULL COMMENT '微信 openid',
    template_id VARCHAR(128) NOT NULL COMMENT '订阅消息模板ID',
    subscribe_status VARCHAR(20) NOT NULL DEFAULT 'accept' COMMENT '订阅授权状态：accept/reject/ban/used',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wechat_subscribe_user_template (user_id, template_id),
    KEY idx_wechat_subscribe_tenant_user (tenant_code, user_id),
    KEY idx_wechat_subscribe_openid (openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信订阅消息授权用户表';
