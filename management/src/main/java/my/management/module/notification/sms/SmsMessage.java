package my.management.module.notification.sms;

/**
 * 短信消息载荷。
 */
public record SmsMessage(String phone, String title, String content) {
}
