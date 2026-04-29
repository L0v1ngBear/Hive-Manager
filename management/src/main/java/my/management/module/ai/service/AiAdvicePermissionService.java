package my.management.module.ai.service;

import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.module.ai.model.vo.DashboardAiAdviceVO;
import my.management.module.dashboard.model.vo.DashboardOverviewVO;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 建议权限服务。
 *
 * <p>AI 建议包含库存、订单、客户、质量、财务、员工和运营等不同管理维度。这里统一维护
 * “全局可见”和“维度可见”的判断规则，避免控制器、通知任务和前端各自写一套权限逻辑。</p>
 */
@Service
public class AiAdvicePermissionService {

    /** 保留旧版全局权限，已有老板或管理层角色不需要重新配置即可继续查看全部 AI 建议。 */
    public static final String GLOBAL_VIEW_PERMISSION = "dashboard:ai:view";
    public static final String AI_ALL_PERMISSION = "dashboard:ai:*";
    public static final String DASHBOARD_ALL_PERMISSION = "dashboard:*";

    public static final List<String> ENTRY_PERMISSION_CODES = List.of(
            GLOBAL_VIEW_PERMISSION,
            AI_ALL_PERMISSION,
            DASHBOARD_ALL_PERMISSION,
            "dashboard:ai:inventory",
            "dashboard:ai:order",
            "dashboard:ai:customer",
            "dashboard:ai:quality",
            "dashboard:ai:finance",
            "dashboard:ai:employee",
            "dashboard:ai:operation"
    );

    private static final Map<String, String> CATEGORY_PERMISSION_MAP = Map.ofEntries(
            Map.entry("inventory", "dashboard:ai:inventory"),
            Map.entry("order", "dashboard:ai:order"),
            Map.entry("delivery", "dashboard:ai:order"),
            Map.entry("customer", "dashboard:ai:customer"),
            Map.entry("quality", "dashboard:ai:quality"),
            Map.entry("finance", "dashboard:ai:finance"),
            Map.entry("employee", "dashboard:ai:employee"),
            Map.entry("operation", "dashboard:ai:operation"),
            Map.entry("approval", "dashboard:ai:operation")
    );

    public boolean canViewAny() {
        return hasGlobalPermission() || ENTRY_PERMISSION_CODES.stream()
                .filter(code -> code.startsWith("dashboard:ai:"))
                .anyMatch(TenantPermissionContext::hasPermission);
    }

    public void requireAnyView() {
        if (!canViewAny()) {
            throw new BusinessException(403, "您没有权限查看 AI 建议");
        }
    }

    public boolean hasGlobalPermission() {
        return TenantPermissionContext.hasPermission(GLOBAL_VIEW_PERMISSION)
                || TenantPermissionContext.hasPermission(AI_ALL_PERMISSION)
                || TenantPermissionContext.hasPermission(DASHBOARD_ALL_PERMISSION)
                || TenantPermissionContext.hasPermission("*")
                || TenantPermissionContext.hasPermission("*:*");
    }

    public boolean canViewCategory(String category) {
        if (hasGlobalPermission()) {
            return true;
        }
        String permissionCode = resolvePermissionCode(category);
        return permissionCode != null && TenantPermissionContext.hasPermission(permissionCode);
    }

    public boolean canViewAny(Set<String> permissionCodes) {
        return hasGlobalPermission(permissionCodes)
                || ENTRY_PERMISSION_CODES.stream()
                .filter(code -> code.startsWith("dashboard:ai:"))
                .anyMatch(code -> hasPermission(permissionCodes, code));
    }

    public boolean canViewCategory(String category, Set<String> permissionCodes) {
        if (hasGlobalPermission(permissionCodes)) {
            return true;
        }
        String permissionCode = resolvePermissionCode(category);
        return permissionCode != null && hasPermission(permissionCodes, permissionCode);
    }

