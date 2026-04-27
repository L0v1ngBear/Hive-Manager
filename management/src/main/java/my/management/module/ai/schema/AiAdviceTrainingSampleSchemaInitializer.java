package my.management.module.ai.schema;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * AI 建议训练样本表初始化器。
 */
@Component
public class AiAdviceTrainingSampleSchemaInitializer {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_advice_training_sample (
                  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                  tenant_code VARCHAR(50) NOT NULL COMMENT '租户编码',
                  sample_key VARCHAR(160) NOT NULL COMMENT '样本去重键',
                  category VARCHAR(60) DEFAULT NULL COMMENT '建议业务维度',
                  title VARCHAR(160) NOT NULL COMMENT '建议标题',
                  source_type VARCHAR(50) NOT NULL DEFAULT 'local_rules' COMMENT '建议来源',
                  priority VARCHAR(20) DEFAULT NULL COMMENT '管理优先级',
                  confidence INT DEFAULT NULL COMMENT '置信度',
                  input_snapshot_json JSON DEFAULT NULL COMMENT '经营快照输入',
                  behavior_context_json JSON DEFAULT NULL COMMENT '租户行为画像输入',
                  advice_json JSON DEFAULT NULL COMMENT '建议输出',
                  label_status VARCHAR(30) NOT NULL DEFAULT 'unlabeled' COMMENT '标注状态',
                  feedback_type VARCHAR(30) DEFAULT NULL COMMENT '反馈类型',
                  feedback_text VARCHAR(500) DEFAULT NULL COMMENT '反馈说明',
                  feedback_user_id BIGINT DEFAULT NULL COMMENT '反馈用户ID',
                  feedback_time DATETIME DEFAULT NULL COMMENT '反馈时间',
                  occurrence_count INT NOT NULL DEFAULT 1 COMMENT '样本出现次数',
                  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_ai_sample_tenant_key (tenant_code, sample_key),
                  KEY idx_ai_sample_tenant_category_time (tenant_code, category, update_time),
                  KEY idx_ai_sample_label_status (tenant_code, label_status, update_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI建议训练样本表'
                """);
        addColumnIfMissing("feedback_type", "ALTER TABLE ai_advice_training_sample ADD COLUMN feedback_type VARCHAR(30) DEFAULT NULL COMMENT '反馈类型' AFTER label_status");
        addColumnIfMissing("feedback_text", "ALTER TABLE ai_advice_training_sample ADD COLUMN feedback_text VARCHAR(500) DEFAULT NULL COMMENT '反馈说明' AFTER feedback_type");
        addColumnIfMissing("feedback_user_id", "ALTER TABLE ai_advice_training_sample ADD COLUMN feedback_user_id BIGINT DEFAULT NULL COMMENT '反馈用户ID' AFTER feedback_text");
        addColumnIfMissing("feedback_time", "ALTER TABLE ai_advice_training_sample ADD COLUMN feedback_time DATETIME DEFAULT NULL COMMENT '反馈时间' AFTER feedback_user_id");
    }

    private void addColumnIfMissing(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'ai_advice_training_sample'
                  AND column_name = ?
                """, Integer.class, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }
}
