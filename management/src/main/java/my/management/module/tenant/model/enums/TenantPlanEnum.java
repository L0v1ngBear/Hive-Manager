package my.management.module.tenant.model.enums;

import java.util.Set;

/**
 * 租户套餐及默认额度。
 */
public enum TenantPlanEnum {
    TRIAL("TRIAL", "试用版", 5, 512),
    STARTER("STARTER", "入门版", 10, 1024),
    STANDARD("STANDARD", "标准版", 30, 5120),
    PROFESSIONAL("PROFESSIONAL", "专业版", 80, 20480),
    PRIVATE("PRIVATE", "私有部署版", 9999, 102400);

    private final String code;
    private final String label;
    private final int maxUsers;
    private final int storageQuotaMb;

    TenantPlanEnum(String code, String label, int maxUsers, int storageQuotaMb) {
        this.code = code;
        this.label = label;
        this.maxUsers = maxUsers;
        this.storageQuotaMb = storageQuotaMb;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public int getMaxUsers() {
        return maxUsers;
    }

    public int getStorageQuotaMb() {
        return storageQuotaMb;
    }

    public static Set<String> allowedCodes() {
        return Set.of(TRIAL.code, STARTER.code, STANDARD.code, PROFESSIONAL.code, PRIVATE.code);
    }

    public static TenantPlanEnum of(String code) {
        if (code == null || code.isBlank()) {
            return TRIAL;
        }
        for (TenantPlanEnum item : values()) {
            if (item.code.equalsIgnoreCase(code.trim())) {
                return item;
            }
        }
        return TRIAL;
    }
}
