CREATE TABLE IF NOT EXISTS `ai_advice_training_sample` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `sample_key` varchar(160) NOT NULL COMMENT '样本去重键',
  `category` varchar(60) DEFAULT NULL COMMENT '建议业务维度',
  `title` varchar(160) NOT NULL COMMENT '建议标题',
  `source_type` varchar(50) NOT NULL DEFAULT 'local_rules' COMMENT '建议来源',
  `priority` varchar(20) DEFAULT NULL COMMENT '管理优先级',
  `confidence` int DEFAULT NULL COMMENT '置信度',
  `input_snapshot_json` json DEFAULT NULL COMMENT '经营快照输入',
  `behavior_context_json` json DEFAULT NULL COMMENT '租户行为画像输入',
  `advice_json` json DEFAULT NULL COMMENT '建议输出',
  `label_status` varchar(30) NOT NULL DEFAULT 'unlabeled' COMMENT '标注状态',
  `feedback_type` varchar(30) DEFAULT NULL COMMENT '反馈类型',
  `feedback_text` varchar(500) DEFAULT NULL COMMENT '反馈说明',
  `feedback_user_id` bigint DEFAULT NULL COMMENT '反馈用户ID',
  `feedback_time` datetime DEFAULT NULL COMMENT '反馈时间',
  `occurrence_count` int NOT NULL DEFAULT '1' COMMENT '样本出现次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_sample_tenant_key` (`tenant_code`,`sample_key`),
  KEY `idx_ai_sample_tenant_category_time` (`tenant_code`,`category`,`update_time`),
  KEY `idx_ai_sample_label_status` (`tenant_code`,`label_status`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='经营建议训练样本表';

SET @database_name = DATABASE();
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'ai_advice_training_sample' AND column_name = 'feedback_type') = 0,
  'ALTER TABLE ai_advice_training_sample ADD COLUMN feedback_type varchar(30) DEFAULT NULL COMMENT ''反馈类型'' AFTER label_status',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'ai_advice_training_sample' AND column_name = 'feedback_text') = 0,
  'ALTER TABLE ai_advice_training_sample ADD COLUMN feedback_text varchar(500) DEFAULT NULL COMMENT ''反馈说明'' AFTER feedback_type',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'ai_advice_training_sample' AND column_name = 'feedback_user_id') = 0,
  'ALTER TABLE ai_advice_training_sample ADD COLUMN feedback_user_id bigint DEFAULT NULL COMMENT ''反馈用户ID'' AFTER feedback_text',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'ai_advice_training_sample' AND column_name = 'feedback_time') = 0,
  'ALTER TABLE ai_advice_training_sample ADD COLUMN feedback_time datetime DEFAULT NULL COMMENT ''反馈时间'' AFTER feedback_user_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
