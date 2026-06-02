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

    private int capacityDiskTotalMb = 40960;

    private String capacityDiskPath = "/app";

    private int capacityPlatformReserveMb = 12288;

    private int capacityProjectionWarnPercent = 70;

    private int capacityProjectionFailPercent = 85;

    private int capacityEstimateTenants = 2;

    private int capacityEstimateDailyOrders = 300;

    private int capacityEstimateOrderItemsPerOrder = 4;

    private int capacityEstimateDailyClothIn = 800;

    private int capacityEstimateDailyClothOut = 600;

    private int capacityEstimateActiveEmployees = 150;

    private int capacityEstimateDailyOperationLogs = 8000;

    private int capacityEstimateDailyNotifications = 2000;

    private int capacityEstimateDailyPrintTasks = 800;

    private int capacityEstimateDailyAttachments = 40;

    private double capacityEstimateAvgAttachmentMb = 1.5;

    private double capacityEstimateLocalAttachmentRatio = 1.0;

    private double capacityEstimateSafetyFactor = 1.45;

    private int capacityEstimateBackupKeep = 3;

    private boolean runtimeAuditEnabled = true;

    private int runtimeAuditRecentMinutes = 30;

    private int runtimeAuditDbLatencyWarnMs = 500;

    private int runtimeAuditErrorWarnCount = 20;

    private int runtimeAuditErrorFailCount = 100;

    private int runtimeAuditSlowWarnCount = 50;

    private int runtimeAuditSlowFailCount = 200;

    private int runtimeAuditSystemErrorWarnCount = 10;

    private int runtimeAuditSystemErrorFailCount = 50;

    private int runtimeAuditNotificationBacklogWarnCount = 200;

    private int runtimeAuditNotificationBacklogFailCount = 1000;
}
