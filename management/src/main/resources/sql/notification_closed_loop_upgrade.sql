CREATE TABLE IF NOT EXISTS `notification_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `dedupe_key` varchar(220) NOT NULL COMMENT '业务去重键',
  `biz_type` varchar(50) NOT NULL COMMENT '业务类型',
  `biz_id` varchar(120) DEFAULT NULL COMMENT '业务ID',
  `title` varchar(120) NOT NULL COMMENT '通知标题',
  `content` varchar(1000) NOT NULL COMMENT '通知内容',
  `level` varchar(20) NOT NULL DEFAULT 'info' COMMENT '级别(info/warning/critical)',
  `channel` varchar(30) NOT NULL DEFAULT 'IN_APP' COMMENT '通知渠道',
  `route` varchar(255) DEFAULT NULL COMMENT '前端跳转地址',
  `receiver_user_id` bigint DEFAULT NULL COMMENT '接收人用户ID，空表示租户广播',
  `receiver_name` varchar(80) DEFAULT NULL COMMENT '接收人姓名',
  `receiver_phone` varchar(30) DEFAULT NULL COMMENT '接收人手机号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态(1有效，0失效)',
  `read_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读',
  `read_time` datetime DEFAULT NULL COMMENT '已读时间',
  `send_status` varchar(30) NOT NULL DEFAULT 'PENDING' COMMENT '外部推送状态',
  `send_time` datetime DEFAULT NULL COMMENT '外部推送时间',
  `task_status` varchar(30) NOT NULL DEFAULT 'PENDING' COMMENT '待办状态(PENDING处理中，DONE已处理，IGNORED暂不处理)',
  `close_result` varchar(30) DEFAULT NULL COMMENT '闭环结果(resolved/ignored)',
  `close_note` varchar(500) DEFAULT NULL COMMENT '闭环备注',
  `close_user_id` bigint DEFAULT NULL COMMENT '闭环处理人ID',
  `close_time` datetime DEFAULT NULL COMMENT '闭环处理时间',
  `source_type` varchar(50) NOT NULL DEFAULT 'system' COMMENT '来源类型',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_dedupe` (`tenant_code`,`dedupe_key`),
  KEY `idx_notification_unread` (`tenant_code`,`receiver_user_id`,`read_flag`,`status`,`update_time`),
  KEY `idx_notification_task_status` (`tenant_code`,`task_status`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知提醒记录表';

SET @database_name = DATABASE();
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'notification_record' AND column_name = 'task_status') = 0,
  'ALTER TABLE notification_record ADD COLUMN task_status varchar(30) NOT NULL DEFAULT ''PENDING'' COMMENT ''待办状态(PENDING处理中，DONE已处理，IGNORED暂不处理)'' AFTER send_time',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'notification_record' AND column_name = 'close_result') = 0,
  'ALTER TABLE notification_record ADD COLUMN close_result varchar(30) DEFAULT NULL COMMENT ''闭环结果(resolved/ignored)'' AFTER task_status',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'notification_record' AND column_name = 'close_note') = 0,
  'ALTER TABLE notification_record ADD COLUMN close_note varchar(500) DEFAULT NULL COMMENT ''闭环备注'' AFTER close_result',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'notification_record' AND column_name = 'close_user_id') = 0,
  'ALTER TABLE notification_record ADD COLUMN close_user_id bigint DEFAULT NULL COMMENT ''闭环处理人ID'' AFTER close_note',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'notification_record' AND column_name = 'close_time') = 0,
  'ALTER TABLE notification_record ADD COLUMN close_time datetime DEFAULT NULL COMMENT ''闭环处理时间'' AFTER close_user_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @database_name AND table_name = 'notification_record' AND index_name = 'idx_notification_task_status') = 0,
  'ALTER TABLE notification_record ADD INDEX idx_notification_task_status (tenant_code, task_status, update_time)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE notification_record
SET task_status = 'DONE',
    close_result = 'legacy_read',
    close_time = COALESCE(read_time, update_time, NOW()),
    update_time = NOW()
WHERE read_flag = 1
  AND task_status = 'PENDING'
  AND close_result IS NULL;
