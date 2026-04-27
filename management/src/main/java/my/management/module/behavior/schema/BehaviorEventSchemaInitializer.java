package my.management.module.behavior.schema;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 用户行为采集表初始化器。
 *
 * <p>行为数据是 AI 个性化建议的训练/校准样本，必须按租户隔离存储。</p>
 */
@Component
public class BehaviorEventSchemaInitializer {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS behavior_event (
                  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                  tenant_code VARCHAR(50) NOT NULL COMMENT '租户编码',
                  user_id BIGINT NOT NULL COMMENT '用户ID',
                  event_type VARCHAR(60) NOT NULL COMMENT '事件类型',
                  page_path VARCHAR(255) DEFAULT NULL COMMENT '页面路径',
                  module VARCHAR(60) DEFAULT NULL COMMENT '业务模块',
                  target_type VARCHAR(60) DEFAULT NULL COMMENT '目标类型',
                  target_id VARCHAR(120) DEFAULT NULL COMMENT '目标标识',
                  action VARCHAR(60) DEFAULT NULL COMMENT '动作',
                  source VARCHAR(80) DEFAULT NULL COMMENT '来源',
                  session_id VARCHAR(80) DEFAULT NULL COMMENT '前端会话ID',
                  metadata_json JSON DEFAULT NULL COMMENT '低敏扩展元数据',
                  client_time DATETIME DEFAULT NULL COMMENT '客户端事件时间',
                  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '服务端写入时间',
                  PRIMARY KEY (id),
                  KEY idx_behavior_tenant_event_time (tenant_code, event_type, create_time),
                  KEY idx_behavior_tenant_user_time (tenant_code, user_id, create_time),
                  KEY idx_behavior_tenant_target (tenant_code, target_type, target_id, create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户行为事件表'
                """);
    }
}
