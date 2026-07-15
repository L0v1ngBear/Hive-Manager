package my.hive.shared.enums;

/**
 * 通用逻辑删除标记。
 */
public enum DeleteFlagEnum {
    NORMAL(0),
    DELETED(1);

    private final Integer code;

    DeleteFlagEnum(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public static boolean isDeleted(Integer value) {
        return DELETED.code.equals(value);
    }

    public static boolean isNormal(Integer value) {
        return value == null || NORMAL.code.equals(value);
    }
}
