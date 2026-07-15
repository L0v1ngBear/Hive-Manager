package my.hive.infrastructure.scheduler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.hive.domain.attendance.model.enums.AttendancePunchStatusEnum;
import my.hive.domain.approval.model.enums.ApprovalStatusEnum;
import my.hive.shared.event.SystemEventPublisher;
import my.hive.shared.redis.HiveRedisKeyBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsJobHandler {

    private static final DefaultRedisScript<Long> RELEASE_LOCK = releaseLockScript();

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final HiveRedisKeyBuilder redisKeyBuilder;
    private final SystemEventPublisher systemEventPublisher;

    @XxlJob("attendanceDailyStatJob")
    public void attendanceDailyStatJob() {
        runLocked("attendance-daily", 30, () -> recomputeAttendance(LocalDate.now().minusDays(1)));
    }

    @XxlJob("inventoryDailyStatJob")
    public void inventoryDailyStatJob() {
        runLocked("inventory-daily", 20, () -> recomputeInventory(LocalDate.now().minusDays(1)));
    }

    void recomputeAttendance(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        Map<String, AttendanceRule> rules = loadAttendanceRules();
        List<Map<String, Object>> users = jdbcTemplate.queryForList("""
                SELECT id, tenant_code
                FROM user
                WHERE IFNULL(status, 1) <> 0
                  AND COALESCE(attendance_required, 1) = 1
                """);
        int processed = 0;
        for (Map<String, Object> user : users) {
            long userId = ((Number) user.get("id")).longValue();
            String tenantCode = String.valueOf(user.get("tenant_code"));
            AttendanceRule rule = rules.get(tenantCode);
            if (rule == null || !rule.worksOn(date)) {
                continue;
            }
            String punchId = date.toString().replace("-", "") + "_" + userId;
            List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                    "SELECT id, sign_in_time, sign_in_status, sign_out_time, sign_out_status FROM attendance_record WHERE tenant_code = ? AND punch_id = ? LIMIT 1",
                    tenantCode, punchId);
            boolean leaveAtStart = hasApprovedLeave(tenantCode, userId, LocalDateTime.of(date, rule.workStart()), dayStart, dayEnd);
            boolean leaveAtEnd = hasApprovedLeave(tenantCode, userId, LocalDateTime.of(date, rule.offWorkEnd()), dayStart, dayEnd);
            int signInStatus = leaveAtStart ? AttendancePunchStatusEnum.LEAVE.getCode() : AttendancePunchStatusEnum.ABSENT.getCode();
            int signOutStatus = leaveAtEnd ? AttendancePunchStatusEnum.LEAVE.getCode() : AttendancePunchStatusEnum.ABSENT.getCode();
            if (existing.isEmpty()) {
                jdbcTemplate.update("""
                        INSERT INTO attendance_record
                            (punch_id, user_id, tenant_code, sign_in_status, sign_out_status, create_time, update_time)
                        VALUES (?, ?, ?, ?, ?, NOW(), NOW())
                        """, punchId, userId, tenantCode, signInStatus, signOutStatus);
            } else {
                Map<String, Object> record = existing.get(0);
                jdbcTemplate.update("""
                        UPDATE attendance_record
                        SET sign_in_status = CASE WHEN sign_in_time IS NULL THEN ? ELSE sign_in_status END,
                            sign_out_status = CASE WHEN sign_out_time IS NULL THEN ? ELSE sign_out_status END,
                            update_time = NOW()
                        WHERE id = ? AND tenant_code = ?
                        """, signInStatus, signOutStatus, record.get("id"), tenantCode);
            }
            processed++;
        }
        XxlJobHelper.log("attendance daily statistics completed, date={}, processed={}", date, processed);
        systemEventPublisher.info("ATTENDANCE_DAILY_STAT", "考勤日统计完成",
                "统计日期 " + date + "，处理员工 " + processed + " 人", Map.of("date", date, "processed", processed));
    }

    void recomputeInventory(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<String> tenantCodes = jdbcTemplate.queryForList(
                "SELECT tenant_code FROM tenant WHERE deleted = 0 AND status = 1", String.class);
        for (String tenantCode : tenantCodes) {
            BigDecimal dayIn = amount("""
                    SELECT COALESCE(SUM(operate_meters), 0) FROM inventory_record
                    WHERE tenant_code = ? AND operate_type IN (0, 2) AND create_time >= ? AND create_time < ?
                    """, tenantCode, start, end);
            BigDecimal dayOut = amount("""
                    SELECT COALESCE(SUM(operate_meters), 0) FROM inventory_record
                    WHERE tenant_code = ? AND operate_type = 1 AND create_time >= ? AND create_time < ?
                    """, tenantCode, start, end);
            Long rollCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM cloth WHERE tenant_code = ? AND COALESCE(del_flag, 0) = 0", Long.class, tenantCode);
            BigDecimal totalMeters = amount(
                    "SELECT COALESCE(SUM(remaining_meters), 0) FROM cloth WHERE tenant_code = ? AND COALESCE(del_flag, 0) = 0",
                    tenantCode);
            jdbcTemplate.update("""
                    INSERT INTO inventory_statics
                        (stat_date, total_roll_count, total_meters, day_in_meters, day_out_meters, tenant_code, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
                    ON DUPLICATE KEY UPDATE
                        total_roll_count = VALUES(total_roll_count),
                        total_meters = VALUES(total_meters),
                        day_in_meters = VALUES(day_in_meters),
                        day_out_meters = VALUES(day_out_meters),
                        update_time = NOW()
                    """, start, rollCount == null ? 0L : rollCount, totalMeters, dayIn, dayOut, tenantCode);
        }
        XxlJobHelper.log("inventory daily statistics completed, date={}, tenants={}", date, tenantCodes.size());
        systemEventPublisher.info("INVENTORY_DAILY_STAT", "库存日统计完成",
                "统计日期 " + date + "，处理租户 " + tenantCodes.size() + " 个", Map.of("date", date, "tenantCount", tenantCodes.size()));
    }

    private Map<String, AttendanceRule> loadAttendanceRules() {
        Map<String, AttendanceRule> result = new HashMap<>();
        jdbcTemplate.query("""
                SELECT tenant_code, work_start_time, off_work_end_time, work_days
                FROM tenant_attendance_rule
                WHERE status = 1
                """, rs -> {
            Time workStart = rs.getTime("work_start_time");
            Time offWorkEnd = rs.getTime("off_work_end_time");
            if (workStart != null && offWorkEnd != null) {
                result.put(rs.getString("tenant_code"),
                        new AttendanceRule(workStart.toLocalTime(), offWorkEnd.toLocalTime(), rs.getString("work_days")));
            }
        });
        return result;
    }

    private boolean hasApprovedLeave(String tenantCode,
                                     long userId,
                                     LocalDateTime point,
                                     LocalDateTime dayStart,
                                     LocalDateTime dayEnd) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM user_leave
                WHERE tenant_code = ? AND apply_user_id = ? AND status = ?
                  AND start_time < ? AND end_time > ?
                  AND start_time <= ? AND end_time >= ?
                """, Integer.class, tenantCode, userId, ApprovalStatusEnum.APPROVED.getCode(), dayEnd, dayStart, point, point);
        return count != null && count > 0;
    }

    private BigDecimal amount(String sql, Object... args) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value;
    }

    private void runLocked(String jobName, int leaseMinutes, Runnable task) {
        String key = redisKeyBuilder.lock("scheduler", jobName);
        String value = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, value, leaseMinutes, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(locked)) {
            XxlJobHelper.log("{} skipped because another node owns the lock", jobName);
            return;
        }
        try {
            task.run();
        } catch (Exception exception) {
            log.error("scheduled statistics failed, job={}", jobName, exception);
            systemEventPublisher.error("SCHEDULED_STAT_FAILED", "定时统计失败", exception, Map.of("job", jobName));
            XxlJobHelper.handleFail(exception.getMessage());
            throw exception;
        } finally {
            redisTemplate.execute(RELEASE_LOCK, List.of(key), value);
        }
    }

    private static DefaultRedisScript<Long> releaseLockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
        return script;
    }

    private record AttendanceRule(LocalTime workStart, LocalTime offWorkEnd, String workDays) {
        boolean worksOn(LocalDate date) {
            if (workDays == null || workDays.isBlank()) {
                return true;
            }
            String day = String.valueOf(date.getDayOfWeek().getValue());
            return List.of(workDays.split(",")).stream().map(String::trim).anyMatch(day::equals);
        }
    }
}
