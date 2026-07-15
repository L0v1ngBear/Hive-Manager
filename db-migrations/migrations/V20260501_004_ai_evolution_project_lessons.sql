-- Seed platform engineering lessons into the AI self-evolution sample store.
-- These rows are deliberately PLATFORM scoped: they are reusable system learning material, not tenant business data.

INSERT INTO ai_advice_training_sample (
    tenant_code,
    sample_key,
    category,
    title,
    source_type,
    priority,
    confidence,
    input_snapshot_json,
    behavior_context_json,
    advice_json,
    label_status,
    feedback_type,
    feedback_text,
    feedback_time,
    occurrence_count,
    create_time,
    update_time
) VALUES
(
    'PLATFORM',
    'PLATFORM_LESSON:20260501:release-rollback-discipline',
    'platform_release',
    '线上发布必须具备快照、迁移、回退闭环',
    'platform_lessons',
    'P0',
    98,
    JSON_OBJECT(
        'incidentType', 'release_safety',
        'observedRisk', '直接重启或手动补丁会扩大线上故障影响面',
        'requiredFlow', JSON_ARRAY('健康检查', '创建回退快照', '停止后端写入', '执行版本化迁移', '启动服务', '日志验证')
    ),
    JSON_OBJECT('source', 'codex_project_memory', 'capturedAt', '2026-05-01'),
    JSON_OBJECT(
        'title', '发布前必须先建立回退点',
        'summary', '线上更新不能只重启服务，必须先创建可恢复快照并保证迁移可重复执行。',
        'suggestion', '所有发布脚本按健康检查、快照、迁移、启动、验证顺序执行；失败时优先回退上一个稳定版本。',
        'firstAction', '发布前检查 backups/releases/latest 和数据库备份是否生成。',
        'reviewMetric', '发布失败后是否能在 10 分钟内回退到稳定版本'
    ),
    'resolved',
    'resolved',
    '已沉淀为发布和回退纪律，后续线上问题按先止血、再定位、再修复执行。',
    NOW(),
    1,
    NOW(),
    NOW()
),
(
    'PLATFORM',
    'PLATFORM_LESSON:20260501:versioned-db-migrations-only',
    'platform_migration',
    '表结构变更必须进入版本化迁移清单',
    'platform_lessons',
    'P0',
    99,
    JSON_OBJECT(
        'incidentType', 'schema_drift',
        'observedRisk', '本地库、部署包、线上库结构不一致会导致 Unknown column 或启动失败',
        'requiredFiles', JSON_ARRAY('db-migrations/migration_manifest.txt', 'db-migrations/migrations/*.sql', 'C:/Users/HUAWEI/Desktop/hive专用')
    ),
    JSON_OBJECT('source', 'codex_project_memory', 'capturedAt', '2026-05-01'),
    JSON_OBJECT(
        'title', '数据库结构以迁移系统为准',
        'summary', '不能依赖启动时建表或人工线上补字段，所有表结构都必须用幂等 SQL 管理。',
        'suggestion', '新增表、字段、索引、权限种子、XXL-JOB 表兼容修复时，同步追加迁移 SQL 和 manifest。',
        'firstAction', '开发完成后检查 migration_manifest 是否包含本次新增 SQL。',
        'reviewMetric', 'fresh deploy 后是否能自动创建完整结构'
    ),
    'resolved',
    'resolved',
    '已沉淀为数据库迁移纪律，后续变更不再跳过 manifest。',
    NOW(),
    1,
    NOW(),
    NOW()
),
(
    'PLATFORM',
    'PLATFORM_LESSON:20260501:mysql-storage-first-diagnosis',
    'platform_migration',
    'MySQL 存储异常优先排查数据目录和引擎状态',
    'platform_lessons',
    'P0',
    96,
    JSON_OBJECT(
        'incidentType', 'mysql_storage_error',
        'symptoms', JSON_ARRAY('Got error 168', 'Failed to create schema directory', 'OS error 71', '/var/lib/mysql write_probe failed'),
        'rootCauseHint', '这类问题通常不是业务 SQL 错，而是数据目录不可写、挂载异常或 InnoDB 文件状态异常'
    ),
    JSON_OBJECT('source', 'codex_project_memory', 'capturedAt', '2026-05-01'),
    JSON_OBJECT(
        'title', 'DDL 失败先做存储诊断',
        'summary', '出现存储引擎通用错误时，不要继续反复跑迁移。',
        'suggestion', '先执行 MySQL 存储诊断，确认 datadir、权限、磁盘、inode、InnoDB 状态，再决定重建或恢复。',
        'firstAction', '停止业务写入，运行诊断脚本并保留日志。',
        'reviewMetric', 'DDL 探针是否通过'
    ),
    'resolved',
    'resolved',
    '已沉淀为 MySQL 故障优先级判断，避免把存储问题误判为 SQL 问题。',
    NOW(),
    1,
    NOW(),
    NOW()
),
(
    'PLATFORM',
    'PLATFORM_LESSON:20260501:xxl-job-managed-scheduler',
    'platform_scheduler',
    '定时任务统一由 XXL-JOB 管理并自动种子',
    'platform_lessons',
    'P1',
    94,
    JSON_OBJECT(
        'incidentType', 'scheduler_management',
        'observedRisk', 'Spring @Scheduled 和人工后台建任务会造成环境不一致',
        'targetTasks', JSON_ARRAY('AI 建议快照', '数据清理', '容量巡检', '通知闭环', '考勤统计')
    ),
    JSON_OBJECT('source', 'codex_project_memory', 'capturedAt', '2026-05-01'),
    JSON_OBJECT(
        'title', '任务调度需要产品化管理',
        'summary', '定时任务不应散落在代码和人工控制台操作中。',
        'suggestion', '新增任务时同时维护 XXL-JOB 种子 SQL、执行器配置和线上兼容迁移。',
        'firstAction', '新增任务前检查 xxl_job_info 是否可由迁移自动生成。',
        'reviewMetric', '新环境重建后任务是否自动出现'
    ),
    'resolved',
    'resolved',
    '已沉淀为定时任务治理规则。',
    NOW(),
    1,
    NOW(),
    NOW()
),
(
    'PLATFORM',
    'PLATFORM_LESSON:20260501:tenant-feature-isolation',
    'platform_commercialization',
    '客户定制能力必须前后端三层隔离',
    'platform_lessons',
    'P0',
    97,
    JSON_OBJECT(
        'incidentType', 'tenant_feature_isolation',
        'observedRisk', '只隐藏菜单会被直接调用接口绕过',
        'requiredLayers', JSON_ARRAY('featureFlags', '前端菜单和路由', '后端 @RequireTenantFeature')
    ),
    JSON_OBJECT('source', 'codex_project_memory', 'capturedAt', '2026-05-01'),
    JSON_OBJECT(
        'title', '定制功能不能只靠前端隐藏',
        'summary', '租户商业化能力必须通过功能码控制可见性和接口访问。',
        'suggestion', '基础模块使用 module.xxx，专属定制使用 custom.xxx；所有业务控制器按模块加 RequireTenantFeature。',
        'firstAction', '新增页面时同步定义 route meta.features 和后端功能注解。',
        'reviewMetric', '未开通租户直接请求接口是否被拒绝'
    ),
    'resolved',
    'resolved',
    '已沉淀为 SaaS 商业化隔离规则。',
    NOW(),
    1,
    NOW(),
    NOW()
),
(
    'PLATFORM',
    'PLATFORM_LESSON:20260501:ai-advice-permission-scope',
    'platform_ai_governance',
    'AI 高维建议必须按权限和视角分发',
    'platform_lessons',
    'P1',
    95,
    JSON_OBJECT(
        'incidentType', 'ai_visibility_governance',
        'observedRisk', '普通员工看到经营、财务、客户等高维建议会造成信息越权',
        'dimensions', JSON_ARRAY('经营', '员工', '客户', '库存', '质量', '财务', '交付')
    ),
    JSON_OBJECT('source', 'codex_project_memory', 'capturedAt', '2026-05-01'),
    JSON_OBJECT(
        'title', 'AI 建议需要分视角和权限',
        'summary', 'AI 建议不只做经营建议，也不能所有人都看全量建议。',
        'suggestion', '按 dashboard:ai:* 和维度权限控制建议可见范围，高维建议仅给老板、负责人或授权角色。',
        'firstAction', '新增 AI 维度时同步权限种子和前后端校验。',
        'reviewMetric', '低权限账号是否无法看到高维建议'
    ),
    'resolved',
    'resolved',
    '已沉淀为 AI 治理规则。',
    NOW(),
    1,
    NOW(),
    NOW()
),
(
    'PLATFORM',
    'PLATFORM_LESSON:20260501:dashboard-snapshot-fast-path',
    'platform_performance',
    '总览和 AI 建议慢查询必须走快照和缓存',
    'platform_lessons',
    'P1',
    94,
    JSON_OBJECT(
        'incidentType', 'performance',
        'observedRisk', '用户打开页面时临时聚合全量业务数据会造成响应慢',
        'solutions', JSON_ARRAY('索引', '预计算快照', 'Redis 缓存', '分页和时间范围限制', '全局 loading')
    ),
    JSON_OBJECT('source', 'codex_project_memory', 'capturedAt', '2026-05-01'),
    JSON_OBJECT(
        'title', '慢接口优先改成快路径',
        'summary', '总览大盘和 AI 建议页应读预计算结果，而不是每次页面打开都重算。',
        'suggestion', '用 XXL-JOB 生成 AI 建议快照和经营摘要，接口优先读取快照，失败再降级实时计算。',
        'firstAction', '为慢查询建立耗时日志和快照命中率指标。',
        'reviewMetric', '总览接口 P95 响应时间是否下降'
    ),
    'resolved',
    'resolved',
    '已沉淀为性能优化规则。',
    NOW(),
    1,
    NOW(),
    NOW()
),
(
    'PLATFORM',
    'PLATFORM_LESSON:20260501:oss-document-metadata',
    'platform_storage',
    '文件上传体系需要 OSS 抽象和文档元数据',
    'platform_lessons',
    'P1',
    92,
    JSON_OBJECT(
        'incidentType', 'file_storage_design',
        'observedRisk', '业务直接依赖本地路径会阻碍 OSS、权限、容量和迁移',
        'metadata', JSON_ARRAY('storageProvider', 'bucket', 'objectKey', 'fileHash', 'etag', 'mimeType', 'fileSize', 'uploadStatus')
    ),
    JSON_OBJECT('source', 'codex_project_memory', 'capturedAt', '2026-05-01'),
    JSON_OBJECT(
        'title', '文档系统应以对象存储元数据为核心',
        'summary', '文件上传需要 StorageService 抽象，document 表只存稳定元数据。',
        'suggestion', 'OSS key 作为源真相，URL 只作展示兼容；容量统计和租户隔离基于元数据完成。',
        'firstAction', '接入 OSS key、bucket、hash 和容量索引。',
        'reviewMetric', '文件是否能跨环境迁移且租户隔离正确'
    ),
    'resolved',
    'resolved',
    '已沉淀为文件系统设计规则。',
    NOW(),
    1,
    NOW(),
    NOW()
),
(
    'PLATFORM',
    'PLATFORM_LESSON:20260501:secrets-and-common-versioning',
    'platform_engineering',
    '密钥和公共包变更必须可追踪',
    'platform_lessons',
    'P0',
    96,
    JSON_OBJECT(
        'incidentType', 'engineering_hygiene',
        'observedRisk', '密钥进入日志或 common 包未版本化会导致安全和部署事故',
        'rules', JSON_ARRAY('不打印 .env', '密钥泄露后轮换', 'common 包固定版本', '非 git 目录显式记录')
    ),
    JSON_OBJECT('source', 'codex_project_memory', 'capturedAt', '2026-05-01'),
    JSON_OBJECT(
        'title', '工程卫生是商用稳定性的底座',
        'summary', '配置、密钥、公共依赖和编译产物必须可追踪、可复现。',
        'suggestion', '公共包变更必须发布固定版本；检查配置时不输出敏感值；构建前分别编译两个后端和管理端前端。',
        'firstAction', '改 common 或 .env 相关逻辑时同步做版本和泄露检查。',
        'reviewMetric', '其他机器是否能在无本机隐藏依赖的情况下编译'
    ),
    'resolved',
    'resolved',
    '已沉淀为工程质量规则。',
    NOW(),
    1,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    title = VALUES(title),
    source_type = VALUES(source_type),
    priority = VALUES(priority),
    confidence = VALUES(confidence),
    input_snapshot_json = VALUES(input_snapshot_json),
    behavior_context_json = VALUES(behavior_context_json),
    advice_json = VALUES(advice_json),
    label_status = VALUES(label_status),
    feedback_type = VALUES(feedback_type),
    feedback_text = VALUES(feedback_text),
    feedback_time = VALUES(feedback_time),
    occurrence_count = GREATEST(occurrence_count, VALUES(occurrence_count)),
    update_time = NOW();
