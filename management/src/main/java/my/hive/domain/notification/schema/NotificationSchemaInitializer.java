package my.hive.domain.notification.schema;

/**
 * 通知模块表结构说明。
 *
 * <p>通知记录属于线上核心辅助表，不能在应用启动阶段自动执行 DDL。
 * 表结构由部署 SQL 手动执行维护，后续可迁移到 Flyway/Liquibase 等正式版本化工具。</p>
 */
public final class NotificationSchemaInitializer {

    /**
     * 手动执行脚本位置：部署包 mysql/manual/20260427_management_manual_schema.sql。
     */
    public static final String MANUAL_SQL = "mysql/manual/20260427_management_manual_schema.sql";

    private NotificationSchemaInitializer() {
        // 工具说明类，不需要实例化。
    }
}
