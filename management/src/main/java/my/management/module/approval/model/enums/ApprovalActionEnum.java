package my.management.module.approval.model.enums;

/**
 * 审批动作。
 */
public enum ApprovalActionEnum {
    APPROVE(1),
    REJECT(2);

    private final Integer code;

    ApprovalActionEnum(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public static boolean isApprove(Integer action) {
        return APPROVE.code.equals(action);
    }
}
