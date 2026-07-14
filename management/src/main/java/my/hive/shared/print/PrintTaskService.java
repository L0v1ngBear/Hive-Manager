package my.hive.shared.print;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.print.dto.PrintTaskReportRequest;
import my.hive.shared.print.vo.PrintTaskVO;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 打印任务服务。
 * 负责记录标签、出库单等打印链路，便于后续补打、失败排查和审计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrintTaskService {

    private static final DateTimeFormatter TASK_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String PRINT_TYPE_LABEL = "label";
    private static final String PRINT_TYPE_ORDER_FLOW = "order_flow";
    private static final String PRINT_TYPE_EQUIPMENT_INSPECTION = "equipment_inspection";
    private static final String PRINT_TYPE_RECEIPT = "receipt";
    private static final List<String> DEFAULT_PRINT_TYPES = List.of(
            PRINT_TYPE_LABEL,
            PRINT_TYPE_ORDER_FLOW,
            PRINT_TYPE_EQUIPMENT_INSPECTION,
            PRINT_TYPE_RECEIPT
    );

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public String createLabelTask(String bizNo, Object payload, Long templateId, String templateName, String reason) {
        return createTask("label", "cloth", bizNo, null, payload, templateId, templateName, reason);
    }

    public String createReceiptTask(String orderNo, Object payload, Long templateId, String templateName, String reason) {
        return createTask("receipt", "outbound_order", orderNo, orderNo, payload, templateId, templateName, reason);
    }

    public String createTask(String printType,
                             String bizType,
                             String bizNo,
                             String sourceOrderNo,
                             Object payload,
                             Long templateId,
                             String templateName,
                             String reason) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        String taskNo = generateTaskNo(printType);
        try {
            jdbcTemplate.update("""
                            INSERT INTO print_task (
                              tenant_code, task_no, print_type, biz_type, biz_no, source_order_no,
                              template_id, template_name, status, retry_count, max_retry,
                              print_payload_json, error_message, operator_id, create_time, update_time
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    tenantCode,
                    taskNo,
                    printType,
                    bizType,
                    bizNo,
                    sourceOrderNo,
                    templateId,
                    templateName,
                    PrintTaskStatus.PENDING,
                    0,
                    3,
                    toJson(payload),
                    reason,
                    userId,
                    LocalDateTime.now(),
                    LocalDateTime.now());
            return taskNo;
        } catch (Exception ex) {
            log.warn("创建打印任务失败，printType={}, bizNo={}", printType, bizNo, ex);
            return null;
        }
    }

    public String createTaskIfAbsent(String printType,
                                     String bizType,
                                     String bizNo,
                                     String sourceOrderNo,
                                     Object payload,
                                     Long templateId,
                                     String templateName,
                                     String reason) {
        ReusableTask existingTask = findReusableTask(printType, bizType, bizNo);
        if (existingTask != null) {
            if (!refreshReusableTask(existingTask, sourceOrderNo, payload, templateId, templateName, reason)) {
                return null;
            }
            return existingTask.taskNo();
        }
        return createTask(printType, bizType, bizNo, sourceOrderNo, payload, templateId, templateName, reason);
    }

    private ReusableTask findReusableTask(String printType, String bizType, String bizNo) {
        String safePrintType = blankToNull(printType);
        String safeBizType = blankToNull(bizType);
        String safeBizNo = blankToNull(bizNo);
        if (safePrintType == null || safeBizType == null || safeBizNo == null) {
            return null;
        }
        try {
            return jdbcTemplate.query("""
                            SELECT task_no, status
                            FROM print_task
                            WHERE tenant_code = ?
                              AND print_type = ?
                              AND biz_type = ?
                              AND biz_no = ?
                              AND (
                                status = ?
                                OR (status = ? AND retry_count < max_retry)
                                OR status = ?
                              )
                            ORDER BY CASE
                                WHEN status = ? THEN 0
                                WHEN status = ? THEN 1
                                ELSE 2
                              END,
                              id DESC
                            LIMIT 1
                            """,
                    rs -> rs.next() ? new ReusableTask(rs.getString("task_no"), rs.getInt("status")) : null,
                    TenantPermissionContext.getTenantCode(),
                    safePrintType,
                    safeBizType,
                    safeBizNo,
                    PrintTaskStatus.PENDING,
                    PrintTaskStatus.FAILED,
                    PrintTaskStatus.SUCCESS,
                    PrintTaskStatus.PENDING,
                    PrintTaskStatus.FAILED);
        } catch (Exception ex) {
            log.warn("查询可复用打印任务失败，printType={}, bizNo={}", safePrintType, safeBizNo, ex);
            return null;
        }
    }

    private boolean refreshReusableTask(ReusableTask task,
                                        String sourceOrderNo,
                                        Object payload,
                                        Long templateId,
                                        String templateName,
                                        String reason) {
        if (task.status() == PrintTaskStatus.SUCCESS) {
            return true;
        }
        try {
            int rows = jdbcTemplate.update("""
                            UPDATE print_task
                            SET source_order_no = ?,
                                template_id = ?,
                                template_name = ?,
                                status = ?,
                                retry_count = 0,
                                print_payload_json = ?,
                                error_message = ?,
                                update_time = ?
                            WHERE tenant_code = ?
                              AND task_no = ?
                              AND status <> ?
                            """,
                    sourceOrderNo,
                    templateId,
                    templateName,
                    PrintTaskStatus.PENDING,
                    toJson(payload),
                    reason,
                    LocalDateTime.now(),
                    TenantPermissionContext.getTenantCode(),
                    task.taskNo(),
                    PrintTaskStatus.SUCCESS);
            return rows > 0;
        } catch (Exception ex) {
            log.warn("刷新可复用打印任务失败，taskNo={}", task.taskNo(), ex);
            return false;
        }
    }

    private record ReusableTask(String taskNo, int status) {
    }

    public void report(PrintTaskReportRequest request) {
        if (request == null || request.getTaskNo() == null || request.getTaskNo().isBlank()) {
            throw new BusinessException("打印任务号不能为空");
        }
        String taskNo = request.getTaskNo().trim();
        PrintTaskAccessInfo taskInfo = requireTaskForAccess(taskNo);
        assertCanAccessPrintType(taskInfo.printType());
        Integer status = normalizeStatus(request.getStatus());
        String tenantCode = TenantPermissionContext.getTenantCode();
        LocalDateTime now = LocalDateTime.now();
        int rows = jdbcTemplate.update("""
                        UPDATE print_task
                        SET status = ?,
                            print_channel = ?,
                            device_name = ?,
                            error_message = ?,
                            retry_count = CASE WHEN ? = ? THEN retry_count + 1 ELSE retry_count END,
                            printed_time = CASE WHEN ? = ? THEN ? ELSE printed_time END,
                            update_time = ?
                        WHERE tenant_code = ?
                          AND task_no = ?
                        """,
                status,
                blankToNull(request.getPrintChannel()),
                blankToNull(request.getDeviceName()),
                blankToNull(request.getErrorMessage()),
                status,
                PrintTaskStatus.FAILED,
                status,
                PrintTaskStatus.SUCCESS,
                now,
                now,
                tenantCode,
                taskNo);
        if (rows == 0) {
            throw new BusinessException("打印任务不存在或无权更新");
        }
    }

    public List<PrintTaskVO> recent(String printType, int limit) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        int safeLimit = Math.max(1, Math.min(limit, 50));
        String safePrintType = blankToNull(printType);
        if (safePrintType == null) {
            return DEFAULT_PRINT_TYPES.stream()
                    .filter(this::canAccessPrintType)
                    .flatMap(type -> recent(type, safeLimit).stream())
                    .sorted(Comparator.comparing(PrintTaskVO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(safeLimit)
                    .toList();
        }
        assertCanAccessPrintType(safePrintType);
        return jdbcTemplate.query("""
                        SELECT id, task_no AS taskNo, print_type AS printType, biz_type AS bizType,
                               biz_no AS bizNo, status, retry_count AS retryCount, print_channel AS printChannel,
                               device_name AS deviceName, error_message AS errorMessage,
                               create_time AS createTime, printed_time AS printedTime
                        FROM print_task
                        WHERE tenant_code = ?
                          AND print_type = ?
                        ORDER BY id DESC
                        LIMIT ?
                        """,
                BeanPropertyRowMapper.newInstance(PrintTaskVO.class),
                tenantCode,
                safePrintType,
                safeLimit);
    }

    public List<PrintTaskVO> pending(String printType, int limit) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        int safeLimit = Math.max(1, Math.min(limit, 50));
        String safePrintType = printType == null || printType.isBlank() ? null : printType.trim();
        if (safePrintType == null) {
            return DEFAULT_PRINT_TYPES.stream()
                    .filter(this::canAccessPrintType)
                    .flatMap(type -> pending(type, safeLimit).stream())
                    .sorted(Comparator.comparing(PrintTaskVO::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                    .limit(safeLimit)
                    .toList();
        }
        assertCanAccessPrintType(safePrintType);
        return jdbcTemplate.query("""
                        SELECT id, task_no, print_type, biz_type, biz_no, status, retry_count,
                               print_channel, device_name, error_message, print_payload_json,
                               create_time, printed_time
                        FROM print_task
                        WHERE tenant_code = ?
                          AND print_type = ?
                          AND (status = ? OR (status = ? AND retry_count < max_retry))
                        ORDER BY status ASC, create_time ASC, id ASC
                        LIMIT ?
                        """,
                (rs, rowNum) -> mapPrintTaskWithPayload(rs),
                tenantCode,
                safePrintType,
                PrintTaskStatus.PENDING,
                PrintTaskStatus.FAILED,
                safeLimit);
    }

    public Map<String, Long> pendingCount(List<String> printTypes) {
        List<String> safePrintTypes = printTypes == null ? List.of() : printTypes.stream()
                .map(this::blankToNull)
                .filter(type -> type != null)
                .distinct()
                .toList();
        if (safePrintTypes.isEmpty()) {
            safePrintTypes = DEFAULT_PRINT_TYPES;
        }
        Map<String, Long> result = new LinkedHashMap<>();
        for (String printType : safePrintTypes) {
            result.put(printType, canAccessPrintType(printType) ? pendingCount(printType) : 0L);
        }
        return result;
    }

    private long pendingCount(String printType) {
        Long count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM print_task
                        WHERE tenant_code = ?
                          AND print_type = ?
                          AND (status = ? OR (status = ? AND retry_count < max_retry))
                        """,
                Long.class,
                TenantPermissionContext.getTenantCode(),
                printType,
                PrintTaskStatus.PENDING,
                PrintTaskStatus.FAILED);
        return count == null ? 0L : count;
    }

    private PrintTaskVO mapPrintTaskWithPayload(java.sql.ResultSet rs) throws java.sql.SQLException {
        PrintTaskVO vo = new PrintTaskVO();
        vo.setId(rs.getLong("id"));
        vo.setTaskNo(rs.getString("task_no"));
        vo.setPrintType(rs.getString("print_type"));
        vo.setBizType(rs.getString("biz_type"));
        vo.setBizNo(rs.getString("biz_no"));
        vo.setStatus(rs.getInt("status"));
        vo.setRetryCount(rs.getInt("retry_count"));
        vo.setPrintChannel(rs.getString("print_channel"));
        vo.setDeviceName(rs.getString("device_name"));
        vo.setErrorMessage(rs.getString("error_message"));
        vo.setCreateTime(rs.getTimestamp("create_time") == null ? null : rs.getTimestamp("create_time").toLocalDateTime());
        vo.setPrintedTime(rs.getTimestamp("printed_time") == null ? null : rs.getTimestamp("printed_time").toLocalDateTime());
        vo.setPrintPayload(parsePayload(rs.getString("print_payload_json")));
        return vo;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payloadJson, Map.class);
        } catch (Exception ex) {
            log.warn("解析打印任务快照失败", ex);
            return Map.of();
        }
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return PrintTaskStatus.SUCCESS;
        }
        if (status == PrintTaskStatus.SUCCESS || status == PrintTaskStatus.FAILED || status == PrintTaskStatus.CANCELED) {
            return status;
        }
        throw new BusinessException("不支持的打印任务状态");
    }

    private String generateTaskNo(String printType) {
        String prefix = "receipt".equalsIgnoreCase(printType) ? "PT-R" : "PT-L";
        return prefix + TASK_NO_FORMATTER.format(LocalDateTime.now()) + ThreadLocalRandom.current().nextInt(100, 999);
    }

    private String toJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return String.valueOf(payload);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PrintTaskAccessInfo requireTaskForAccess(String taskNo) {
        PrintTaskAccessInfo info = jdbcTemplate.query("""
                            SELECT print_type
                            FROM print_task
                            WHERE tenant_code = ?
                              AND task_no = ?
                            LIMIT 1
                            """,
                rs -> rs.next() ? new PrintTaskAccessInfo(rs.getString("print_type")) : null,
                TenantPermissionContext.getTenantCode(),
                taskNo);
        if (info == null) {
            throw new BusinessException("打印任务不存在或无权更新");
        }
        return info;
    }

    private void assertCanAccessPrintType(String printType) {
        if (!canAccessPrintType(printType)) {
            throw new BusinessException(403, "当前账号没有权限处理该打印任务，请联系管理员分配对应打印权限");
        }
    }

    private boolean canAccessPrintType(String printType) {
        String type = blankToNull(printType);
        if (type == null) {
            return false;
        }
        return switch (type) {
            case PRINT_TYPE_LABEL -> hasAnyPermission(
                    "label:template:list", "label:template:detail", "inventory:cloth:in", "inventory:cloth:out");
            case PRINT_TYPE_ORDER_FLOW -> hasAnyPermission(
                    "order:*", "order:list", "order:detail", "order:status:*",
                    "sales:order:list", "sales:order:detail", "sales:order:status",
                    "sales:order:pre-confirm", "sales:order:fulfillment",
                    "production:order:list", "production:order:detail", "production:order:log", "production:order:status",
                    "production:order:pre-production", "production:order:fulfillment");
            case PRINT_TYPE_EQUIPMENT_INSPECTION -> hasAnyPermission(
                    "equipment:list", "equipment:detail", "equipment:inspection:list", "equipment:inspection:submit");
            case PRINT_TYPE_RECEIPT -> hasAnyPermission(
                    "receipt:print:list", "receipt:print:detail", "receipt:print:mark", "inventory:cloth:out");
            default -> false;
        };
    }

    private boolean hasAnyPermission(String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return false;
        }
        for (String permissionCode : permissionCodes) {
            if (TenantPermissionContext.hasPermission(permissionCode)) {
                return true;
            }
        }
        return false;
    }

    private record PrintTaskAccessInfo(String printType) {
    }
}
