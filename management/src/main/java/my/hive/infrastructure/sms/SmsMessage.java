package my.hive.infrastructure.sms;

import java.util.Map;

/**
 * 短信消息载荷。
 */
public record SmsMessage(String phone, String title, String content, Map<String, String> templateParams) {

    public SmsMessage(String phone, String title, String content) {
        this(phone, title, content, Map.of());
    }
}
