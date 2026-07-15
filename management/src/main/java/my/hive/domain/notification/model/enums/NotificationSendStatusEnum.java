package my.hive.domain.notification.model.enums;

/**
 * 通知发送状态。
 */
public enum NotificationSendStatusEnum {
    PENDING("PENDING");

    private final String code;

    NotificationSendStatusEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
