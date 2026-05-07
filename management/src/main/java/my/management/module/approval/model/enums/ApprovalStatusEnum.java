package my.management.module.approval.model.enums;

/**
 * 审批状态。
 */
public enum ApprovalStatusEnum {
    PENDING(1, "待审批"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已拒绝"),
    UNKNOWN(-1, "未知");

    private final Integer code;
    private final String label;

    ApprovalStatusEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public Integer getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static boolean isPending(Integer status) {
        return PENDING.code.equals(status);
    }

    public static ApprovalStatusEnum of(Integer status) {
        if (status == null) {
            return UNKNOWN;
        }
        for (ApprovalStatusEnum item : values()) {
            if (item.code.equals(status)) {
                return item;
            }
        }
        return UNKNOWN;
    }
}
