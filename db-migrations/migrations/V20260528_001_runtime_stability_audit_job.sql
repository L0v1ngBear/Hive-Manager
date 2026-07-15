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
  SELECT `id`
  FROM `xxl_job_group`
  WHERE `app_name` = 'hive-management'
  ORDER BY `id`
  LIMIT 1
);

SET @runtime_audit_next = UNIX_TIMESTAMP(NOW() + INTERVAL 5 MINUTE) * 1000;

INSERT INTO `xxl_job_info` (
  `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
  `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
  `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`,
  `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
  `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
)
SELECT @hive_group_id, '系统运行稳定性巡检', NOW(), NOW(), 'Hive', '',
       'CRON', '0 0/10 * * * ? *', 'DO_NOTHING', 'FIRST',
       'runtimeStabilityAuditJob', '', 'SERIAL_EXECUTION', 300,
       1, 'BEAN', '', 'init', NOW(), '',
       1, 0, @runtime_audit_next
WHERE NOT EXISTS (
  SELECT 1 FROM `xxl_job_info` WHERE `executor_handler` = 'runtimeStabilityAuditJob'
);

UPDATE `xxl_job_info`
SET `job_desc` = '系统运行稳定性巡检',
    `schedule_type` = 'CRON',
    `schedule_conf` = '0 0/10 * * * ? *',
    `misfire_strategy` = 'DO_NOTHING',
    `executor_route_strategy` = 'FIRST',
    `executor_block_strategy` = 'SERIAL_EXECUTION',
    `executor_timeout` = 300,
    `executor_fail_retry_count` = 1,
    `trigger_status` = 1,
    `update_time` = NOW()
WHERE `executor_handler` = 'runtimeStabilityAuditJob';
