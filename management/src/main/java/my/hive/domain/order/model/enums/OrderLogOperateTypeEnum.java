package my.hive.domain.order.model.enums;

/**
 * 订单状态日志操作类型。
 */
public enum OrderLogOperateTypeEnum {
    CREATE("create"),
    STATUS_CHANGE("status_change"),
    PROCESS_CHANGE("process_change"),
    APPROVAL_PENDING("approval_pending"),
    NOTE_CREATE("note_create"),
    NOTE_UPDATE("note_update"),
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
