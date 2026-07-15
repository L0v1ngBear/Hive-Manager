CREATE DATABASE IF NOT EXISTS `xxl_job`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `xxl_job`;

-- Low-cost single-machine mode: keep scheduled jobs, but reduce noisy high-frequency triggers.
UPDATE `xxl_job_info`
SET `schedule_type` = 'CRON',
    `schedule_conf` = '0 0 8-20 * * ? *',
    `trigger_next_time` = UNIX_TIMESTAMP(NOW() + INTERVAL 1 HOUR) * 1000,
    `update_time` = NOW()
WHERE `executor_handler` = 'notificationClosedLoopJob';

UPDATE `xxl_job_info`
SET `schedule_type` = 'CRON',
    `schedule_conf` = '0 15 3 * * ? *',
    `trigger_next_time` = UNIX_TIMESTAMP(NOW() + INTERVAL 1 DAY) * 1000,
    `update_time` = NOW()
WHERE `executor_handler` = 'runtimeStabilityAuditJob';

UPDATE `xxl_job_info`
SET `schedule_type` = 'CRON',
    `schedule_conf` = '0 0 6 * * ? *',
    `trigger_next_time` = UNIX_TIMESTAMP(NOW() + INTERVAL 1 DAY) * 1000,
    `update_time` = NOW()
WHERE `executor_handler` = 'aiAdviceSnapshotRefreshJob';
