package my.hive.infrastructure.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import my.hive.shared.external.ExternalApiGuardService;
import my.hive.infrastructure.sms.SmsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * 阿里云短信发送器。
 *
 * <p>线上配置 provider=aliyun、签名和模板后启用；验证码模板变量默认使用 code。</p>
 */
@Component
@ConditionalOnProperty(prefix = "notification.sms", name = "provider", havingValue = "aliyun")
public class AliyunSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsSender.class);
    private static final String SUCCESS_CODE = "OK";

    @Resource
    private SmsProperties smsProperties;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ExternalApiGuardService externalApiGuardService;

    @Value("${external-api.guard.sms.max-calls-per-phone-window:5}")
    private Integer smsMaxCallsPerPhoneWindow;

    @Value("${external-api.guard.sms.window-seconds:3600}")
    private Integer smsWindowSeconds;

    @Override
    public boolean send(SmsMessage message) {
        if (!isConfigured(message)) {
            log.warn("Aliyun SMS sender is not fully configured, skip sending.");
            return false;
        }
        try {
            externalApiGuardService.checkRateLimit(
                    "aliyun-sms",
                    "send",
                    externalApiGuardService.fingerprint(message.phone().trim()),
                    smsMaxCallsPerPhoneWindow == null ? 5 : smsMaxCallsPerPhoneWindow,
                    Duration.ofSeconds(smsWindowSeconds == null ? 3600 : Math.max(1, smsWindowSeconds))
            );
            Client client = createClient();
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(message.phone().trim())
                    .setSignName(smsProperties.getSignName().trim())
                    .setTemplateCode(smsProperties.getTemplateCode().trim())
                    .setTemplateParam(toTemplateParamJson(message));
            SendSmsResponse response = client.sendSms(request);
            String resultCode = response == null || response.getBody() == null ? null : response.getBody().getCode();
            boolean success = SUCCESS_CODE.equalsIgnoreCase(resultCode);
            if (!success) {
                String resultMessage = response == null || response.getBody() == null ? "empty response" : response.getBody().getMessage();
                log.warn("Aliyun SMS send failed, phone={}, code={}, message={}", maskPhone(message.phone()), resultCode, resultMessage);
            }
            return success;
        } catch (Exception exception) {
            log.error("Aliyun SMS send exception, phone={}", maskPhone(message.phone()), exception);
            return false;
        }
    }

    private Client createClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(smsProperties.getAccessKey().trim())
                .setAccessKeySecret(smsProperties.getAccessSecret().trim());
        config.endpoint = smsProperties.getEndpoint() == null || smsProperties.getEndpoint().isBlank()
                ? "dysmsapi.aliyuncs.com"
                : smsProperties.getEndpoint().trim();
        return new Client(config);
    }

    private boolean isConfigured(SmsMessage message) {
        return Boolean.TRUE.equals(smsProperties.getEnabled())
                && message != null
                && hasText(message.phone())
                && hasText(smsProperties.getAccessKey())
                && hasText(smsProperties.getAccessSecret())
                && hasText(smsProperties.getSignName())
                && hasText(smsProperties.getTemplateCode());
    }

    private String toTemplateParamJson(SmsMessage message) throws JsonProcessingException {
        Map<String, String> params = message.templateParams() == null || message.templateParams().isEmpty()
                ? Map.of("content", message.content() == null ? "" : message.content())
                : message.templateParams();
        return objectMapper.writeValueAsString(params);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String maskPhone(String phone) {
        String normalized = phone == null ? "" : phone.trim();
        if (normalized.length() <= 7) {
            return "***";
        }
        return normalized.substring(0, 3) + "****" + normalized.substring(normalized.length() - 4);
    }
}
