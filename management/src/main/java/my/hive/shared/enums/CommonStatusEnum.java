package my.hive.shared.enums;

/**
 * 通用启停状态。仅用于字段语义明确为启用/停用的场景。
 */
public enum CommonStatusEnum {
    DISABLED(0),
    ENABLED(1);

    private final Integer code;

    CommonStatusEnum(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public static boolean isEnabled(Integer value) {
        return ENABLED.code.equals(value);
    }

    public static boolean isDisabled(Integer value) {
        return value == null || DISABLED.code.equals(value);
    }
}
