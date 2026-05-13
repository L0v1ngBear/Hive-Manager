package my.management.module.notification.sms;

import jakarta.annotation.Resource;
import my.management.module.notification.config.SmsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 本地开发短信发送器。
 *
 * <p>只在 provider=console 时启用，用于本地联调验证码流程；生产环境不得使用。</p>
 */
@Component
@ConditionalOnProperty(prefix = "notification.sms", name = "provider", havingValue = "console")
public class ConsoleSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsSender.class);

    @Resource
    private SmsProperties smsProperties;

    @Override
    public boolean send(SmsMessage message) {
        if (!Boolean.TRUE.equals(smsProperties.getEnabled()) || message == null || message.phone() == null) {
            return false;
        }
        log.warn("DEV SMS ONLY, phone={}, title={}, content={}", message.phone(), message.title(), message.content());
        return true;
    }
}