    public List<DashboardAiAdviceVO> filterVisible(List<DashboardAiAdviceVO> advices) {
        if (advices == null || advices.isEmpty()) {
            return List.of();
        }
        if (hasGlobalPermission()) {
            return advices;
        }
        return advices.stream()
                .filter(advice -> canViewCategory(advice == null ? null : advice.getCategory()))
                .toList();
    }

    public Set<String> parsePermissionCodes(String permissionCodes) {
        if (permissionCodes == null || permissionCodes.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(permissionCodes.split(","))
                .map(String::trim)
                .filter(code -> !code.isBlank())
                .collect(Collectors.toSet());
    }

    /**
     * 生成 AI 建议时使用的可见性。业务首页的普通指标仍按原业务权限控制；
     * AI 建议单独按 AI 维度权限补足需要的查询维度。
     */
    public DashboardOverviewVO.Visibility enrichVisibilityForAi(DashboardOverviewVO.Visibility source) {
        DashboardOverviewVO.Visibility visibility = copyVisibility(source);
        visibility.setAiAdviceVisible(canViewAny());
        if (!Boolean.TRUE.equals(visibility.getAiAdviceVisible())) {
            return visibility;
        }
        if (hasGlobalPermission()) {
            visibility.setOrderVisible(true);
            visibility.setInventoryVisible(true);
            visibility.setApprovalVisible(true);
            visibility.setReceiptVisible(true);
            visibility.setTrendVisible(true);
            visibility.setAttendanceVisible(true);
            return visibility;
        }
        if (canViewCategory("inventory")) {
            visibility.setInventoryVisible(true);
            visibility.setTrendVisible(true);
        }
        if (canViewCategory("order") || canViewCategory("delivery") || canViewCategory("customer")) {
            visibility.setOrderVisible(true);
        }
        if (canViewCategory("employee")) {
            visibility.setAttendanceVisible(true);
        }
        if (canViewCategory("operation")) {
            visibility.setApprovalVisible(true);
            visibility.setReceiptVisible(true);
        }
        return visibility;
    }

    private DashboardOverviewVO.Visibility copyVisibility(DashboardOverviewVO.Visibility source) {
        DashboardOverviewVO.Visibility visibility = new DashboardOverviewVO.Visibility();
        if (source == null) {
            return visibility;
        }
        visibility.setOrderVisible(source.getOrderVisible());
        visibility.setInventoryVisible(source.getInventoryVisible());
        visibility.setApprovalVisible(source.getApprovalVisible());
        visibility.setReceiptVisible(source.getReceiptVisible());
        visibility.setTrendVisible(source.getTrendVisible());
        visibility.setAttendanceVisible(source.getAttendanceVisible());
        visibility.setAiAdviceVisible(source.getAiAdviceVisible());
        return visibility;
    }

    private String resolvePermissionCode(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return CATEGORY_PERMISSION_MAP.get(category.trim().toLowerCase(Locale.ROOT));
    }

    private boolean hasGlobalPermission(Set<String> permissionCodes) {
        return hasPermission(permissionCodes, GLOBAL_VIEW_PERMISSION)
                || hasPermission(permissionCodes, AI_ALL_PERMISSION)
                || hasPermission(permissionCodes, DASHBOARD_ALL_PERMISSION)
                || hasPermission(permissionCodes, "*")
                || hasPermission(permissionCodes, "*:*");
    }

    private boolean hasPermission(Set<String> permissionCodes, String requiredCode) {
        if (permissionCodes == null || permissionCodes.isEmpty() || requiredCode == null || requiredCode.isBlank()) {
            return false;
        }
        if (permissionCodes.contains("*") || permissionCodes.contains("*:*") || permissionCodes.contains(requiredCode)) {
            return true;
        }
        int index = requiredCode.length();
        while ((index = requiredCode.lastIndexOf(':', index - 1)) > 0) {
            String wildcardCode = requiredCode.substring(0, index) + ":*";
            if (permissionCodes.contains(wildcardCode)) {
                return true;
            }
        }
        return false;
    }
}
