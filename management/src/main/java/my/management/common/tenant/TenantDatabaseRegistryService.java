package my.management.common.tenant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * TenantDatabaseRegistryService 属于管理端后端通用能力层，属于租户隔离基础设施，当前服务字段隔离方案，同时为未来一租户一库预留扩展点。
 */
@Service
@ConditionalOnProperty(prefix = "tenant.isolation", name = "mode", havingValue = "DATABASE")
public class TenantDatabaseRegistryService {

    private final JdbcTemplate bootstrapJdbcTemplate;
    private final TenantIsolationProperties properties;

    public TenantDatabaseRegistryService(@Qualifier("bootstrapJdbcTemplate") JdbcTemplate bootstrapJdbcTemplate,
                                         TenantIsolationProperties properties) {
        this.bootstrapJdbcTemplate = bootstrapJdbcTemplate;
        this.properties = properties;
    }

    // Future DATABASE mode uses a shared registry table instead of hardcoding one
    // datasource block per tenant in yml. This keeps tenant growth operationally manageable.
    public Map<String, TenantDatabaseProperties> loadEnabledConfigs() {
        String table = validateTableName();
        List<TenantDatabaseProperties> rows = bootstrapJdbcTemplate.query(
                "SELECT tenant_code, datasource_key, jdbc_url, jdbc_username, jdbc_password, jdbc_driver_class " +
                        "FROM " + table + " WHERE enabled = 1",
                (rs, rowNum) -> {
                    TenantDatabaseProperties db = new TenantDatabaseProperties();
                    db.setDatasourceKey(rs.getString("datasource_key"));
                    db.setUrl(rs.getString("jdbc_url"));
                    db.setUsername(rs.getString("jdbc_username"));
                    db.setPassword(rs.getString("jdbc_password"));
                    db.setDriverClassName(rs.getString("jdbc_driver_class"));
                    db.setTenantCode(rs.getString("tenant_code"));
                    return db;
                }
        );

        Map<String, TenantDatabaseProperties> result = new LinkedHashMap<>();
        for (TenantDatabaseProperties row : rows) {
            result.put(row.getTenantCode(), row);
        }
        return result;
    }

    // Management login currently does not submit tenantCode. Before DATABASE mode
    // is enabled, a shared identity index must be able to locate tenantCode first.
    public String resolveDatasourceKey(String tenantCode) {
        TenantDatabaseProperties db = loadEnabledConfigs().get(tenantCode);
        if (db == null || db.getDatasourceKey() == null || db.getDatasourceKey().isBlank()) {
            throw new IllegalStateException("No tenant datasource config found for tenantCode=" + tenantCode);
        }
        return db.getDatasourceKey();
    }

    private String validateTableName() {
        String table = properties.getRegistryTable();
        if (!table.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalStateException("Invalid tenant registry table name: " + table);
        }
        return table;
    }
}
