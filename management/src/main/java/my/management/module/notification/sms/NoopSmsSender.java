package my.management.module.notification.sms;

import jakarta.annotation.Resource;
import my.management.module.notification.config.SmsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 默认短信发送器。
 *
 * <p>在没有配置真实短信供应商前，系统只记录站内通知，不会向外发送短信。
 * 后续新增供应商时实现 SmsSender 并按 provider 条件启用即可。</p>
 */
@Component
@ConditionalOnProperty(prefix = "notification.sms", name = "provider", havingValue = "noop", matchIfMissing = true)
public class NoopSmsSender implements SmsSender {

    @Resource
    private SmsProperties smsProperties;

    @Override
    public boolean send(SmsMessage message) {
        return Boolean.TRUE.equals(smsProperties.getEnabled()) && false;
    }
}
