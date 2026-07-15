-- Add OSS storage metadata to document records.
-- file_url stays for frontend compatibility; storage_object_key is the stable source of truth.

SET @database_name = DATABASE();

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'document' AND column_name = 'original_name') = 0,
  'ALTER TABLE document ADD COLUMN original_name varchar(255) DEFAULT NULL COMMENT ''Original uploaded filename'' AFTER name',
  'SELECT ''original_name exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'document' AND column_name = 'storage_provider') = 0,
  'ALTER TABLE document ADD COLUMN storage_provider varchar(32) DEFAULT NULL COMMENT ''Storage provider, e.g. ALIYUN_OSS'' AFTER file_url',
  'SELECT ''storage_provider exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'document' AND column_name = 'storage_bucket') = 0,
  'ALTER TABLE document ADD COLUMN storage_bucket varchar(128) DEFAULT NULL COMMENT ''Storage bucket name'' AFTER storage_provider',
  'SELECT ''storage_bucket exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'document' AND column_name = 'storage_object_key') = 0,
  'ALTER TABLE document ADD COLUMN storage_object_key varchar(700) DEFAULT NULL COMMENT ''Storage object key'' AFTER storage_bucket',
  'SELECT ''storage_object_key exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'document' AND column_name = 'file_hash') = 0,
  'ALTER TABLE document ADD COLUMN file_hash varchar(128) DEFAULT NULL COMMENT ''SHA-256 file hash'' AFTER mime_type',
  'SELECT ''file_hash exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'document' AND column_name = 'etag') = 0,
  'ALTER TABLE document ADD COLUMN etag varchar(128) DEFAULT NULL COMMENT ''OSS ETag'' AFTER file_hash',
  'SELECT ''etag exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @database_name AND table_name = 'document' AND column_name = 'upload_status') = 0,
  'ALTER TABLE document ADD COLUMN upload_status varchar(32) DEFAULT NULL COMMENT ''Upload status'' AFTER etag',
  'SELECT ''upload_status exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE document
SET original_name = COALESCE(original_name, name),
    upload_status = CASE
      WHEN type = 1 AND upload_status IS NULL THEN 'LEGACY'
      ELSE upload_status
    END,
    update_time = update_time
WHERE original_name IS NULL
   OR (type = 1 AND upload_status IS NULL);

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @database_name AND table_name = 'document' AND index_name = 'idx_document_storage_key') = 0,
  'ALTER TABLE document ADD INDEX idx_document_storage_key (tenant_code, storage_provider, storage_object_key(191))',
  'SELECT ''idx_document_storage_key exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @database_name AND table_name = 'document' AND index_name = 'idx_document_storage_usage') = 0,
  'ALTER TABLE document ADD INDEX idx_document_storage_usage (tenant_code, is_deleted, type, file_size)',
  'SELECT ''idx_document_storage_usage exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
