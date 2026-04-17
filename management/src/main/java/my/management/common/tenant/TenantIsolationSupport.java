package my.management.common.tenant;

import my.management.common.context.TenantPermissionContext;
import org.springframework.stereotype.Component;
/**
 * TenantIsolationSupport 属于管理端后端通用能力层，属于租户隔离基础设施，当前服务字段隔离方案，同时为未来一租户一库预留扩展点。
 */
@Component
public class TenantIsolationSupport {

    private final TenantIsolationProperties properties;
    private final TenantDatabaseRegistryService registryService;

    public TenantIsolationSupport(TenantIsolationProperties properties,
                                  @org.springframework.beans.factory.annotation.Autowired(required = false) TenantDatabaseRegistryService registryService) {
        this.properties = properties;
        this.registryService = registryService;
    }

    public TenantIsolationMode getMode() {
        return properties.getMode();
    }

    // FIELD is the current production behavior: all tenants share one schema and
    // MyBatis Plus appends tenant_code automatically.
    public boolean useTenantColumn() {
        return getMode() == TenantIsolationMode.FIELD;
    }

    // In DATABASE mode the tenant filter must stop injecting tenant_code, because
    // the database itself becomes the isolation boundary.
    public boolean shouldIgnoreTenantLine() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        return !useTenantColumn() || tenantCode == null || tenantCode.isBlank();
    }

    public String getTenantColumn() {
        return "tenant_code";
    }

    // Interceptors bind the resolved tenant datasource before service/mapper execution.
    public void bindTenantDatasource(String tenantCode) {
        if (!useTenantColumn()) {
            TenantDataSourceContextHolder.set(resolveDatasourceKey(tenantCode));
        }
    }

    public void clearTenantDatasource() {
        TenantDataSourceContextHolder.clear();
    }

    // The shared registry table is only consulted in DATABASE mode. Until then,
    // every request stays on the default datasource.
    public String resolveDatasourceKey(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank() || registryService == null) {
            return properties.getDefaultDatasourceKey();
        }
        return registryService.resolveDatasourceKey(tenantCode);
    }

    /*
     * Future extension point:
     * DATABASE mode is already scaffolded here but remains disabled by config.
     * To switch later, populate the shared tenant registry table and change mode to DATABASE.
     */
}
