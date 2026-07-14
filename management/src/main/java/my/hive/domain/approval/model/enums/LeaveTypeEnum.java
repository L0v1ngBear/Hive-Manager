package my.hive.domain.approval.model.enums;

/**
 * 请假类型。
 */
public enum LeaveTypeEnum {
    PERSONAL(1, "事假"),
    SICK(2, "病假"),
    ANNUAL(3, "年假"),
    COMPENSATORY(4, "调休"),
    OTHER(-1, "其他"),
    EMPTY(null, "未填写");

    private final Integer code;
    private final String label;

    LeaveTypeEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public Integer getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static LeaveTypeEnum of(Integer code) {
        if (code == null) {
            return EMPTY;
        }
        for (LeaveTypeEnum item : values()) {
            if (code.equals(item.code)) {
                return item;
            }
        }
        return OTHER;
    }
}
