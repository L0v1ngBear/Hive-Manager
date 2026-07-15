-- Move wording changes out of historical migrations.
-- Executed migration files must remain byte-for-byte stable online.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

UPDATE `xxl_job`.`xxl_job_info`
SET `job_desc` = '经营建议快照刷新',
    `update_time` = NOW()
WHERE `executor_handler` = 'aiAdviceSnapshotRefreshJob';

UPDATE `tenant`
SET `tenant_name` = '平台管理组织',
    `package_name` = '平台管理版',
    `update_time` = NOW()
WHERE `tenant_code` = 'super';

UPDATE `user`
SET `department_name` = '平台管理',
    `position` = '平台管理员',
    `update_time` = NOW()
WHERE `tenant_code` = 'super'
  AND `login_name` = 'super';

UPDATE `sys_permission`
SET `perm_name` = '平台管理权限',
    `update_time` = NOW()
WHERE `perm_code` = 'super';

UPDATE `sys_role`
SET `role_name` = '平台管理员',
    `update_time` = NOW()
WHERE `tenant_code` = 'super'
  AND `role_code` = 'super';

UPDATE `ai_advice_training_sample`
SET `title` = '经营建议权限要按业务维度拆分',
    `feedback_text` = '高维经营建议只能给高权限用户看，普通用户只看自己负责范围内的建议。',
    `advice_json` = JSON_SET(
        COALESCE(`advice_json`, JSON_OBJECT()),
        '$.summary', '经营建议不能只做一个大视图，需要按经营、员工、客户、库存、订单等维度拆开。',
        '$.firstAction', '权限种子、后端注解、前端菜单、页面展示必须同步收敛。'
    ),
    `update_time` = NOW()
WHERE `tenant_code` = 'PLATFORM'
  AND `sample_key` = 'PLATFORM_LESSON:20260501:ai-advice-permission-scope';

UPDATE `ai_advice_training_sample`
SET `advice_json` = JSON_SET(
        COALESCE(`advice_json`, JSON_OBJECT()),
        '$.summary', '总览大盘要避免实时聚合慢查询，经营建议快照、库存趋势、待办数量都应走预计算或缓存。',
        '$.suggestion', '首页展示数据优先读快照；快照失败时返回可解释的降级数据。'
    ),
    `update_time` = NOW()
WHERE `tenant_code` = 'PLATFORM'
  AND `sample_key` = 'PLATFORM_LESSON:20260501:dashboard-snapshot-fast-path';

UPDATE `ai_advice_training_sample`
SET `input_snapshot_json` = JSON_SET(
        COALESCE(`input_snapshot_json`, JSON_OBJECT()),
        '$.targetTasks',
        JSON_ARRAY('经营建议快照', '通知闭环', '日志清理', '容量预警')
    ),
    `advice_json` = JSON_SET(
        COALESCE(`advice_json`, JSON_OBJECT()),
        '$.summary', '所有定时任务统一交给 XXL-JOB 管理，避免散落在应用内不可见。'
    ),
    `update_time` = NOW()
WHERE `tenant_code` = 'PLATFORM'
  AND `sample_key` = 'PLATFORM_LESSON:20260501:xxl-job-managed-scheduler';
