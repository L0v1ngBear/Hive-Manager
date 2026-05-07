package my.management.common.enums;

import org.springframework.util.StringUtils;

/**
 * 平台级租户标识。
 */
public enum PlatformTenantEnum {
    SUPER("super");

    private final String code;

    PlatformTenantEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean matches(String tenantCode) {
        return StringUtils.hasText(tenantCode) && code.equalsIgnoreCase(tenantCode.trim());
    }

    public static boolean isSuper(String tenantCode) {
        return SUPER.matches(tenantCode);
    }
}
