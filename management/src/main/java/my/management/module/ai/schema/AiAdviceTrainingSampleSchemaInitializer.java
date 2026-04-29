package my.management.module.ai.schema;

/**
 * AI 建议训练样本表结构说明。
 *
 * <p>线上环境不允许业务后端在启动阶段执行 CREATE/ALTER 等 DDL，避免运行账号需要过高权限，
 * 也避免表结构异常直接阻塞服务启动。该表结构统一由部署 SQL 手动执行维护。</p>
 */
public final class AiAdviceTrainingSampleSchemaInitializer {

    /**
     * 手动执行脚本位置：部署包 mysql/manual/20260427_management_manual_schema.sql。
     */
    public static final String MANUAL_SQL = "mysql/manual/20260427_management_manual_schema.sql";

    private AiAdviceTrainingSampleSchemaInitializer() {
        // 工具说明类，不需要实例化。
    }
}
