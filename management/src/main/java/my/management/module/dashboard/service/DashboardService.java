package my.management.module.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.redis.HiveRedisKeyBuilder;
import my.management.module.ai.model.vo.AiAdviceDailyBriefVO;
import my.management.module.ai.model.vo.AiBusinessSnapshotVO;
import my.management.module.ai.model.vo.DashboardAiAdviceVO;
import my.management.module.ai.service.AiAdvicePermissionService;
import my.management.module.ai.service.AiAdviceSnapshotService;
import my.management.module.ai.service.AiAnalysisService;
import my.management.module.attendance.model.enums.AttendancePunchStatusEnum;
import my.management.module.dashboard.mapper.DashboardMapper;
import my.management.module.dashboard.model.vo.DashboardAttendanceAlertRowVO;
import my.management.module.dashboard.model.vo.DashboardInventoryTrendRowVO;
import my.management.module.dashboard.model.vo.DashboardOverviewVO;
import my.management.module.dashboard.model.vo.DashboardPendingPrintRowVO;
import my.management.module.inventory.model.enums.InventoryOperateTypeEnum;
import my.management.module.inventory.model.vo.InventoryWarningVO;
import my.management.module.inventory.service.InventoryWarningCacheService;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import my.management.module.tenant.service.TenantLicenseService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 总览大盘服务，负责聚合首页需要的摘要数据，并优先从 Redis 读取缓存结果。
 */
