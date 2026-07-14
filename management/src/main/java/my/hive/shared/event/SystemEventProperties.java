package my.hive.shared.event;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "hive.system-event")
public class SystemEventProperties {

    private boolean enabled = true;

    private String sourceApp = "hive";

    private int maxTitleLength = 180;

    private int maxContentLength = 1000;

    private int maxJsonLength = 4000;
}
