package my.management.module.tenant.model.enums;

import java.util.Set;

/**
 * 租户套餐及默认额度。
 */
public enum TenantPlanEnum {
    TRIAL("TRIAL", "试用版", 5, 30, 512, false),
    STARTER("STARTER", "入门版", 10, 80, 1024, false),
    STANDARD("STANDARD", "标准版", 30, 300, 5120, false),
    PROFESSIONAL("PROFESSIONAL", "专业版", 80, 1000, 20480, true),
    PRIVATE("PRIVATE", "私有部署版", 9999, 100000, 102400, true);

    private final String code;
    private final String label;
    private final int maxUsers;
    private final int maxAiAdvicePerMonth;
    private final int storageQuotaMb;
    private final boolean advancedAiIncluded;

    TenantPlanEnum(String code, String label, int maxUsers, int maxAiAdvicePerMonth, int storageQuotaMb, boolean advancedAiIncluded) {
        this.code = code;
        this.label = label;
        this.maxUsers = maxUsers;
        this.maxAiAdvicePerMonth = maxAiAdvicePerMonth;
        this.storageQuotaMb = storageQuotaMb;
        this.advancedAiIncluded = advancedAiIncluded;
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

    public int getMaxAiAdvicePerMonth() {
        return maxAiAdvicePerMonth;
    }

    public int getStorageQuotaMb() {
        return storageQuotaMb;
    }

    public boolean isAdvancedAiIncluded() {
        return advancedAiIncluded;
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
