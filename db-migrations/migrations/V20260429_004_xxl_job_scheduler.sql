CREATE DATABASE IF NOT EXISTS `xxl_job`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `xxl_job`;

CREATE TABLE IF NOT EXISTS `xxl_job_group` (
  `id` int NOT NULL AUTO_INCREMENT,
  `app_name` varchar(64) NOT NULL COMMENT 'Executor AppName',
  `title` varchar(64) NOT NULL COMMENT 'Executor title',
  `address_type` tinyint NOT NULL DEFAULT '0' COMMENT '0=auto registry, 1=manual',
  `address_list` text COMMENT 'Executor addresses',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xxl_job_group_app_name` (`app_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_registry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `registry_group` varchar(50) NOT NULL,
  `registry_key` varchar(255) NOT NULL,
  `registry_value` varchar(255) NOT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_g_k_v` (`registry_group`, `registry_key`, `registry_value`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `job_group` int NOT NULL COMMENT 'Executor group id',
  `job_desc` varchar(255) NOT NULL,
  `add_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `author` varchar(64) DEFAULT NULL,
  `alarm_email` varchar(255) DEFAULT NULL,
  `schedule_type` varchar(50) NOT NULL DEFAULT 'NONE',
  `schedule_conf` varchar(128) DEFAULT NULL,
  `misfire_strategy` varchar(50) NOT NULL DEFAULT 'DO_NOTHING',
  `executor_route_strategy` varchar(50) DEFAULT NULL,
  `executor_handler` varchar(255) DEFAULT NULL,
  `executor_param` text DEFAULT NULL,
  `executor_block_strategy` varchar(50) DEFAULT NULL,
  `executor_timeout` int NOT NULL DEFAULT '0',
  `executor_fail_retry_count` int NOT NULL DEFAULT '0',
  `glue_type` varchar(50) NOT NULL,
  `glue_source` mediumtext,
  `glue_remark` varchar(128) DEFAULT NULL,
  `glue_updatetime` datetime DEFAULT NULL,
  `child_jobid` varchar(255) DEFAULT NULL,
  `trigger_status` tinyint NOT NULL DEFAULT '0',
  `trigger_last_time` bigint NOT NULL DEFAULT '0',
  `trigger_next_time` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_xxl_job_info_handler` (`executor_handler`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_logglue` (
  `id` int NOT NULL AUTO_INCREMENT,
  `job_id` int NOT NULL,
  `glue_type` varchar(50) DEFAULT NULL,
  `glue_source` mediumtext,
  `glue_remark` varchar(128) NOT NULL,
  `add_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_group` int NOT NULL,
  `job_id` int NOT NULL,
  `executor_address` varchar(255) DEFAULT NULL,
  `executor_handler` varchar(255) DEFAULT NULL,
  `executor_param` text,
  `executor_sharding_param` varchar(20) DEFAULT NULL,
  `executor_fail_retry_count` int NOT NULL DEFAULT '0',
  `trigger_time` datetime DEFAULT NULL,
  `trigger_code` int NOT NULL,
  `trigger_msg` text,
  `handle_time` datetime DEFAULT NULL,
  `handle_code` int NOT NULL,
  `handle_msg` text,
  `alarm_status` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `I_trigger_time` (`trigger_time`),
  KEY `I_handle_code` (`handle_code`),
  KEY `I_jobgroup` (`job_group`),
  KEY `I_jobid` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_log_report` (
  `id` int NOT NULL AUTO_INCREMENT,
  `trigger_day` datetime DEFAULT NULL,
  `running_count` int NOT NULL DEFAULT '0',
  `suc_count` int NOT NULL DEFAULT '0',
  `fail_count` int NOT NULL DEFAULT '0',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_trigger_day` (`trigger_day`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_lock` (
  `lock_name` varchar(50) NOT NULL,
  PRIMARY KEY (`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `token` varchar(100) DEFAULT NULL,
  `role` tinyint NOT NULL,
  `permission` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_username` (`username`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `xxl_job_user` (`id`, `username`, `password`, `role`, `permission`)
VALUES (1, 'admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 1, NULL);

INSERT IGNORE INTO `xxl_job_lock` (`lock_name`)
VALUES ('schedule_lock');

INSERT INTO `xxl_job_group` (`app_name`, `title`, `address_type`, `address_list`, `update_time`)
SELECT 'hive-management', 'Hive Management Executor', 0, NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `xxl_job_group` WHERE `app_name` = 'hive-management'
);

INSERT INTO `xxl_job_group` (`app_name`, `title`, `address_type`, `address_list`, `update_time`)
SELECT 'hive-mini', 'Hive Mini Backend Executor', 0, NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `xxl_job_group` WHERE `app_name` = 'hive-mini'
);

SET @hive_group_id = (
  SELECT `id` FROM `xxl_job_group`
  WHERE `app_name` = 'hive-management'
  ORDER BY `id`
  LIMIT 1
);

SET @hive_mini_group_id = (
  SELECT `id` FROM `xxl_job_group`
  WHERE `app_name` = 'hive-mini'
  ORDER BY `id`
  LIMIT 1
);

SET @today_start = CAST(CURRENT_DATE AS DATETIME);
SET @report_next = UNIX_TIMESTAMP(
  @today_start
  + INTERVAL IF(TIME_TO_SEC(CURRENT_TIME()) < 11400, 0, 1) DAY
  + INTERVAL 11400 SECOND
) * 1000;

SET @days_to_sunday = (6 - WEEKDAY(CURRENT_DATE) + 7) % 7;
SET @cleanup_candidate = @today_start + INTERVAL @days_to_sunday DAY + INTERVAL 12600 SECOND;
SET @cleanup_next = UNIX_TIMESTAMP(
  CASE
    WHEN @cleanup_candidate > NOW() THEN @cleanup_candidate
    ELSE @cleanup_candidate + INTERVAL 7 DAY
  END
) * 1000;

SET @notification_next = UNIX_TIMESTAMP(NOW() + INTERVAL 5 MINUTE) * 1000;
SET @attendance_next = UNIX_TIMESTAMP(
  @today_start
  + INTERVAL IF(TIME_TO_SEC(CURRENT_TIME()) < 7200, 0, 1) DAY
  + INTERVAL 7200 SECOND
) * 1000;
SET @inventory_next = UNIX_TIMESTAMP(
  @today_start
  + INTERVAL IF(TIME_TO_SEC(CURRENT_TIME()) < 300, 0, 1) DAY
  + INTERVAL 300 SECOND
) * 1000;

INSERT INTO `xxl_job_info` (
  `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
  `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
  `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`,
  `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
  `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
)
SELECT @hive_group_id, 'Database capacity report', NOW(), NOW(), 'Hive', '',
       'CRON', '0 10 3 * * ? *', 'DO_NOTHING', 'FIRST',
       'dbCapacityReportJob', '', 'SERIAL_EXECUTION', 300,
       1, 'BEAN', '', 'init', NOW(), '',
       1, 0, @report_next
WHERE NOT EXISTS (
  SELECT 1 FROM `xxl_job_info` WHERE `executor_handler` = 'dbCapacityReportJob'
);

INSERT INTO `xxl_job_info` (
  `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
  `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
  `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`,
  `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
  `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
)
SELECT @hive_group_id, 'Database historical data cleanup', NOW(), NOW(), 'Hive', '',
       'CRON', '0 30 3 ? * SUN *', 'DO_NOTHING', 'FIRST',
       'dbCleanupJob', '', 'SERIAL_EXECUTION', 1800,
       1, 'BEAN', '', 'init', NOW(), '',
       1, 0, @cleanup_next
WHERE NOT EXISTS (
  SELECT 1 FROM `xxl_job_info` WHERE `executor_handler` = 'dbCleanupJob'
);

INSERT INTO `xxl_job_info` (
  `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
  `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
  `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`,
  `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
  `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
)
SELECT @hive_group_id, 'Notification closed-loop sync', NOW(), NOW(), 'Hive', '',
       'CRON', '0 0/15 * * * ? *', 'DO_NOTHING', 'FIRST',
       'notificationClosedLoopJob', '', 'SERIAL_EXECUTION', 600,
       1, 'BEAN', '', 'init', NOW(), '',
       1, 0, @notification_next
WHERE NOT EXISTS (
  SELECT 1 FROM `xxl_job_info` WHERE `executor_handler` = 'notificationClosedLoopJob'
);

INSERT INTO `xxl_job_info` (
  `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
  `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
  `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`,
  `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
  `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
)
SELECT @hive_mini_group_id, 'Attendance daily stat', NOW(), NOW(), 'Hive', '',
       'CRON', '0 0 2 * * ? *', 'DO_NOTHING', 'FIRST',
       'attendanceDailyStatJob', '', 'SERIAL_EXECUTION', 1800,
       1, 'BEAN', '', 'init', NOW(), '',
       1, 0, @attendance_next
WHERE NOT EXISTS (
  SELECT 1 FROM `xxl_job_info` WHERE `executor_handler` = 'attendanceDailyStatJob'
);

INSERT INTO `xxl_job_info` (
  `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
  `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
  `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`,
  `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
  `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
)
SELECT @hive_mini_group_id, 'Inventory daily stat', NOW(), NOW(), 'Hive', '',
       'CRON', '0 5 0 * * ? *', 'DO_NOTHING', 'FIRST',
       'inventoryDailyStatJob', '', 'SERIAL_EXECUTION', 1200,
       1, 'BEAN', '', 'init', NOW(), '',
       1, 0, @inventory_next
WHERE NOT EXISTS (
  SELECT 1 FROM `xxl_job_info` WHERE `executor_handler` = 'inventoryDailyStatJob'
);
