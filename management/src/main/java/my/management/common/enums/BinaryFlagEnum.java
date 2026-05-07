package my.management.common.enums;

/**
 * 通用 0/1 标记，适用于数据库中以 0/1 保存的开关字段。
 */
public enum BinaryFlagEnum {
    NO(0),
    YES(1);

    private final Integer code;

    BinaryFlagEnum(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public static Integer codeOf(Boolean value) {
        return Boolean.TRUE.equals(value) ? YES.code : NO.code;
    }

    public static boolean isYes(Integer value) {
        return YES.code.equals(value);
    }

    public static boolean isNo(Integer value) {
        return value == null || NO.code.equals(value);
    }
}
