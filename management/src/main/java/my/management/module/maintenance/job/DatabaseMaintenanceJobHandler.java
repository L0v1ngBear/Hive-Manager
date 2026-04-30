package my.management.module.maintenance.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.hive.common.event.SystemEvent;
import my.hive.common.event.SystemEventPublisher;
import my.management.module.maintenance.service.DatabaseMaintenanceService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseMaintenanceJobHandler {

    private final DatabaseMaintenanceService databaseMaintenanceService;
    private final SystemEventPublisher systemEventPublisher;

    @XxlJob("dbCapacityReportJob")
    public void dbCapacityReportJob() {
        try {
            Map<String, Object> report = databaseMaintenanceService.buildCapacityReport();
            XxlJobHelper.log("Database capacity report finished: {}", report);
            boolean capacityWarning = Boolean.TRUE.equals(report.get("capacityWarning"));
            systemEventPublisher.publish(SystemEvent.builder()
                    .eventType("DB_CAPACITY_REPORT")
                    .level(capacityWarning ? "WARN" : "INFO")
                    .module("maintenance")
                    .title(capacityWarning ? "数据库容量接近预警线" : "数据库容量巡检完成")
                    .content("当前数据库容量 " + report.get("databaseTotalMb") + " MB")
                    .bizType("xxl-job")
                    .bizNo("dbCapacityReportJob")
                    .detail(report)
                    .build());
        } catch (Exception ex) {
            log.error("database capacity report job failed", ex);
            XxlJobHelper.log("Database capacity report failed: {}", ex.getMessage());
            systemEventPublisher.error("DB_CAPACITY_REPORT_FAILED", "数据库容量巡检失败", ex,
                    Map.of("job", "dbCapacityReportJob"));
            XxlJobHelper.handleFail(ex.getMessage());
        }
    }

    @XxlJob("dbCleanupJob")
    public void dbCleanupJob() {
        try {
            Map<String, Object> result = databaseMaintenanceService.cleanupExpiredData();
            XxlJobHelper.log("Database cleanup finished: {}", result);
            systemEventPublisher.publish(SystemEvent.builder()
                    .eventType("DB_CLEANUP_FINISHED")
                    .level(Boolean.TRUE.equals(result.get("skipped")) ? "WARN" : "INFO")
                    .module("maintenance")
                    .title(Boolean.TRUE.equals(result.get("skipped")) ? "数据库清理任务跳过" : "数据库清理任务完成")
                    .content("清理行数: " + result.getOrDefault("deletedTotal", 0))
                    .bizType("xxl-job")
                    .bizNo("dbCleanupJob")
                    .detail(result)
                    .build());
        } catch (Exception ex) {
            log.error("database cleanup job failed", ex);
            XxlJobHelper.log("Database cleanup failed: {}", ex.getMessage());
            systemEventPublisher.error("DB_CLEANUP_FAILED", "数据库清理任务失败", ex,
                    Map.of("job", "dbCleanupJob"));
            XxlJobHelper.handleFail(ex.getMessage());
        }
    }
}
