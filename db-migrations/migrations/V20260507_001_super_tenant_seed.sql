-- Seed platform developer tenant and super account.
-- Idempotent: safe to run more than once.
SET NAMES utf8mb4;

INSERT INTO `tenant` (
    `tenant_code`, `tenant_name`, `tenant_type`, `contact_person`, `contact_phone`, `password`,
    `status`, `package_code`, `package_name`, `subscription_status`, `subscription_start_time`,
    `subscription_end_time`, `max_users`, `max_ai_advice_per_month`, `max_storage_mb`,
    `feature_flags`, `creator`, `deleted`
) VALUES (
    'super', 'Platform Developer Tenant', 3, 'super', NULL,
    '$2a$10$XrilCqWQWYIWM16nkP6y.OSIcrW4Na9d2BDa95DwWVY2DM8mCD6LC',
    1, 'PLATFORM', 'Platform Developer Edition', 'ACTIVE', NOW(), '2099-12-31 23:59:59',
    999999, 999999, 1048576,
    JSON_OBJECT(
        'aiAdvice', true,
        'advancedAi', true,
        'platform.super', true,
        'document', true,
        'inventory', true,
        'salesOrder', true,
        'productionOrder', true,
        'attendance', true,
        'approval', true,
        'oss', true
    ),
    NULL, 0
) ON DUPLICATE KEY UPDATE
    `tenant_name` = VALUES(`tenant_name`),
    `tenant_type` = VALUES(`tenant_type`),
    `contact_person` = VALUES(`contact_person`),
    `password` = VALUES(`password`),
    `status` = VALUES(`status`),
    `package_code` = VALUES(`package_code`),
    `package_name` = VALUES(`package_name`),
    `subscription_status` = VALUES(`subscription_status`),
    `subscription_end_time` = VALUES(`subscription_end_time`),
    `max_users` = VALUES(`max_users`),
    `max_ai_advice_per_month` = VALUES(`max_ai_advice_per_month`),
    `max_storage_mb` = VALUES(`max_storage_mb`),
    `feature_flags` = VALUES(`feature_flags`),
    `deleted` = 0;

INSERT INTO `user` (
    `tenant_code`, `name`, `login_name`, `phone`, `password`, `department_name`, `position`,
    `manager_id`, `manager_name`, `status`, `role_level`, `phone_hash`, `phone_mask`
) VALUES (
    'super', 'super', 'super', NULL,
    '$2a$10$XrilCqWQWYIWM16nkP6y.OSIcrW4Na9d2BDa95DwWVY2DM8mCD6LC',
    'Platform Development', 'Platform Super Admin', NULL, NULL, 1, 3, NULL, NULL
) ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `password` = VALUES(`password`),
    `department_name` = VALUES(`department_name`),
    `position` = VALUES(`position`),
    `status` = VALUES(`status`),
    `role_level` = VALUES(`role_level`),
    `phone_hash` = NULL,
    `phone_mask` = NULL;

INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_type`, `sort`, `perm_name`, `is_deleted`)
SELECT 0, 'super', 1, 0, 'Developer Super Permission', 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission` WHERE `perm_code` = 'super'
);

UPDATE `sys_permission`
SET `parent_id` = 0,
    `perm_type` = 1,
    `sort` = 0,
    `perm_name` = 'Developer Super Permission',
    `is_deleted` = 0
WHERE `perm_code` = 'super';

INSERT INTO `sys_role` (`tenant_code`, `role_code`, `role_name`, `is_system`, `is_deleted`)
SELECT 'super', 'super', 'Developer Super Admin', 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role`
    WHERE `tenant_code` = 'super' AND `role_code` = 'super' AND IFNULL(`is_deleted`, 0) = 0
);

UPDATE `sys_role`
SET `role_name` = 'Developer Super Admin',
    `is_system` = 1,
    `is_deleted` = 0
WHERE `tenant_code` = 'super' AND `role_code` = 'super';

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`, `is_deleted`)
SELECT r.`id`, p.`id`, 0
FROM `sys_role` r
JOIN `sys_permission` p ON p.`perm_code` IN ('*', 'super') AND IFNULL(p.`is_deleted`, 0) = 0
WHERE r.`tenant_code` = 'super'
  AND r.`role_code` = 'super'
  AND IFNULL(r.`is_deleted`, 0) = 0
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission` rp
      WHERE rp.`role_id` = r.`id`
        AND rp.`permission_id` = p.`id`
        AND IFNULL(rp.`is_deleted`, 0) = 0
  );

UPDATE `sys_role_permission` rp
JOIN `sys_role` r ON rp.`role_id` = r.`id`
JOIN `sys_permission` p ON rp.`permission_id` = p.`id`
SET rp.`is_deleted` = 0
WHERE r.`tenant_code` = 'super'
  AND r.`role_code` = 'super'
  AND p.`perm_code` IN ('*', 'super');

INSERT INTO `sys_user_role` (`user_id`, `tenant_code`, `role_id`, `is_deleted`)
SELECT u.`id`, 'super', r.`id`, 0
FROM `user` u
JOIN `sys_role` r ON r.`tenant_code` = 'super' AND r.`role_code` = 'super' AND IFNULL(r.`is_deleted`, 0) = 0
WHERE u.`tenant_code` = 'super'
  AND u.`login_name` = 'super'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_user_role` ur
      WHERE ur.`user_id` = u.`id`
        AND ur.`tenant_code` = 'super'
        AND ur.`role_id` = r.`id`
        AND IFNULL(ur.`is_deleted`, 0) = 0
  );

UPDATE `sys_user_role` ur
JOIN `user` u ON ur.`user_id` = u.`id`
JOIN `sys_role` r ON ur.`role_id` = r.`id`
SET ur.`is_deleted` = 0
WHERE u.`tenant_code` = 'super'
  AND u.`login_name` = 'super'
  AND ur.`tenant_code` = 'super'
  AND r.`tenant_code` = 'super'
  AND r.`role_code` = 'super';
