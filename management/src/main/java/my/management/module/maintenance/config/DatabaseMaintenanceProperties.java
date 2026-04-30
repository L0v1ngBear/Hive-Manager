package my.management.module.maintenance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "maintenance.database")
public class DatabaseMaintenanceProperties {

    private boolean enabled = true;

    private boolean cleanupEnabled = true;

    private int batchSize = 5000;

    private int operationLogRetentionDays = 90;

    private int behaviorEventRetentionDays = 90;

    private int aiSampleRetentionDays = 365;

    private int notificationRetentionDays = 180;

    private int printTaskRetentionDays = 180;

    private int orderStatusLogRetentionDays = 730;

    private int systemEventRetentionDays = 180;

    private int capacityWarnMb = 32768;
}
