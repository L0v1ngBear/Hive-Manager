package my.hive.shared.tenant;

/**
 * 租户数据库连接信息模型。
 * 未来启用一租户一库时，由共享库中的注册表记录映射到这个对象。
 */
public class TenantDatabaseProperties {

    private String tenantCode;
    private String datasourceKey;
    private String url;
    private String username;
    private String password;
    private String driverClassName = "com.mysql.cj.jdbc.Driver";

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getDatasourceKey() {
        return datasourceKey;
    }

    public void setDatasourceKey(String datasourceKey) {
        this.datasourceKey = datasourceKey;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        if (driverClassName != null && !driverClassName.isBlank()) {
            this.driverClassName = driverClassName;
        }
    }
}
