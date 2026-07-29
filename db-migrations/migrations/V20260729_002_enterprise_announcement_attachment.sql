-- Add one optional attachment to enterprise announcements.
-- Existing announcements remain valid with NULL attachment fields.

SET @database_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*)
     FROM information_schema.columns
    WHERE table_schema = @database_name
      AND table_name = 'enterprise_announcement'
      AND column_name = 'attachment_name') = 0,
  'ALTER TABLE enterprise_announcement ADD COLUMN attachment_name VARCHAR(180) DEFAULT NULL COMMENT ''公告附件名称'' AFTER publisher_name',
  'SELECT ''enterprise_announcement.attachment_name exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*)
     FROM information_schema.columns
    WHERE table_schema = @database_name
      AND table_name = 'enterprise_announcement'
      AND column_name = 'attachment_url') = 0,
  'ALTER TABLE enterprise_announcement ADD COLUMN attachment_url VARCHAR(1000) DEFAULT NULL COMMENT ''公告附件存储引用'' AFTER attachment_name',
  'SELECT ''enterprise_announcement.attachment_url exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*)
     FROM information_schema.columns
    WHERE table_schema = @database_name
      AND table_name = 'enterprise_announcement'
      AND column_name = 'attachment_size') = 0,
  'ALTER TABLE enterprise_announcement ADD COLUMN attachment_size BIGINT DEFAULT NULL COMMENT ''公告附件字节数'' AFTER attachment_url',
  'SELECT ''enterprise_announcement.attachment_size exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
