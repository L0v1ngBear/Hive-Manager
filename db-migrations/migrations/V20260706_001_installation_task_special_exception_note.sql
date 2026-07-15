SET @database_name = DATABASE();

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'installation_task' AND column_name = 'special_exception_note') = 0,
  'ALTER TABLE installation_task ADD COLUMN special_exception_note varchar(1000) DEFAULT NULL AFTER construction_remark',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
