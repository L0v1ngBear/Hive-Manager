package my.management.common.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * TenantIsolationProperties 属于管理端后端通用能力层，属于租户隔离基础设施，当前服务字段隔离方案，同时为未来一租户一库预留扩展点。
 */
@ConfigurationProperties
public class TenantIsolationProperties {

    /**
     * Keep FIELD as the current behavior. DATABASE is reserved for future
     * one-tenant-one-database routing and is intentionally not enabled now.
     */
    private TenantIsolationMode mode = TenantIsolationMode.FIELD;
    private String defaultDatasourceKey = "default";
    private String registryTable = "tenant_database_config";

    public TenantIsolationMode getMode() {
        return mode;
    }

    public void setMode(TenantIsolationMode mode) {
        this.mode = mode == null ? TenantIsolationMode.FIELD : mode;
    }

    public String getDefaultDatasourceKey() {
        return defaultDatasourceKey;
    }

    public void setDefaultDatasourceKey(String defaultDatasourceKey) {
        this.defaultDatasourceKey = (defaultDatasourceKey == null || defaultDatasourceKey.isBlank()) ? "default" : defaultDatasourceKey;
    }

    public String getRegistryTable() {
        return registryTable;
    }

    public void setRegistryTable(String registryTable) {
        this.registryTable = (registryTable == null || registryTable.isBlank()) ? "tenant_database_config" : registryTable;
    }
}
