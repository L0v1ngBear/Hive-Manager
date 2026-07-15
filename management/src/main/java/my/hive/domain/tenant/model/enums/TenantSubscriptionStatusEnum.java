package my.hive.domain.tenant.model.enums;

import java.util.Set;

/**
 * 租户订阅状态。
 */
public enum TenantSubscriptionStatusEnum {
    TRIAL("TRIAL"),
    ACTIVE("ACTIVE"),
    EXPIRED("EXPIRED"),
    SUSPENDED("SUSPENDED");

    private final String code;

    TenantSubscriptionStatusEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean matches(String value) {
        return code.equalsIgnoreCase(value);
    }

    public static Set<String> allowedCodes() {
        return Set.of(TRIAL.code, ACTIVE.code, EXPIRED.code, SUSPENDED.code);
    }

    public static boolean isUnavailable(String value) {
        return SUSPENDED.matches(value) || EXPIRED.matches(value);
    }
}
