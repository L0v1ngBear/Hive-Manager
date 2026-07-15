CREATE TABLE IF NOT EXISTS `ai_advice_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_code` VARCHAR(50) NOT NULL COMMENT 'Tenant code',
  `snapshot_json` JSON DEFAULT NULL COMMENT 'Latest successful AI advice snapshot',
  `advice_count` INT NOT NULL DEFAULT 0 COMMENT 'Advice count in latest snapshot',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'Latest refresh status',
  `error_message` VARCHAR(500) DEFAULT NULL COMMENT 'Latest refresh error',
  `generated_at` DATETIME DEFAULT NULL COMMENT 'Latest successful generation time',
  `last_attempt_time` DATETIME DEFAULT NULL COMMENT 'Latest refresh attempt time',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_advice_snapshot_tenant` (`tenant_code`),
  KEY `idx_ai_advice_snapshot_status_time` (`status`, `last_attempt_time`),
  KEY `idx_ai_advice_snapshot_generated` (`generated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI advice precomputed snapshot';

CREATE DATABASE IF NOT EXISTS `xxl_job`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `xxl_job`;

INSERT INTO `xxl_job_group` (`app_name`, `title`, `address_type`, `address_list`, `update_time`)
SELECT 'hive-management', 'Hive Management Executor', 0, NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `xxl_job_group` WHERE `app_name` = 'hive-management'
);

SET @hive_group_id = (
  SELECT `id` FROM `xxl_job_group`
  WHERE `app_name` = 'hive-management'
  ORDER BY `id`
  LIMIT 1
);

SET @ai_snapshot_next = UNIX_TIMESTAMP(NOW() + INTERVAL 10 MINUTE) * 1000;

INSERT INTO `xxl_job_info` (
  `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
  `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
  `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`,
  `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
  `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
)
SELECT @hive_group_id, 'AI advice snapshot refresh', NOW(), NOW(), 'Hive', '',
       'CRON', '0 0/30 * * * ? *', 'DO_NOTHING', 'FIRST',
       'aiAdviceSnapshotRefreshJob', '', 'SERIAL_EXECUTION', 2400,
       1, 'BEAN', '', 'init', NOW(), '',
       1, 0, @ai_snapshot_next
WHERE NOT EXISTS (
  SELECT 1 FROM `xxl_job_info` WHERE `executor_handler` = 'aiAdviceSnapshotRefreshJob'
);

UPDATE `xxl_job_info`
SET `job_group` = @hive_group_id,
    `job_desc` = 'AI advice snapshot refresh',
    `schedule_type` = 'CRON',
    `schedule_conf` = '0 0/30 * * * ? *',
    `misfire_strategy` = 'DO_NOTHING',
    `executor_route_strategy` = 'FIRST',
    `executor_block_strategy` = 'SERIAL_EXECUTION',
    `executor_timeout` = 2400,
    `executor_fail_retry_count` = 1,
    `glue_type` = 'BEAN',
    `trigger_status` = 1,
    `trigger_next_time` = CASE
      WHEN COALESCE(`trigger_next_time`, 0) <= 0 THEN @ai_snapshot_next
      ELSE `trigger_next_time`
    END,
    `update_time` = NOW()
WHERE `executor_handler` = 'aiAdviceSnapshotRefreshJob';
