package my.management.module.notification.schema;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 通知模块轻量建表器。
 *
 * <p>项目目前没有统一 Flyway/Liquibase 迁移体系，因此这里用 IF NOT EXISTS 保证本地和服务器
 * 首次启动时具备通知表。后续引入正式迁移工具后，可把该 SQL 移入版本化脚本。</p>
 */
@Component
public class NotificationSchemaInitializer {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS notification_record (
                  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                  tenant_code VARCHAR(50) NOT NULL COMMENT '租户编码',
                  dedupe_key VARCHAR(220) NOT NULL COMMENT '业务去重键',
                  biz_type VARCHAR(50) NOT NULL COMMENT '业务类型',
                  biz_id VARCHAR(120) DEFAULT NULL COMMENT '业务ID',
                  title VARCHAR(120) NOT NULL COMMENT '通知标题',
                  content VARCHAR(1000) NOT NULL COMMENT '通知内容',
                  level VARCHAR(20) NOT NULL DEFAULT 'info' COMMENT '级别(info/warning/critical)',
                  channel VARCHAR(30) NOT NULL DEFAULT 'IN_APP' COMMENT '通知渠道',
                  route VARCHAR(255) DEFAULT NULL COMMENT '前端跳转地址',
                  receiver_user_id BIGINT DEFAULT NULL COMMENT '接收人用户ID，空表示租户广播',
                  receiver_name VARCHAR(80) DEFAULT NULL COMMENT '接收人姓名',
                  receiver_phone VARCHAR(30) DEFAULT NULL COMMENT '接收人手机号',
                  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1有效，0失效)',
                  read_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
                  read_time DATETIME DEFAULT NULL COMMENT '已读时间',
                  send_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '外部推送状态',
                  send_time DATETIME DEFAULT NULL COMMENT '外部推送时间',
                  source_type VARCHAR(50) NOT NULL DEFAULT 'system' COMMENT '来源类型',
                  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_notification_dedupe (tenant_code, dedupe_key),
                  KEY idx_notification_unread (tenant_code, receiver_user_id, read_flag, status, update_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知提醒记录表'
                """);
    }
}
