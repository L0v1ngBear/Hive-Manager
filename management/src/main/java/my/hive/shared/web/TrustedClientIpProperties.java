package my.hive.shared.web;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "hive.web.client-ip")
public class TrustedClientIpProperties {

    /**
     * Socket peers that are allowed to supply forwarding headers. Loopback is
     * trusted by default for the local nginx/container proxy deployment.
     */
    private List<String> trustedProxies = new ArrayList<>(List.of(
            "127.0.0.0/8",
            "::1/128"
    ));
}
