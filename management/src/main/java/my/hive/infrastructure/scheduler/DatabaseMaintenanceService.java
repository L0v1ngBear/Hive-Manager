package my.hive.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.hive.infrastructure.scheduler.DatabaseMaintenanceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private static final List<Integer> PROJECTION_MONTHS = List.of(6, 12, 24);
    private static final BigDecimal MB_BYTES = BigDecimal.valueOf(1024L * 1024L);

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseMaintenanceProperties properties;

    public Map<String, Object> buildCapacityReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        BigDecimal totalMb = queryDatabaseSizeMb();
        List<Map<String, Object>> largestTables = queryLargestTables();
        List<Map<String, Object>> candidates = queryCleanupCandidates();
        Map<String, Object> projection = buildCapacityProjection(totalMb);
        boolean currentWarning = totalMb.compareTo(BigDecimal.valueOf(Math.max(properties.getCapacityWarnMb(), 1))) >= 0;
        boolean projectionWarning = Boolean.TRUE.equals(projection.get("warning"));

        report.put("generatedAt", LocalDateTime.now());
        report.put("databaseTotalMb", totalMb);
        report.put("diskSnapshot", projection.get("diskSnapshot"));
        report.put("capacityWarnMb", properties.getCapacityWarnMb());
        report.put("currentCapacityWarning", currentWarning);
        report.put("capacityProjection", projection);
        report.put("capacityProjectionWarning", projectionWarning);
        report.put("capacityProjectionFail", Boolean.TRUE.equals(projection.get("fail")));
        report.put("capacityWarning", currentWarning || projectionWarning);
        report.put("largestTables", largestTables);
        report.put("cleanupCandidates", candidates);

        log.info("database capacity report: totalMb={}, currentWarning={}, projection={}, cleanupCandidates={}",
                totalMb, currentWarning, projection, candidates);
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

    private Map<String, Object> buildCapacityProjection(BigDecimal currentDbMb) {
        Map<String, Object> projection = new LinkedHashMap<>();
        Map<String, Object> actualGrowth = estimateActualMonthlyDatabaseGrowthMb();
        BigDecimal configuredMonthlyDbGrowthMb = estimateConfiguredMonthlyDatabaseGrowthMb();
        BigDecimal actualMonthlyDbGrowthMb = asBigDecimal(actualGrowth.get("monthlyDatabaseGrowthMb"));
        BigDecimal effectiveMonthlyDbGrowthMb = configuredMonthlyDbGrowthMb.max(actualMonthlyDbGrowthMb);
        BigDecimal monthlyLocalFileGrowthMb = estimateMonthlyLocalFileGrowthMb();
        BigDecimal backupHeadroomMb = effectiveMonthlyDbGrowthMb
                .multiply(BigDecimal.valueOf(Math.max(properties.getCapacityEstimateBackupKeep(), 0)));
        BigDecimal platformReserveMb = BigDecimal.valueOf(Math.max(properties.getCapacityPlatformReserveMb(), 0));
        Map<String, Object> diskSnapshot = queryDiskSnapshot();
        BigDecimal actualDiskTotalMb = asBigDecimal(diskSnapshot.get("totalMb"));
        BigDecimal actualDiskUsedMb = asBigDecimal(diskSnapshot.get("usedMb"));
        BigDecimal configuredDiskTotalMb = BigDecimal.valueOf(Math.max(properties.getCapacityDiskTotalMb(), 1));
        BigDecimal diskTotalMb = actualDiskTotalMb.compareTo(BigDecimal.ZERO) > 0 ? actualDiskTotalMb : configuredDiskTotalMb;
        BigDecimal projectionBaseUsedMb;
        String projectionBase;
        if (actualDiskUsedMb.compareTo(BigDecimal.ZERO) > 0) {
            projectionBaseUsedMb = actualDiskUsedMb;
            projectionBase = "filesystem";
        } else {
            projectionBaseUsedMb = platformReserveMb.add(currentDbMb);
            projectionBase = "configured-reserve";
        }

        List<Map<String, Object>> monthItems = new ArrayList<>();
        boolean warning = false;
        boolean fail = false;
        for (Integer months : PROJECTION_MONTHS) {
            BigDecimal projectedUsedMb = projectionBaseUsedMb
                    .add(effectiveMonthlyDbGrowthMb.multiply(BigDecimal.valueOf(months)))
                    .add(monthlyLocalFileGrowthMb.multiply(BigDecimal.valueOf(months)))
                    .add(backupHeadroomMb);
            BigDecimal usedPercent = projectedUsedMb
                    .multiply(BigDecimal.valueOf(100))
                    .divide(diskTotalMb, 2, RoundingMode.HALF_UP);
            String status = capacityStatus(usedPercent);
            warning = warning || !"OK".equals(status);
            fail = fail || "FAIL".equals(status);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("months", months);
            item.put("projectedUsedMb", round2(projectedUsedMb));
            item.put("diskUsedPercent", usedPercent);
            item.put("status", status);
            monthItems.add(item);
        }

        projection.put("diskTotalMb", round2(diskTotalMb));
        projection.put("configuredDiskTotalMb", properties.getCapacityDiskTotalMb());
        projection.put("diskSnapshot", diskSnapshot);
        projection.put("platformReserveMb", properties.getCapacityPlatformReserveMb());
        projection.put("projectionBase", projectionBase);
        projection.put("projectionBaseUsedMb", round2(projectionBaseUsedMb));
        projection.put("currentDatabaseMb", currentDbMb);
        projection.put("configuredMonthlyDatabaseGrowthMb", configuredMonthlyDbGrowthMb);
        projection.put("actualMonthlyDatabaseGrowthMb", actualMonthlyDbGrowthMb);
        projection.put("effectiveMonthlyDatabaseGrowthMb", effectiveMonthlyDbGrowthMb);
        projection.put("monthlyLocalFileGrowthMb", monthlyLocalFileGrowthMb);
        projection.put("backupHeadroomMb", backupHeadroomMb);
        projection.put("warningPercent", safePercent(properties.getCapacityProjectionWarnPercent(), 70));
        projection.put("failPercent", safePercent(properties.getCapacityProjectionFailPercent(), 85));
        projection.put("warning", warning);
        projection.put("fail", fail);
        projection.put("months", monthItems);
        projection.put("actualGrowthSources", actualGrowth.get("sources"));
        projection.put("businessAssumptions", buildCapacityBusinessAssumptions());
        return projection;
    }

    private Map<String, Object> queryDiskSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        String configuredPath = properties.getCapacityDiskPath();
        String path = configuredPath == null || configuredPath.isBlank() ? "/app" : configuredPath.trim();
        File target = new File(path);
        if (!target.exists()) {
            target = new File("/");
            snapshot.put("fallbackPath", target.getAbsolutePath());
        }
        snapshot.put("path", path);
        try {
            long totalBytes = target.getTotalSpace();
            long freeBytes = target.getUsableSpace();
            if (totalBytes <= 0) {
                snapshot.put("available", false);
                snapshot.put("reason", "disk total is unavailable");
                return snapshot;
            }
            long usedBytes = Math.max(totalBytes - Math.max(freeBytes, 0L), 0L);
            BigDecimal totalMb = bytesToMb(totalBytes);
            BigDecimal usedMb = bytesToMb(usedBytes);
            BigDecimal freeMb = bytesToMb(Math.max(freeBytes, 0L));
            BigDecimal usedPercent = usedMb.multiply(BigDecimal.valueOf(100))
                    .divide(totalMb, 2, RoundingMode.HALF_UP);
            snapshot.put("available", true);
            snapshot.put("resolvedPath", target.getAbsolutePath());
            snapshot.put("totalMb", totalMb);
            snapshot.put("usedMb", usedMb);
            snapshot.put("freeMb", freeMb);
            snapshot.put("usedPercent", usedPercent);
            return snapshot;
        } catch (SecurityException ex) {
            snapshot.put("available", false);
            snapshot.put("reason", ex.getMessage());
            return snapshot;
        }
    }

    private BigDecimal estimateConfiguredMonthlyDatabaseGrowthMb() {
        BigDecimal dailyBytesPerTenant = BigDecimal.ZERO;
        BigDecimal orders = BigDecimal.valueOf(Math.max(properties.getCapacityEstimateDailyOrders(), 0));
        BigDecimal itemsPerOrder = BigDecimal.valueOf(Math.max(properties.getCapacityEstimateOrderItemsPerOrder(), 0));
        BigDecimal clothIn = BigDecimal.valueOf(Math.max(properties.getCapacityEstimateDailyClothIn(), 0));
        BigDecimal clothOut = BigDecimal.valueOf(Math.max(properties.getCapacityEstimateDailyClothOut(), 0));

        dailyBytesPerTenant = dailyBytesPerTenant.add(orders.multiply(BigDecimal.valueOf(4096)));
        dailyBytesPerTenant = dailyBytesPerTenant.add(orders.multiply(itemsPerOrder).multiply(BigDecimal.valueOf(1536)));
        dailyBytesPerTenant = dailyBytesPerTenant.add(orders.multiply(BigDecimal.valueOf(2L * 2048L)));
        dailyBytesPerTenant = dailyBytesPerTenant.add(clothIn.multiply(BigDecimal.valueOf(2048)));
        dailyBytesPerTenant = dailyBytesPerTenant.add(clothIn.add(clothOut).multiply(BigDecimal.valueOf(1536)));
        dailyBytesPerTenant = dailyBytesPerTenant.add(BigDecimal.valueOf(Math.max(properties.getCapacityEstimateActiveEmployees(), 0)).multiply(BigDecimal.valueOf(1536)));
        dailyBytesPerTenant = dailyBytesPerTenant.add(BigDecimal.valueOf(Math.max(properties.getCapacityEstimateDailyOperationLogs(), 0)).multiply(BigDecimal.valueOf(3072)));
        dailyBytesPerTenant = dailyBytesPerTenant.add(BigDecimal.valueOf(Math.max(properties.getCapacityEstimateDailyNotifications(), 0)).multiply(BigDecimal.valueOf(2048)));
        dailyBytesPerTenant = dailyBytesPerTenant.add(BigDecimal.valueOf(Math.max(properties.getCapacityEstimateDailyPrintTasks(), 0)).multiply(BigDecimal.valueOf(2048)));

        return dailyBytesPerTenant
                .multiply(BigDecimal.valueOf(Math.max(properties.getCapacityEstimateTenants(), 1)))
                .multiply(BigDecimal.valueOf(30))
                .multiply(safePositiveFactor(properties.getCapacityEstimateSafetyFactor(), 1.45))
                .divide(MB_BYTES, 2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> estimateActualMonthlyDatabaseGrowthMb() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<Map<String, Object>> sources = new ArrayList<>();
        BigDecimal totalBytes = BigDecimal.ZERO;

        totalBytes = totalBytes.add(addGrowthSource(sources, "sales_order", "create_time", 4096, cutoff));
        totalBytes = totalBytes.add(addGrowthSource(sources, "sales_order_detail", "create_time", 1536, cutoff));
        totalBytes = totalBytes.add(addGrowthSource(sources, "sales_order_status_log", "create_time", 2048, cutoff));
        totalBytes = totalBytes.add(addGrowthSource(sources, "production_order", "create_time", 4096, cutoff));
        totalBytes = totalBytes.add(addGrowthSource(sources, "production_order_status_log", "create_time", 2048, cutoff));
        totalBytes = totalBytes.add(addGrowthSource(sources, "cloth", "create_time", 2048, cutoff));
        totalBytes = totalBytes.add(addGrowthSource(sources, "inventory_record", "create_time", 1536, cutoff));
        totalBytes = totalBytes.add(addGrowthSource(sources, "attendance_record", "create_time", 1536, cutoff));
        totalBytes = totalBytes.add(addGrowthSource(sources, "operation_log", "create_time", 3072, cutoff));
        totalBytes = totalBytes.add(addGrowthSource(sources, "notification_record", "create_time", 2048, cutoff));
        totalBytes = totalBytes.add(addGrowthSource(sources, "print_task", "create_time", 2048, cutoff));
        totalBytes = totalBytes.add(addGrowthSource(sources, "system_event", "create_time", 1536, cutoff));

        BigDecimal monthlyGrowthMb = totalBytes
                .multiply(safePositiveFactor(properties.getCapacityEstimateSafetyFactor(), 1.45))
                .divide(MB_BYTES, 2, RoundingMode.HALF_UP);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("monthlyDatabaseGrowthMb", monthlyGrowthMb);
        result.put("sources", sources);
        return result;
    }

    private BigDecimal addGrowthSource(List<Map<String, Object>> sources,
                                       String table,
                                       String dateColumn,
                                       int estimatedBytesPerRow,
                                       LocalDateTime cutoff) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("tableName", table);
        source.put("estimatedBytesPerRow", estimatedBytesPerRow);
        if (!tableExists(table) || !columnExists(table, dateColumn)) {
            source.put("skipped", true);
            source.put("reason", "table or date column missing");
            sources.add(source);
            return BigDecimal.ZERO;
        }
        Long rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `" + table + "` WHERE `" + dateColumn + "` >= ?",
                Long.class,
                cutoff
        );
        long safeRows = rows == null ? 0L : Math.max(rows, 0L);
        BigDecimal bytes = BigDecimal.valueOf(safeRows).multiply(BigDecimal.valueOf(estimatedBytesPerRow));
        source.put("last30DaysRows", safeRows);
        source.put("estimatedMb", bytes.divide(MB_BYTES, 2, RoundingMode.HALF_UP));
        sources.add(source);
        return bytes;
    }

    private BigDecimal estimateMonthlyLocalFileGrowthMb() {
        return BigDecimal.valueOf(Math.max(properties.getCapacityEstimateTenants(), 1))
                .multiply(BigDecimal.valueOf(Math.max(properties.getCapacityEstimateDailyAttachments(), 0)))
                .multiply(safeFactor(properties.getCapacityEstimateAvgAttachmentMb(), 1.5))
                .multiply(BigDecimal.valueOf(30))
                .multiply(safeFactor(properties.getCapacityEstimateLocalAttachmentRatio(), 1.0))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> buildCapacityBusinessAssumptions() {
        Map<String, Object> assumptions = new LinkedHashMap<>();
        assumptions.put("tenants", properties.getCapacityEstimateTenants());
        assumptions.put("dailyOrders", properties.getCapacityEstimateDailyOrders());
        assumptions.put("orderItemsPerOrder", properties.getCapacityEstimateOrderItemsPerOrder());
        assumptions.put("dailyClothIn", properties.getCapacityEstimateDailyClothIn());
        assumptions.put("dailyClothOut", properties.getCapacityEstimateDailyClothOut());
        assumptions.put("activeEmployees", properties.getCapacityEstimateActiveEmployees());
        assumptions.put("dailyOperationLogs", properties.getCapacityEstimateDailyOperationLogs());
        assumptions.put("dailyNotifications", properties.getCapacityEstimateDailyNotifications());
        assumptions.put("dailyPrintTasks", properties.getCapacityEstimateDailyPrintTasks());
        assumptions.put("dailyAttachments", properties.getCapacityEstimateDailyAttachments());
        assumptions.put("avgAttachmentMb", properties.getCapacityEstimateAvgAttachmentMb());
        assumptions.put("localAttachmentRatio", properties.getCapacityEstimateLocalAttachmentRatio());
        assumptions.put("safetyFactor", properties.getCapacityEstimateSafetyFactor());
        assumptions.put("backupKeep", properties.getCapacityEstimateBackupKeep());
        return assumptions;
    }

    private String capacityStatus(BigDecimal usedPercent) {
        int warn = safePercent(properties.getCapacityProjectionWarnPercent(), 70);
        int fail = safePercent(properties.getCapacityProjectionFailPercent(), 85);
        if (usedPercent.compareTo(BigDecimal.valueOf(fail)) >= 0) {
            return "FAIL";
        }
        if (usedPercent.compareTo(BigDecimal.valueOf(warn)) >= 0) {
            return "WARN";
        }
        return "OK";
    }

    private int safePercent(int value, int fallback) {
        if (value <= 0 || value > 100) {
            return fallback;
        }
        return value;
    }

    private BigDecimal safeFactor(double value, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            return BigDecimal.valueOf(fallback);
        }
        return BigDecimal.valueOf(value);
    }

    private BigDecimal safePositiveFactor(double value, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
            return BigDecimal.valueOf(fallback);
        }
        return BigDecimal.valueOf(value);
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal round2(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal bytesToMb(long bytes) {
        if (bytes <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(bytes).divide(MB_BYTES, 2, RoundingMode.HALF_UP);
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
