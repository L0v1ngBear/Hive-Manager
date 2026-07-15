package my.hive.domain.notification.model.enums;

/**
 * 通知闭环任务状态。
 */
public enum NotificationTaskStatusEnum {
    PENDING("PENDING"),
    DONE("DONE"),
    IGNORED("IGNORED");

    private final String code;

    NotificationTaskStatusEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean matches(String value) {
        return code.equalsIgnoreCase(value);
    }

    public static NotificationTaskStatusEnum normalizeCloseStatus(String value) {
        return IGNORED.matches(value) ? IGNORED : DONE;
    }
}
