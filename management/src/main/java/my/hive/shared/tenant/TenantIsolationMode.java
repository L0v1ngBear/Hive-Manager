package my.hive.shared.tenant;

/**
 * 租户隔离模式枚举。
 * 当前默认使用 FIELD，通过 tenant_code 字段做隔离。
 * DATABASE 为未来一租户一库预留，未正式启用前不要切换。
 */
public enum TenantIsolationMode {

    FIELD,
    DATABASE
}
