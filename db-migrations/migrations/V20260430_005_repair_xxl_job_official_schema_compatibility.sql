-- Keep XXL-JOB schema compatible when the database was created from the
-- official XXL-JOB SQL before Hive migrations ran.
-- Official 2.4.1 uses xxl_job_group.title varchar(12), which is too short for
-- Hive executor titles such as "Hive Management Executor".

USE `xxl_job`;

ALTER TABLE `xxl_job_group`
  MODIFY `title` varchar(64) NOT NULL COMMENT 'Executor title';

ALTER TABLE `xxl_job_info`
  MODIFY `executor_param` text DEFAULT NULL;
