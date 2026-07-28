package my.hive.infrastructure.wechat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat.web-login")
public class WechatWebLoginProperties {
    private boolean enabled;
    private String appId;
    private String appSecret;
    private String callbackUri;
    private String frontendLoginPath = "/login";
}
