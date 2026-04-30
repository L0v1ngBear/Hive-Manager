package my.management.module.maintenance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.management.module.maintenance.config.DatabaseMaintenanceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseMaintenanceService {

    private static final String CLEANUP_LOCK_NAME = "hive_database_maintenance_cleanup";

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseMaintenanceProperties properties;

    public Map<String, Object> buildCapacityReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        BigDecimal totalMb = queryDatabaseSizeMb();
        List<Map<String, Object>> largestTables = queryLargestTables();
        List<Map<String, Object>> candidates = queryCleanupCandidates();

        report.put("generatedAt", LocalDateTime.now());
        report.put("databaseTotalMb", totalMb);
        report.put("capacityWarnMb", properties.getCapacityWarnMb());
        report.put("capacityWarning", totalMb.compareTo(BigDecimal.valueOf(Math.max(properties.getCapacityWarnMb(), 1))) >= 0);
        report.put("largestTables", largestTables);
        report.put("cleanupCandidates", candidates);

        log.info("database capacity report: totalMb={}, largestTables={}, cleanupCandidates={}",
                totalMb, largestTables, candidates);
        return report;
    }

    public Map<String, Object> cleanupExpiredData() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startedAt", LocalDateTime.now());

        if (!properties.isEnabled() || !properties.isCleanupEnabled()) {
            result.put("skipped", true);
            result.put("reason", "database maintenance cleanup is disabled");
            log.info("database maintenance cleanup skipped: disabled");
            return result;
        }

        if (!tryAcquireCleanupLock()) {
            result.put("skipped", true);
            result.put("reason", "another cleanup task is running");
            log.info("database maintenance cleanup skipped: lock busy");
            return result;
        }

        List<Map<String, Object>> tableResults = new ArrayList<>();
        int deletedTotal = 0;
        try {
            tableResults.add(cleanupTable("operation_log", "create_time",
                    properties.getOperationLogRetentionDays(), ""));
            tableResults.add(cleanupTable("behavior_event", "create_time",
                    properties.getBehaviorEventRetentionDays(), ""));
            tableResults.add(cleanupAiSamples());
            tableResults.add(cleanupNotifications());
            tableResults.add(cleanupPrintTasks());
            tableResults.add(cleanupTable("sales_order_status_log", "create_time",
                    properties.getOrderStatusLogRetentionDays(), ""));
            tableResults.add(cleanupTable("production_order_status_log", "create_time",
                    properties.getOrderStatusLogRetentionDays(), ""));
            tableResults.add(cleanupTable("system_event", "create_time",
                    properties.getSystemEventRetentionDays(), ""));

            for (Map<String, Object> tableResult : tableResults) {
                Object value = tableResult.get("deletedRows");
                if (value instanceof Number number) {
                    deletedTotal += number.intValue();
                }
            }
            result.put("deletedTotal", deletedTotal);
            result.put("tables", tableResults);
            result.put("finishedAt", LocalDateTime.now());
            log.info("database maintenance cleanup finished: deletedTotal={}, tables={}", deletedTotal, tableResults);
            return result;
        } finally {
            releaseCleanupLock();
        }
    }

    private BigDecimal queryDatabaseSizeMb() {
        BigDecimal value = jdbcTemplate.queryForObject("""
                SELECT COALESCE(ROUND(SUM(data_length + index_length) / 1024 / 1024, 2), 0)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                """, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<Map<String, Object>> queryLargestTables() {
        return jdbcTemplate.queryForList("""
                SELECT
                  table_name AS tableName,
                  table_rows AS tableRows,
                  ROUND(data_length / 1024 / 1024, 2) AS dataMb,
                  ROUND(index_length / 1024 / 1024, 2) AS indexMb,
                  ROUND((data_length + index_length) / 1024 / 1024, 2) AS totalMb
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                ORDER BY (data_length + index_length) DESC
                LIMIT 20
                """);
    }

    private List<Map<String, Object>> queryCleanupCandidates() {
        List<Map<String, Object>> candidates = new ArrayList<>();
        candidates.add(countCandidate("operation_log", "create_time",
                properties.getOperationLogRetentionDays(), ""));
        candidates.add(countCandidate("behavior_event", "create_time",
                properties.getBehaviorEventRetentionDays(), ""));
        candidates.add(countAiSampleCandidate());
        candidates.add(countNotificationCandidate());
        candidates.add(countPrintTaskCandidate());
        candidates.add(countCandidate("sales_order_status_log", "create_time",
                properties.getOrderStatusLogRetentionDays(), ""));
        candidates.add(countCandidate("production_order_status_log", "create_time",
                properties.getOrderStatusLogRetentionDays(), ""));
        candidates.add(countCandidate("system_event", "create_time",
                properties.getSystemEventRetentionDays(), ""));
        return candidates;
    }

    private Map<String, Object> countAiSampleCandidate() {
        if (!tableExists("ai_advice_training_sample") || !columnExists("ai_advice_training_sample", "label_status")) {
            return skipped("ai_advice_training_sample", "table or label_status column missing");
        }
        return countCandidate("ai_advice_training_sample", "update_time",
                properties.getAiSampleRetentionDays(), "`label_status` IN ('unlabeled','ignored')");
    }

    private Map<String, Object> countNotificationCandidate() {
        if (!tableExists("notification_record")) {
            return skipped("notification_record", "table missing");
        }
        if (columnExists("notification_record", "task_status")) {
            return countCandidate("notification_record", "create_time",
                    properties.getNotificationRetentionDays(), "`task_status` IN ('DONE','IGNORED')");
        }
        if (columnExists("notification_record", "read_flag")) {
            return countCandidate("notification_record", "create_time",
                    properties.getNotificationRetentionDays(), "`read_flag` = 1");
        }
        return skipped("notification_record", "no safe closed/read column");
    }

    private Map<String, Object> countPrintTaskCandidate() {
        if (!tableExists("print_task") || !columnExists("print_task", "status")) {
            return skipped("print_task", "table or status column missing");
        }
        return countCandidate("print_task", "create_time",
                properties.getPrintTaskRetentionDays(), "`status` IN ('SUCCESS','FAILED','CANCELLED','DONE')");
    }

    private Map<String, Object> cleanupAiSamples() {
        if (!tableExists("ai_advice_training_sample") || !columnExists("ai_advice_training_sample", "label_status")) {
            return skipped("ai_advice_training_sample", "table or label_status column missing");
        }
        return cleanupTable("ai_advice_training_sample", "update_time",
                properties.getAiSampleRetentionDays(), "`label_status` IN ('unlabeled','ignored')");
    }

    private Map<String, Object> cleanupNotifications() {
        if (!tableExists("notification_record")) {
            return skipped("notification_record", "table missing");
        }
        if (columnExists("notification_record", "task_status")) {
            return cleanupTable("notification_record", "create_time",
                    properties.getNotificationRetentionDays(), "`task_status` IN ('DONE','IGNORED')");
        }
        if (columnExists("notification_record", "read_flag")) {
            return cleanupTable("notification_record", "create_time",
                    properties.getNotificationRetentionDays(), "`read_flag` = 1");
        }
        return skipped("notification_record", "no safe closed/read column");
    }

    private Map<String, Object> cleanupPrintTasks() {
        if (!tableExists("print_task") || !columnExists("print_task", "status")) {
            return skipped("print_task", "table or status column missing");
        }
        return cleanupTable("print_task", "create_time",
                properties.getPrintTaskRetentionDays(), "`status` IN ('SUCCESS','FAILED','CANCELLED','DONE')");
    }

    private Map<String, Object> countCandidate(String table, String dateColumn, int retentionDays, String condition) {
        if (!tableExists(table)) {
            return skipped(table, "table missing");
        }
        if (!columnExists(table, dateColumn)) {
            return skipped(table, "date column missing: " + dateColumn);
        }
        int safeDays = positiveDays(retentionDays);
        String whereClause = buildWhereClause(dateColumn, condition);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `" + table + "` WHERE " + whereClause,
                Integer.class,
                LocalDateTime.now().minusDays(safeDays)
        );
        Map<String, Object> item = baseResult(table, safeDays);
        item.put("candidateRows", count == null ? 0 : count);
        return item;
    }

    private Map<String, Object> cleanupTable(String table, String dateColumn, int retentionDays, String condition) {
        if (!tableExists(table)) {
            return skipped(table, "table missing");
        }
        if (!columnExists(table, dateColumn)) {
            return skipped(table, "date column missing: " + dateColumn);
        }

        int safeDays = positiveDays(retentionDays);
        int limit = Math.min(Math.max(properties.getBatchSize(), 100), 50000);
        String whereClause = buildWhereClause(dateColumn, condition);
        String orderClause = columnExists(table, "id") ? " ORDER BY `id`" : "";

        int deletedTotal = 0;
        int deletedBatch;
        do {
            deletedBatch = jdbcTemplate.update(
                    "DELETE FROM `" + table + "` WHERE " + whereClause + orderClause + " LIMIT ?",
                    LocalDateTime.now().minusDays(safeDays),
                    limit
            );
            deletedTotal += deletedBatch;
        } while (deletedBatch >= limit);

        Map<String, Object> item = baseResult(table, safeDays);
        item.put("deletedRows", deletedTotal);
        return item;
    }

    private String buildWhereClause(String dateColumn, String condition) {
        String whereClause = "`" + dateColumn + "` < ?";
        if (condition != null && !condition.isBlank()) {
            whereClause += " AND (" + condition + ")";
        }
        return whereClause;
    }

    private int positiveDays(int days) {
        return days <= 0 ? 1 : days;
    }

    private Map<String, Object> baseResult(String table, int retentionDays) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("tableName", table);
        item.put("retentionDays", retentionDays);
        return item;
    }

    private Map<String, Object> skipped(String table, String reason) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("tableName", table);
        item.put("skipped", true);
        item.put("reason", reason);
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

    private boolean tryAcquireCleanupLock() {
        Integer lockResult = jdbcTemplate.queryForObject("SELECT GET_LOCK(?, 0)", Integer.class, CLEANUP_LOCK_NAME);
        return lockResult != null && lockResult == 1;
    }

    private void releaseCleanupLock() {
        try {
            jdbcTemplate.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, CLEANUP_LOCK_NAME);
        } catch (Exception ignored) {
        }
    }
}
