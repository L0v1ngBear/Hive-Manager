package my.hive.infrastructure.sms;

/**
 * 短信发送接口，用于把业务提醒推送到外部短信服务。
 */
public interface SmsSender {

    boolean send(SmsMessage message);
}
