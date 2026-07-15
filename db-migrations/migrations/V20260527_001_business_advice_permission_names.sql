UPDATE sys_permission
SET perm_name = CASE perm_code
  WHEN 'dashboard:ai' THEN '经营建议中心'
  WHEN 'dashboard:ai:view' THEN '查看全部经营建议'
  WHEN 'dashboard:ai:*' THEN '经营建议-全部维度'
  WHEN 'dashboard:ai:inventory' THEN '经营建议-库存维度'
  WHEN 'dashboard:ai:order' THEN '经营建议-订单维度'
  WHEN 'dashboard:ai:customer' THEN '经营建议-客户维度'
  WHEN 'dashboard:ai:quality' THEN '经营建议-质量维度'
  WHEN 'dashboard:ai:finance' THEN '经营建议-财务维度'
  WHEN 'dashboard:ai:employee' THEN '经营建议-员工维度'
  WHEN 'dashboard:ai:operation' THEN '经营建议-运营维度'
  ELSE perm_name
END
WHERE perm_code IN (
  'dashboard:ai',
  'dashboard:ai:view',
  'dashboard:ai:*',
  'dashboard:ai:inventory',
  'dashboard:ai:order',
  'dashboard:ai:customer',
  'dashboard:ai:quality',
  'dashboard:ai:finance',
  'dashboard:ai:employee',
  'dashboard:ai:operation'
)
  AND IFNULL(is_deleted, 0) = 0;

UPDATE sys_role
SET role_name = '经营建议管理员'
WHERE role_code = 'AUTO_DASHBOARD_AI'
  AND IFNULL(is_deleted, 0) = 0;
