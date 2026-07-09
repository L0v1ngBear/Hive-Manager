package my.management.module.order.model.enums;

/**
 * 订单状态日志操作类型。
 */
public enum OrderLogOperateTypeEnum {
    CREATE("create"),
    STATUS_CHANGE("status_change"),
    PROCESS_CHANGE("process_change"),
    ROLLBACK_PENDING("rollback_pending"),
    ROLLBACK_APPROVED("rollback_approved"),
    SYNC("sync");

    private final String code;

    OrderLogOperateTypeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
