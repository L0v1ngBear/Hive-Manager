package my.management.module.maintenance.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.hive.common.event.SystemEvent;
import my.hive.common.event.SystemEventPublisher;
import my.management.module.maintenance.service.DatabaseMaintenanceService;
import my.management.module.maintenance.service.RuntimeStabilityAuditService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseMaintenanceJobHandler {

    private final DatabaseMaintenanceService databaseMaintenanceService;
    private final RuntimeStabilityAuditService runtimeStabilityAuditService;
    private final SystemEventPublisher systemEventPublisher;

    @XxlJob("runtimeStabilityAuditJob")
    public void runtimeStabilityAuditJob() {
        try {
            Map<String, Object> report = runtimeStabilityAuditService.buildAuditReport();
            String status = String.valueOf(report.getOrDefault("status", "OK"));
            XxlJobHelper.log("Runtime stability audit finished: {}", report);
            systemEventPublisher.publish(SystemEvent.builder()
                    .eventType("RUNTIME_STABILITY_AUDIT")
                    .level("ERROR".equals(status) ? "ERROR" : ("WARN".equals(status) ? "WARN" : "INFO"))
                    .module("maintenance")
                    .title(("OK".equals(status) || "SKIPPED".equals(status)) ? "系统运行稳定性巡检完成" : "系统运行稳定性存在风险")
                    .content(runtimeAuditSummary(report))
                    .bizType("xxl-job")
                    .bizNo("runtimeStabilityAuditJob")
                    .detail(report)
                    .build());
        } catch (Exception ex) {
            log.error("runtime stability audit job failed", ex);
            XxlJobHelper.log("Runtime stability audit failed: {}", ex.getMessage());
            systemEventPublisher.error("RUNTIME_STABILITY_AUDIT_FAILED", "系统运行稳定性巡检失败", ex,
                    Map.of("job", "runtimeStabilityAuditJob"));
            XxlJobHelper.handleFail(ex.getMessage());
        }
    }

    @XxlJob("dbCapacityReportJob")
    public void dbCapacityReportJob() {
        try {
            Map<String, Object> report = databaseMaintenanceService.buildCapacityReport();
            XxlJobHelper.log("Database capacity report finished: {}", report);
            boolean capacityWarning = Boolean.TRUE.equals(report.get("capacityWarning"));
            boolean capacityFail = Boolean.TRUE.equals(report.get("capacityProjectionFail"));
            systemEventPublisher.publish(SystemEvent.builder()
                    .eventType("DB_CAPACITY_REPORT")
                    .level(capacityFail ? "ERROR" : (capacityWarning ? "WARN" : "INFO"))
                    .module("maintenance")
                    .title(capacityFail ? "数据库容量预测高风险"
                            : (capacityWarning ? "数据库容量接近预警线" : "数据库容量巡检完成"))
                    .content("当前数据库容量 " + report.get("databaseTotalMb") + " MB" + capacityProjectionSummary(report))
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

    @SuppressWarnings("unchecked")
    private String capacityProjectionSummary(Map<String, Object> report) {
        Object projectionValue = report.get("capacityProjection");
        if (!(projectionValue instanceof Map<?, ?> projection)) {
            return "";
        }
        Object monthsValue = projection.get("months");
        if (!(monthsValue instanceof Iterable<?> months)) {
            return "";
        }
        Map<String, Object> selected = null;
        for (Object itemValue : months) {
            if (!(itemValue instanceof Map<?, ?> item)) {
                continue;
            }
            Object monthValue = item.get("months");
            if (monthValue instanceof Number number && number.intValue() == 12) {
                selected = (Map<String, Object>) item;
                break;
            }
            if (selected == null) {
                selected = (Map<String, Object>) item;
            }
        }
        if (selected == null) {
            return "";
        }
        return "，12个月预测使用率 " + selected.get("diskUsedPercent") + "%，状态 " + selected.get("status");
    }

    @SuppressWarnings("unchecked")
    private String runtimeAuditSummary(Map<String, Object> report) {
        String status = String.valueOf(report.getOrDefault("status", "OK"));
        Object findingsValue = report.get("findings");
        int findingCount = findingsValue instanceof List<?> list ? list.size() : 0;
        if (findingCount == 0) {
            return "状态 " + status + "，未发现运行风险";
        }
        if (findingsValue instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            return "状态 " + status + "，风险 " + findingCount + " 项：" + first.get("title");
        }
        return "状态 " + status + "，风险 " + findingCount + " 项";
    }
}
