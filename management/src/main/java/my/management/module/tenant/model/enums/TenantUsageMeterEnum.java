package my.management.module.tenant.model.enums;

/**
 * 租户用量计量类型。
 */
public enum TenantUsageMeterEnum {
    AI_ADVICE("AI_ADVICE");

    private final String code;

    TenantUsageMeterEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
