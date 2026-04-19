package my.management.module.dashboard.service;

import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.management.module.dashboard.mapper.DashboardMapper;
import my.management.module.dashboard.model.vo.DashboardAttendanceAlertRowVO;
import my.management.module.dashboard.model.vo.DashboardInventoryTrendRowVO;
import my.management.module.dashboard.model.vo.DashboardInventoryWarningRowVO;
import my.management.module.dashboard.model.vo.DashboardOverviewVO;
import my.management.module.dashboard.model.vo.DashboardPendingPrintRowVO;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
/**
 * DashboardService 属于管理端后端总览大盘模块，实现核心业务编排与规则逻辑。
 */
@Service
public class DashboardService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final DateTimeFormatter DAY_PREFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final BigDecimal INVENTORY_WARNING_THRESHOLD = new BigDecimal("100");
    private static final Duration OVERVIEW_CACHE_TTL = Duration.ofSeconds(20);

    @Resource
    private DashboardMapper dashboardMapper;

    private final ConcurrentMap<String, DashboardCacheEntry> overviewCache = new ConcurrentHashMap<>();

    public DashboardOverviewVO overview() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        String cacheKey = buildCacheKey(tenantCode, userId);
        DashboardCacheEntry cached = overviewCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.value();
        }

        DashboardOverviewVO vo = new DashboardOverviewVO();
        DashboardOverviewVO.Visibility visibility = buildVisibility();
        vo.setVisibility(visibility);
        vo.setSummary(buildSummary(tenantCode, userId, visibility));
        buildTrend(tenantCode, visibility, vo);
        vo.setBusinessAlerts(buildBusinessAlerts(tenantCode, visibility));
        vo.setAttendanceAlerts(buildAttendanceAlerts(tenantCode, visibility));
        vo.setQuickActions(buildQuickActions(visibility));

        // Dashboard traffic is read-heavy, so a short cache window removes repeated aggregate queries.
        overviewCache.put(cacheKey, new DashboardCacheEntry(vo, System.currentTimeMillis() + OVERVIEW_CACHE_TTL.toMillis()));
        return vo;
    }

    private DashboardOverviewVO.Visibility buildVisibility() {
        DashboardOverviewVO.Visibility visibility = new DashboardOverviewVO.Visibility();
        visibility.setOrderVisible(hasAnyPermission("sales:order:list", "sales:order:*", "production:order:list", "production:order:*"));
        visibility.setInventoryVisible(hasAnyPermission("inventory", "inventory:*", "inventory:warning:list", "inventory:trend"));
        visibility.setApprovalVisible(hasAnyPermission("approval:*", "approval:leave:audit", "approval:finance:audit", "approval:leave", "approval:finance"));
        visibility.setReceiptVisible(hasAnyPermission("receipt:print:list", "receipt:print:detail", "receipt:print:mark", "receipt:print:cancel"));
        visibility.setTrendVisible(Boolean.TRUE.equals(visibility.getInventoryVisible()));
        visibility.setAttendanceVisible(hasAnyPermission("employee:list", "attendance:*", "attendance:record:list"));
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
            summary.setInventoryWarningCount(nvl(dashboardMapper.countLowStockModels(tenantCode, INVENTORY_WARNING_THRESHOLD)));
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
        List<DashboardInventoryTrendRowVO> rows = dashboardMapper.selectInventoryTrend(
                tenantCode,
                startDate.atStartOfDay(),
                today.plusDays(1).atStartOfDay().minusSeconds(1)
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
            List<DashboardInventoryWarningRowVO> warnings = dashboardMapper.selectLowStockModels(tenantCode, INVENTORY_WARNING_THRESHOLD, 3);
            for (DashboardInventoryWarningRowVO warning : warnings) {
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

    private List<DashboardOverviewVO.QuickAction> buildQuickActions(DashboardOverviewVO.Visibility visibility) {
        List<DashboardOverviewVO.QuickAction> actions = new ArrayList<>();
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

    private String buildCacheKey(String tenantCode, Long userId) {
        Set<String> permCodes = TenantPermissionContext.getPermCodes();
        String permSignature = permCodes == null ? "" : String.join(",", new TreeSet<>(permCodes));
        return tenantCode + ":" + userId + ":" + Integer.toHexString(permSignature.hashCode());
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
        if (row.getSignInStatus() != null) {
            if (row.getSignInStatus() == 1) {
                return "迟到";
            }
            if (row.getSignInStatus() == 3 || row.getSignInStatus() == 6) {
                return "缺勤";
            }
        }
        if (row.getSignOutStatus() != null) {
            if (row.getSignOutStatus() == 2) {
                return "早退";
            }
            if (row.getSignOutStatus() == 3 || row.getSignOutStatus() == 6) {
                return "缺卡";
            }
        }
        return "考勤异常";
    }

    private record DashboardCacheEntry(DashboardOverviewVO value, long expireAt) {
        private boolean isExpired() {
            return System.currentTimeMillis() >= expireAt;
        }
    }
}
