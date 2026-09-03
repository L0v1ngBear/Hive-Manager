-- 客户来源与导入审计。历史客户无法从既有数据可靠反推来源，因此保留 NULL 表示来源未记录。
ALTER TABLE `customer`
  ADD COLUMN `source_type` VARCHAR(20) NULL COMMENT 'manual/import/after_sales；NULL表示历史来源未记录' AFTER `opening_date`,
  ADD COLUMN `import_time` DATETIME NULL COMMENT '客户导入时间' AFTER `source_type`,
  ADD COLUMN `import_user_id` BIGINT NULL COMMENT '执行导入的用户ID' AFTER `import_time`,
  ADD COLUMN `import_user_name` VARCHAR(80) NULL COMMENT '执行导入的用户名快照' AFTER `import_user_id`,
  ADD KEY `idx_customer_tenant_source_import_time` (`tenant_code`, `source_type`, `import_time`);

-- 业务方已确认：客户 53 所在租户中，编号 53 之后均来自 2026-09-02 的客户导入。
UPDATE `customer` AS `target`
JOIN `customer` AS `anchor`
  ON `anchor`.`id` = 53
 AND `anchor`.`tenant_code` = `target`.`tenant_code`
SET `target`.`source_type` = 'import',
    `target`.`import_time` = '2026-09-02 00:00:00',
    `target`.`import_user_id` = NULL,
    `target`.`import_user_name` = '历史导入（人工确认）'
WHERE `target`.`id` > 53;
