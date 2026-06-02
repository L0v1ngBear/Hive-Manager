package my.management.module.maintenance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.management.module.maintenance.config.DatabaseMaintenanceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuntimeStabilityAuditService {

    private static final List<String> CRITICAL_TABLES = List.of(
            "user",
            "sys_role",
            "sys_permission",
            "sales_order",
            "cloth",
            "inventory_record",
            "notification_record",
            "system_event",
            "operation_log"
    );

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseMaintenanceProperties properties;

    public Map<String, Object> buildAuditReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        List<Map<String, Object>> findings = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(Math.max(properties.getRuntimeAuditRecentMinutes(), 1));

        report.put("generatedAt", now);
        report.put("recentMinutes", Math.max(properties.getRuntimeAuditRecentMinutes(), 1));
        if (!properties.isRuntimeAuditEnabled()) {
            report.put("status", "SKIPPED");
            report.put("skipped", true);
            report.put("reason", "runtime stability audit is disabled");
            report.put("findings", findings);
            return report;
        }

        long dbLatencyMs = measureDatabaseLatencyMs();
        report.put("databaseLatencyMs", dbLatencyMs);
        if (dbLatencyMs >= Math.max(properties.getRuntimeAuditDbLatencyWarnMs(), 1)) {
            findings.add(finding("WARN", "数据库响应变慢", "当前数据库探针耗时 " + dbLatencyMs + "ms"));
        }

        List<String> missingTables = findMissingCriticalTables();
        report.put("missingCriticalTables", missingTables);
        if (!missingTables.isEmpty()) {
            findings.add(finding("ERROR", "核心业务表缺失", String.join(",", missingTables)));
        }

        int failedMigrations = countFailedMigrations();
        report.put("failedMigrationCount", failedMigrations);
        if (failedMigrations > 0) {
            findings.add(finding("ERROR", "存在失败的数据库迁移", "失败迁移数量 " + failedMigrations));
        }

        int recentOperationErrors = countOperationErrors(cutoff);
        report.put("recentOperationErrorCount", recentOperationErrors);
        addCountFinding(findings, recentOperationErrors,
                properties.getRuntimeAuditErrorWarnCount(),
                properties.getRuntimeAuditErrorFailCount(),
                "最近业务错误偏多",
                "最近业务错误过多");

        int recentSlowOperations = countSlowOperations(cutoff);
        report.put("recentSlowOperationCount", recentSlowOperations);
        addCountFinding(findings, recentSlowOperations,
                properties.getRuntimeAuditSlowWarnCount(),
                properties.getRuntimeAuditSlowFailCount(),
                "最近慢请求偏多",
                "最近慢请求过多");

        int recentSystemErrors = countSystemErrors(cutoff);
        report.put("recentSystemErrorCount", recentSystemErrors);
        addCountFinding(findings, recentSystemErrors,
                properties.getRuntimeAuditSystemErrorWarnCount(),
                properties.getRuntimeAuditSystemErrorFailCount(),
                "最近系统错误偏多",
                "最近系统错误过多");

        int notificationBacklog = countNotificationBacklog();
        report.put("notificationBacklogCount", notificationBacklog);
        addCountFinding(findings, notificationBacklog,
                properties.getRuntimeAuditNotificationBacklogWarnCount(),
                properties.getRuntimeAuditNotificationBacklogFailCount(),
                "待办通知积压偏多",
                "待办通知积压过多");

        report.put("topOperationErrors", queryTopOperationErrors(cutoff));
        report.put("findings", findings);
        report.put("status", resolveStatus(findings));

        log.info("runtime stability audit report: status={}, findings={}", report.get("status"), findings);
        return report;
    }

    private long measureDatabaseLatencyMs() {
        long start = System.nanoTime();
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return Math.max((System.nanoTime() - start) / 1_000_000L, 0L);
    }

    private List<String> findMissingCriticalTables() {
        List<String> missing = new ArrayList<>();
        for (String table : CRITICAL_TABLES) {
            if (!tableExists(table)) {
                missing.add(table);
            }
        }
        return missing;
    }

    private int countFailedMigrations() {
        if (!tableExists("schema_migration_history") || !columnExists("schema_migration_history", "status")) {
            return 0;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM schema_migration_history
                WHERE UPPER(COALESCE(status, '')) <> 'SUCCESS'
                """, Integer.class);
        return count == null ? 0 : Math.max(count, 0);
    }

    private int countOperationErrors(LocalDateTime cutoff) {
        if (!tableExists("operation_log") || !columnExists("operation_log", "success") || !columnExists("operation_log", "create_time")) {
            return 0;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM operation_log
                WHERE create_time >= ?
                  AND COALESCE(success, 1) = 0
                """, Integer.class, cutoff);
        return count == null ? 0 : Math.max(count, 0);
    }

    private int countSlowOperations(LocalDateTime cutoff) {
        if (!tableExists("operation_log") || !columnExists("operation_log", "slow") || !columnExists("operation_log", "create_time")) {
            return 0;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM operation_log
                WHERE create_time >= ?
                  AND COALESCE(slow, 0) = 1
                """, Integer.class, cutoff);
        return count == null ? 0 : Math.max(count, 0);
    }

    private int countSystemErrors(LocalDateTime cutoff) {
        if (!tableExists("system_event") || !columnExists("system_event", "level") || !columnExists("system_event", "create_time")) {
            return 0;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM system_event
                WHERE create_time >= ?
                  AND UPPER(COALESCE(level, '')) = 'ERROR'
                """, Integer.class, cutoff);
        return count == null ? 0 : Math.max(count, 0);
    }

    private int countNotificationBacklog() {
        if (!tableExists("notification_record") || !columnExists("notification_record", "status")) {
            return 0;
        }
        String taskClause = columnExists("notification_record", "task_status")
                ? " AND UPPER(COALESCE(task_status, 'PENDING')) NOT IN ('DONE','IGNORED','CLOSED')"
                : "";
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM notification_record
                WHERE COALESCE(status, 1) = 1
                """ + taskClause, Integer.class);
        return count == null ? 0 : Math.max(count, 0);
    }

    private List<Map<String, Object>> queryTopOperationErrors(LocalDateTime cutoff) {
        if (!tableExists("operation_log") || !columnExists("operation_log", "success") || !columnExists("operation_log", "create_time")) {
            return List.of();
        }
        return jdbcTemplate.queryForList("""
                SELECT
                  COALESCE(module, 'unknown') AS module,
                  COALESCE(action, 'unknown') AS action,
                  COALESCE(error_type, 'unknown') AS errorType,
                  LEFT(COALESCE(error_message, ''), 160) AS errorMessage,
                  COUNT(*) AS errorCount
                FROM operation_log
                WHERE create_time >= ?
                  AND COALESCE(success, 1) = 0
                GROUP BY COALESCE(module, 'unknown'), COALESCE(action, 'unknown'), COALESCE(error_type, 'unknown'), LEFT(COALESCE(error_message, ''), 160)
                ORDER BY COUNT(*) DESC
                LIMIT 8
                """, cutoff);
    }

    private void addCountFinding(List<Map<String, Object>> findings,
                                 int count,
                                 int warnCount,
                                 int failCount,
                                 String warnTitle,
                                 String failTitle) {
        int safeWarn = Math.max(warnCount, 1);
        int safeFail = Math.max(failCount, safeWarn + 1);
        if (count >= safeFail) {
            findings.add(finding("ERROR", failTitle, "数量 " + count));
        } else if (count >= safeWarn) {
            findings.add(finding("WARN", warnTitle, "数量 " + count));
        }
    }

    private String resolveStatus(List<Map<String, Object>> findings) {
        boolean hasWarn = false;
        for (Map<String, Object> finding : findings) {
            Object level = finding.get("level");
            if ("ERROR".equals(level)) {
                return "ERROR";
            }
            if ("WARN".equals(level)) {
                hasWarn = true;
            }
        }
        return hasWarn ? "WARN" : "OK";
    }

    private Map<String, Object> finding(String level, String title, String message) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("level", level);
        item.put("title", title);
        item.put("message", message);
        return item;
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """, Integer.class, table);
        return count != null && count > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }
}
