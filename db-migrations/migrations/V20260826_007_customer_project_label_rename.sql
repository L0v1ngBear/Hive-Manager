-- 客户管理统一使用“项目名称”表述；仅替换系统历史默认文案，不覆盖租户自行定义的其他名称。
UPDATE tenant_field_config
SET field_label = '项目名称'
WHERE module_code = 'customer'
  AND field_key = 'projectName'
  AND field_label = '合作项目';
