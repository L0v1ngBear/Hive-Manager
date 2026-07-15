-- Replace retired single-person installation task fields with ordered installer details.
CREATE TABLE IF NOT EXISTS `installation_task_installer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(64) NOT NULL,
  `installation_task_id` bigint NOT NULL,
  `installer_name` varchar(50) NOT NULL,
  `installer_phone` varchar(40) NOT NULL,
  `sort_order` int NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_installation_task_installer_task` (`tenant_code`, `installation_task_id`),
  UNIQUE KEY `uk_installation_task_installer_sort` (`tenant_code`, `installation_task_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @database_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*)
     FROM information_schema.columns
    WHERE table_schema = @database_name
      AND table_name = 'installation_task'
      AND column_name = 'construction_personnel') > 0,
  'ALTER TABLE installation_task DROP COLUMN construction_personnel',
  'SELECT 1'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @sql = IF(
  (SELECT COUNT(*)
     FROM information_schema.columns
    WHERE table_schema = @database_name
      AND table_name = 'installation_task'
      AND column_name = 'construction_phone') > 0,
  'ALTER TABLE installation_task DROP COLUMN construction_phone',
  'SELECT 1'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;
