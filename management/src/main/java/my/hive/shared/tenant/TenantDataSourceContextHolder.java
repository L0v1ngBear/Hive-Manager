package my.hive.shared.tenant;

public final class TenantDataSourceContextHolder {

    private static final ThreadLocal<String> DATASOURCE_KEY = new ThreadLocal<>();

    private TenantDataSourceContextHolder() {
    }

    public static void set(String datasourceKey) {
        DATASOURCE_KEY.set(datasourceKey);
    }

    public static String get() {
        return DATASOURCE_KEY.get();
    }

    public static void clear() {
        DATASOURCE_KEY.remove();
    }
}
