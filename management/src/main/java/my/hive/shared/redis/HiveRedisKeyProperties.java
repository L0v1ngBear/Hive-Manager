package my.hive.shared.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "hive.redis")
public class HiveRedisKeyProperties {

    private String namespace = "hive";

    private String environment = "local";
}
