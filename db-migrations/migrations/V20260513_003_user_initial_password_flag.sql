-- 新用户首次登录网页端强制修改初始密码标记。
-- 只追加字段，现有用户默认 0，避免上线后老账号被误拦截。

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user'
      AND column_name = 'must_change_password'
);

SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE `user` ADD COLUMN `must_change_password` TINYINT NOT NULL DEFAULT 0 COMMENT ''首次登录是否必须修改密码'' AFTER `password`',
    'SELECT ''must_change_password exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
