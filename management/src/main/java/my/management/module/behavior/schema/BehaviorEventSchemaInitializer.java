package my.management.module.behavior.schema;

/**
 * 用户行为采集表结构说明。
 *
 * <p>行为数据是 AI 个性化建议的训练/校准样本，必须按租户隔离存储。
 * 线上环境不在应用启动时自动建表，统一通过部署 SQL 手动执行，避免运行账号持有 DDL 权限。</p>
 */
public final class BehaviorEventSchemaInitializer {

    /**
     * 手动执行脚本位置：部署包 mysql/manual/20260427_management_manual_schema.sql。
     */
    public static final String MANUAL_SQL = "mysql/manual/20260427_management_manual_schema.sql";

    private BehaviorEventSchemaInitializer() {
        // 工具说明类，不需要实例化。
    }
}