@Service
@Slf4j
public class DashboardService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final DateTimeFormatter DAY_PREFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Duration OVERVIEW_CACHE_TTL = Duration.ofSeconds(90);
    private static final Duration AI_ADVICE_CACHE_TTL = Duration.ofSeconds(60);
    private static final long SLOW_OVERVIEW_MILLIS = 500L;
    private static final long SLOW_AI_ADVICE_MILLIS = 1000L;

    @Resource
    private DashboardMapper dashboardMapper;

    @Resource
    private AiAnalysisService aiAnalysisService;

    @Resource
    private AiAdvicePermissionService aiAdvicePermissionService;

    @Resource
    private AiAdviceSnapshotService aiAdviceSnapshotService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

    @Resource
    private TenantLicenseService tenantLicenseService;

    @Resource
    private InventoryWarningCacheService inventoryWarningCacheService;

    public DashboardOverviewVO overview() {
        long startNanos = System.nanoTime();
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        String cacheKey = buildScopedCacheKey("overview", tenantCode, userId);

        try {
            DashboardOverviewVO cached = getCachedOverview(cacheKey);
            if (cached != null) {
                return cached;
            }

            DashboardOverviewVO vo = new DashboardOverviewVO();
            DashboardOverviewVO.Visibility visibility = buildVisibility();
            vo.setVisibility(visibility);
            vo.setSummary(buildSummary(tenantCode, userId, visibility));
            buildTrend(tenantCode, visibility, vo);
            vo.setBusinessAlerts(buildBusinessAlerts(tenantCode, visibility));
            vo.setAttendanceSummary(buildAttendanceSummary(tenantCode, visibility));
            vo.setAttendanceAlerts(buildAttendanceAlerts(tenantCode, visibility));
            vo.setQuickActions(buildQuickActions(visibility));

            cacheOverview(cacheKey, vo);
            return vo;
        } finally {
            logSlowOperation("dashboard overview", startNanos, SLOW_OVERVIEW_MILLIS);
        }
    }

    public List<DashboardAiAdviceVO> aiAdvices(boolean refresh) {
        return aiAdvices(refresh, null);
    }

    public List<DashboardAiAdviceVO> aiAdvices(boolean refresh, Integer limit) {
        long startNanos = System.nanoTime();
        aiAdvicePermissionService.requireAnyView();
        String tenantCode = TenantPermissionContext.getTenantCode();
        tenantLicenseService.requireFeatureEnabled(tenantCode, TenantFeatureEnum.CODE_AI_ADVICE, "当前套餐暂未开放经营建议，请联系管理员开通");
        Long userId = TenantPermissionContext.getUserId();
        String cacheKey = buildScopedCacheKey("ai-advice", tenantCode, userId);

        try {
            if (refresh) {
                stringRedisTemplate.delete(cacheKey);
            } else {
                List<DashboardAiAdviceVO> cached = getCachedAiAdvices(cacheKey);
                if (cached != null) {
                    return limitAdvices(cached, limit);
                }
            }

            List<DashboardAiAdviceVO> advices = aiAdvicePermissionService.filterVisible(
                    aiAdviceSnapshotService.readSnapshotAdvices(tenantCode)
            );
            cacheAiAdvices(cacheKey, advices);
            return limitAdvices(advices, limit);
        } finally {
            logSlowOperation("dashboard ai advice", startNanos, SLOW_AI_ADVICE_MILLIS);
        }
    }

    public AiAdviceDailyBriefVO aiBrief(boolean refresh) {
        return aiAnalysisService.buildDailyBrief(aiAdvices(refresh));
    }

    public AiBusinessSnapshotVO aiSnapshot() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        tenantLicenseService.requireFeatureEnabled(tenantCode, TenantFeatureEnum.CODE_AI_ADVICE, "当前套餐暂未开放经营快照，请联系管理员开通");
        return aiAnalysisService.buildBusinessSnapshot(tenantCode);
    }

    private DashboardOverviewVO.Visibility buildVisibility() {
        DashboardOverviewVO.Visibility visibility = new DashboardOverviewVO.Visibility();
        visibility.setOrderVisible(hasAnyPermission("sales:order:list", "sales:order:*", "production:order:list", "production:order:*"));
        visibility.setInventoryVisible(hasAnyPermission("inventory", "inventory:*", "inventory:warning:list", "inventory:trend"));
        visibility.setApprovalVisible(hasAnyPermission("approval:*", "approval:leave:audit", "approval:finance:audit", "approval:leave", "approval:finance"));
        visibility.setReceiptVisible(hasAnyPermission("receipt:print:list", "receipt:print:detail", "receipt:print:mark", "receipt:print:cancel"));
        visibility.setTrendVisible(Boolean.TRUE.equals(visibility.getInventoryVisible()));
        visibility.setAttendanceVisible(hasAnyPermission("employee:list", "attendance:*", "attendance:record:list"));
        visibility.setAiAdviceVisible(false);
        return visibility;
    }

    private DashboardOverviewVO.Summary buildSummary(String tenantCode, Long userId, DashboardOverviewVO.Visibility visibility) {
        DashboardOverviewVO.Summary summary = new DashboardOverviewVO.Summary();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        if (Boolean.TRUE.equals(visibility.getOrderVisible())) {
            long salesCount = nvl(dashboardMapper.countMonthSalesOrders(tenantCode, startOfMonth));
            long productionCount = nvl(dashboardMapper.countMonthProductionOrders(tenantCode, startOfMonth));
            summary.setMonthOrderCount(salesCount + productionCount);
        }

        if (Boolean.TRUE.equals(visibility.getInventoryVisible())) {
            summary.setTotalInventoryMeters(scale(dashboardMapper.sumInventoryMeters(tenantCode)));
            summary.setInventoryWarningCount(inventoryWarningCacheService.countWarningModels(tenantCode));
        }

        if (Boolean.TRUE.equals(visibility.getApprovalVisible())) {
            long leaveCount = nvl(dashboardMapper.countPendingLeaveApprovals(tenantCode, userId));
            long financeCount = nvl(dashboardMapper.countPendingFinanceApprovals(tenantCode, userId));
            summary.setPendingApprovalCount(leaveCount + financeCount);
        }

        if (Boolean.TRUE.equals(visibility.getReceiptVisible())) {
            summary.setPendingPrintCount(nvl(dashboardMapper.countPendingPrintOrders(tenantCode)));
        }

        return summary;
    }

    private void buildTrend(String tenantCode, DashboardOverviewVO.Visibility visibility, DashboardOverviewVO vo) {
        if (!Boolean.TRUE.equals(visibility.getTrendVisible())) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        List<DashboardInventoryTrendRowVO> rows = dashboardMapper.selectInventoryRecordTrend(
                tenantCode,
                startDate.atStartOfDay(),
                today.plusDays(1).atStartOfDay().minusSeconds(1),
                InventoryOperateTypeEnum.IN.getCode(),
                InventoryOperateTypeEnum.EXTERNAL_IMPORT.getCode(),
                InventoryOperateTypeEnum.OUT.getCode()
        );
        Map<LocalDate, DashboardInventoryTrendRowVO> trendMap = rows.stream()
                .collect(Collectors.toMap(item -> item.getStatDate().toLocalDate(), item -> item, (a, b) -> a));

        List<String> dates = new ArrayList<>();
        List<BigDecimal> inMeters = new ArrayList<>();
        List<BigDecimal> outMeters = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            DashboardInventoryTrendRowVO row = trendMap.get(date);
            dates.add(date.format(DATE_FORMATTER));
            inMeters.add(scale(row == null ? BigDecimal.ZERO : row.getDayInMeters()));
            outMeters.add(scale(row == null ? BigDecimal.ZERO : row.getDayOutMeters()));
        }
        vo.setTrendDates(dates);
        vo.setTrendInMeters(inMeters);
        vo.setTrendOutMeters(outMeters);
    }

    private List<DashboardOverviewVO.AlertItem> buildBusinessAlerts(String tenantCode, DashboardOverviewVO.Visibility visibility) {
        List<DashboardOverviewVO.AlertItem> alerts = new ArrayList<>();

        if (Boolean.TRUE.equals(visibility.getInventoryVisible())) {
            List<InventoryWarningVO> warnings = inventoryWarningCacheService.topWarnings(tenantCode, 3);
            for (InventoryWarningVO warning : warnings) {
                DashboardOverviewVO.AlertItem item = new DashboardOverviewVO.AlertItem();
                item.setType("inventory");
                item.setLevel("warning");
                item.setTitle("库存预警");
                item.setContent(String.format(
                        "面料型号 %s 当前可用库存仅剩 %s 米，已低于建议安全阈值。",
                        defaultText(warning.getModelCode(), "未命名型号"),
                        scale(warning.getTotalMeters()).stripTrailingZeros().toPlainString()
                ));
                item.setTime(formatTime(warning.getLatestTime()));
                alerts.add(item);
            }
        }

        if (Boolean.TRUE.equals(visibility.getReceiptVisible())) {
            List<DashboardPendingPrintRowVO> orders = dashboardMapper.selectRecentPendingPrintOrders(tenantCode, 3);
            for (DashboardPendingPrintRowVO order : orders) {
                DashboardOverviewVO.AlertItem item = new DashboardOverviewVO.AlertItem();
                item.setType("receipt");
                item.setLevel("info");
                item.setTitle("待打印出库单");
                item.setContent(String.format(
                        "出库单 %s（客户：%s）仍待打印处理。",
                        defaultText(order.getOrderNo(), "--"),
                        defaultText(order.getCustomerName(), "未填写")
                ));
                item.setTime(formatTime(order.getUpdateTime()));
                alerts.add(item);
            }
        }

        alerts.sort(Comparator.comparing(this::parseAlertTime).reversed());
        return alerts.stream().limit(6).toList();
    }

    private List<DashboardOverviewVO.AttendanceAlert> buildAttendanceAlerts(String tenantCode, DashboardOverviewVO.Visibility visibility) {
        if (!Boolean.TRUE.equals(visibility.getAttendanceVisible())) {
            return List.of();
        }

        List<DashboardAttendanceAlertRowVO> rows = dashboardMapper.selectTodayAttendanceAlerts(
                tenantCode,
                LocalDate.now().format(DAY_PREFIX_FORMATTER),
                6
        );
        if (rows.isEmpty()) {
            return List.of();
        }

        List<DashboardOverviewVO.AttendanceAlert> alerts = new ArrayList<>();
        for (DashboardAttendanceAlertRowVO row : rows) {
            DashboardOverviewVO.AttendanceAlert alert = new DashboardOverviewVO.AttendanceAlert();
            alert.setUserId(row.getUserId());
            alert.setUserName(defaultText(row.getUserName(), "未命名员工"));
            alert.setDepartmentName(defaultText(row.getDepartmentName(), "未分配部门"));
            alert.setStatusText(resolveAttendanceStatus(row));
            alert.setTime(formatTime(row.getUpdateTime() == null ? row.getCreateTime() : row.getUpdateTime()));
            alerts.add(alert);
        }
        return alerts;
    }

    private DashboardOverviewVO.AttendanceSummary buildAttendanceSummary(String tenantCode, DashboardOverviewVO.Visibility visibility) {
        DashboardOverviewVO.AttendanceSummary summary = new DashboardOverviewVO.AttendanceSummary();
        if (!Boolean.TRUE.equals(visibility.getAttendanceVisible())) {
            summary.setStatusText("暂无查看权限");
            summary.setStatusType("no_permission");
            return summary;
        }

        String dayPrefix = LocalDate.now().format(DAY_PREFIX_FORMATTER);
        long totalEmployeeCount = nvl(dashboardMapper.countAttendanceEmployees(tenantCode));
        long actualCount = nvl(dashboardMapper.countTodayAttendanceActual(tenantCode, dayPrefix));
        long abnormalCount = nvl(dashboardMapper.countTodayAttendanceAbnormal(tenantCode, dayPrefix));

        summary.setTotalEmployeeCount(totalEmployeeCount);
        summary.setActualCount(actualCount);
        summary.setAbnormalCount(abnormalCount);
        if (totalEmployeeCount <= 0) {
            summary.setStatusText("暂无在职员工");
            summary.setStatusType("empty");
        } else if (actualCount <= 0) {
            summary.setStatusText("今日暂无打卡记录");
            summary.setStatusType("waiting");
        } else if (abnormalCount > 0) {
            summary.setStatusText("发现考勤异常，请及时处理");
            summary.setStatusType("warning");
        } else {
            summary.setStatusText("今日已打卡员工考勤正常");
            summary.setStatusType("normal");
        }
        return summary;
    }

    private List<DashboardOverviewVO.QuickAction> buildQuickActions(DashboardOverviewVO.Visibility visibility) {
        List<DashboardOverviewVO.QuickAction> actions = new ArrayList<>();
        if (hasAnyPermission("notification:announcement:publish", "dashboard:*")) {
            actions.add(buildAction("发布通知", "发布企业通知公告", "/function/announcement", "campaign"));
        }
        if (Boolean.TRUE.equals(visibility.getApprovalVisible())) {
            actions.add(buildAction("审批中心", "处理请假与财务审批", "/function/approval", "fact_check"));
        }
        if (Boolean.TRUE.equals(visibility.getReceiptVisible())) {
            actions.add(buildAction("出库单打印", "跟进待打印出库单", "/function/receipt", "receipt_long"));
        }
        if (hasAnyPermission("employee:list", "employee:create", "employee:update")) {
            actions.add(buildAction("员工管理", "查看组织与员工状态", "/function/employee", "groups"));
        }
        if (hasAnyPermission("customer:page", "customer:add")) {
            actions.add(buildAction("客户管理", "维护客户与项目资料", "/function/customer", "handshake"));
        }
        if (hasAnyPermission("price:list", "price:publish")) {
            actions.add(buildAction("价格管理", "查看价格与变更情况", "/function/price", "sell"));
        }
        return actions;
    }

    private List<DashboardAiAdviceVO> limitAdvices(List<DashboardAiAdviceVO> advices, Integer limit) {
        if (advices == null || advices.isEmpty()) {
            return List.of();
        }
        if (limit == null) {
            return advices;
        }
        int safeLimit = Math.max(0, Math.min(limit, 50));
        if (safeLimit == 0) {
            return List.of();
        }
        return advices.stream().limit(safeLimit).toList();
    }

    private DashboardOverviewVO.QuickAction buildAction(String title, String description, String route, String icon) {
        DashboardOverviewVO.QuickAction action = new DashboardOverviewVO.QuickAction();
        action.setTitle(title);
        action.setDescription(description);
        action.setRoute(route);
        action.setIcon(icon);
        return action;
    }

    private boolean hasAnyPermission(String... permCodes) {
        for (String permCode : permCodes) {
            if (TenantPermissionContext.hasPermission(permCode)) {
                return true;
            }
        }
        Set<String> currentPermCodes = TenantPermissionContext.getPermCodes();
        return currentPermCodes.contains("*") || currentPermCodes.contains("*:*");
    }

    private String buildScopedCacheKey(String cacheName, String tenantCode, Long userId) {
        Set<String> permCodes = TenantPermissionContext.getPermCodes();
        String permSignature = permCodes == null ? "" : String.join(",", new TreeSet<>(permCodes));
        return redisKeyBuilder.cache("management", "dashboard", cacheName, tenantCode, String.valueOf(userId),
                Integer.toHexString(permSignature.hashCode()));
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "--" : time.format(TIME_FORMATTER);
    }

    private LocalDateTime parseAlertTime(DashboardOverviewVO.AlertItem item) {
        try {
            String value = item.getTime();
            if (value == null || "--".equals(value)) {
                return LocalDateTime.MIN;
            }
            return LocalDateTime.parse(
                    LocalDate.now().getYear() + "-" + value.replace(" ", "T") + ":00",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            );
        } catch (Exception ignored) {
            return LocalDateTime.MIN;
        }
    }

    private String resolveAttendanceStatus(DashboardAttendanceAlertRowVO row) {
        return AttendancePunchStatusEnum.resolveDashboardText(row.getSignInStatus(), row.getSignOutStatus());
    }

    private DashboardOverviewVO getCachedOverview(String cacheKey) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached == null || cached.isBlank()) {
                return null;
            }
            return objectMapper.readValue(cached, DashboardOverviewVO.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<DashboardAiAdviceVO> getCachedAiAdvices(String cacheKey) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached == null || cached.isBlank()) {
                return null;
            }
            return objectMapper.readValue(cached, new TypeReference<List<DashboardAiAdviceVO>>() {
            });
        } catch (Exception ignored) {
            return null;
        }
    }

    private void cacheOverview(String cacheKey, DashboardOverviewVO value) {
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(value), OVERVIEW_CACHE_TTL);
        } catch (Exception ignored) {
        }
    }

    private void cacheAiAdvices(String cacheKey, List<DashboardAiAdviceVO> value) {
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(value), AI_ADVICE_CACHE_TTL);
        } catch (Exception ignored) {
        }
    }

    private void logSlowOperation(String operation, long startNanos, long thresholdMillis) {
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        if (elapsedMillis >= thresholdMillis) {
            log.warn("{} slow, elapsedMillis={}", operation, elapsedMillis);
        }
    }
}
