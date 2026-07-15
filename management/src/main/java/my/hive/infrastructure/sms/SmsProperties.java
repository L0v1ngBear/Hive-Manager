package my.hive.infrastructure.sms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 短信推送配置。
 *
 * <p>当前先预留统一配置入口，enabled 默认关闭；后续接阿里云、腾讯云等供应商时，
 * 只需要扩展 SmsSender 实现并读取这些配置，不需要改业务闭环逻辑。</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "notification.sms")
public class SmsProperties {

    private Boolean enabled = false;

    private String provider = "noop";

    private String accessKey;

    private String accessSecret;

    private String endpoint = "dysmsapi.aliyuncs.com";

    private String signName;

    private String templateCode;
}
