package my.management.common.tenant;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 租户路由数据源。
 * 只有未来启用 DATABASE 模式时才会真正按租户切库，
 * 当前 FIELD 模式下仍然回落到默认数据源。
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private final String defaultDatasourceKey;

    public TenantRoutingDataSource(String defaultDatasourceKey) {
        this.defaultDatasourceKey = defaultDatasourceKey;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String currentKey = TenantDataSourceContextHolder.get();
        return (currentKey == null || currentKey.isBlank()) ? defaultDatasourceKey : currentKey;
    }
}
