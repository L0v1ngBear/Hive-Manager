package my.hive.shared.tenant;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;
/**
 * TenantRoutingDataSourceConfig 属于管理端后端通用能力层，属于租户隔离基础设施，当前服务字段隔离方案，同时为未来一租户一库预留扩展点。
 */
@Configuration
@ConditionalOnProperty(prefix = "tenant.isolation", name = "mode", havingValue = "DATABASE")
public class TenantRoutingDataSourceConfig {

    @Bean
    // This JdbcTemplate always talks to the shared/default database. Future registry
    // lookups must stay here because tenant库路由信息本身不能再依赖租户库。
    public JdbcTemplate bootstrapJdbcTemplate(@Qualifier("dataSource") DataSource defaultDataSource) {
        return new JdbcTemplate(defaultDataSource);
    }

    @Bean
    @Primary
    // Pre-build tenant datasources from the shared registry table once at startup.
    // Current deployments keep FIELD mode, so this bean stays inactive until needed.
    public DataSource tenantRoutingDataSource(@Qualifier("dataSource") DataSource defaultDataSource,
                                              TenantIsolationProperties properties,
                                              TenantDatabaseRegistryService registryService) {
        Map<Object, Object> targetDataSources = new LinkedHashMap<>();
        targetDataSources.put(properties.getDefaultDatasourceKey(), defaultDataSource);

        registryService.loadEnabledConfigs().forEach((tenantCode, db) -> {
            if (db.getUrl() == null || db.getUrl().isBlank()) {
                return;
            }
            String datasourceKey = (db.getDatasourceKey() == null || db.getDatasourceKey().isBlank()) ? tenantCode : db.getDatasourceKey();
            targetDataSources.put(datasourceKey, buildDataSource(db));
        });

        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource(properties.getDefaultDatasourceKey());
        routingDataSource.setDefaultTargetDataSource(defaultDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }

    private DataSource buildDataSource(TenantDatabaseProperties db) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(db.getUrl());
        dataSource.setUsername(db.getUsername());
        dataSource.setPassword(db.getPassword());
        dataSource.setDriverClassName(db.getDriverClassName());
        return dataSource;
    }
}
