package my.management.module.ai.service;

import jakarta.annotation.Resource;
import my.management.module.ai.mapper.AiAnalysisMapper;
import my.management.module.ai.model.vo.BadProductTypeSummaryRowVO;
import my.management.module.ai.model.vo.CustomerOrderDigestRowVO;
import my.management.module.ai.model.vo.DashboardAiAdviceVO;
import my.management.module.ai.model.vo.DueOrderRiskRowVO;
import my.management.module.dashboard.mapper.DashboardMapper;
import my.management.module.dashboard.model.vo.DashboardInventoryWarningRowVO;
import my.management.module.dashboard.model.vo.DashboardOverviewVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 分析服务第一阶段实现，先基于业务规则生成稳定可解释的建议内容。
 */
@Service
public class AiAnalysisService {

    private static final BigDecimal INVENTORY_WARNING_THRESHOLD = new BigDecimal("100");

    @Resource
    private AiAnalysisMapper aiAnalysisMapper;

    @Resource
    private DashboardMapper dashboardMapper;

    public List<DashboardAiAdviceVO> buildDashboardAdvices(String tenantCode, DashboardOverviewVO.Visibility visibility) {
        List<DashboardAiAdviceVO> advices = new ArrayList<>();

        if (Boolean.TRUE.equals(visibility.getInventoryVisible())) {
            DashboardAiAdviceVO inventoryAdvice = buildInventoryAdvice(tenantCode);
            if (inventoryAdvice != null) {
                advices.add(inventoryAdvice);
            }
        }

        if (Boolean.TRUE.equals(visibility.getOrderVisible())) {
            DashboardAiAdviceVO customerAdvice = buildCustomerAdvice(tenantCode);
            if (customerAdvice != null) {
                advices.add(customerAdvice);
            }

            DashboardAiAdviceVO deliveryAdvice = buildDeliveryAdvice(tenantCode);
            if (deliveryAdvice != null) {
                advices.add(deliveryAdvice);
            }
        }

        DashboardAiAdviceVO badProductAdvice = buildBadProductAdvice(tenantCode);
        if (badProductAdvice != null) {
            advices.add(badProductAdvice);
        }

        if (advices.isEmpty()) {
            advices.add(buildStableAdvice());
        }

        return advices.stream().limit(4).toList();
    }

    private DashboardAiAdviceVO buildInventoryAdvice(String tenantCode) {
        List<DashboardInventoryWarningRowVO> warnings = dashboardMapper.selectLowStockModels(tenantCode, INVENTORY_WARNING_THRESHOLD, 3);
        if (warnings == null || warnings.isEmpty()) {
            return null;
        }

        DashboardInventoryWarningRowVO first = warnings.get(0);
        DashboardAiAdviceVO advice = baseAdvice("inventory", "warning", "inventory_2");
        advice.setTitle("库存补货建议");
        advice.setSummary(String.format(
                "当前有 %d 个面料型号低于安全库存，最紧张的是 %s，仅剩 %s 米。",
                warnings.size(),
                defaultText(first.getModelCode(), "未命名型号"),
                formatNumber(first.getTotalMeters())
        ));
        advice.setSuggestion("建议优先核对在途采购和近期订单排产，先补最紧张型号，再逐步处理其余低库存面料。");
        advice.setRoute("/dashboard");
        return advice;
    }

    private DashboardAiAdviceVO buildCustomerAdvice(String tenantCode) {
        List<CustomerOrderDigestRowVO> rows = aiAnalysisMapper.selectCustomerOrderDigests(tenantCode, LocalDateTime.now().minusDays(180));
        if (rows == null || rows.isEmpty()) {
            return null;
        }

        Map<String, List<CustomerOrderDigestRowVO>> grouped = new LinkedHashMap<>();
        for (CustomerOrderDigestRowVO row : rows) {
            grouped.computeIfAbsent(row.getCustomerName(), key -> new ArrayList<>()).add(row);
        }

        CustomerFollowUpCandidate candidate = grouped.entrySet().stream()
                .map(entry -> toFollowUpCandidate(entry.getKey(), entry.getValue()))
                .filter(item -> item != null && item.orderCount() >= 2)
                .filter(item -> item.lastOrderDays() >= Math.max(item.avgCycleDays() + 7, 21))
                .sorted(Comparator.comparingLong((CustomerFollowUpCandidate item) -> item.lastOrderDays() - item.avgCycleDays()).reversed())
                .findFirst()
                .orElse(null);

        if (candidate == null) {
            return null;
        }

        DashboardAiAdviceVO advice = baseAdvice("customer", "info", "group");
        advice.setTitle("客户跟进建议");
        advice.setSummary(String.format(
                "客户 %s 最近一次下单距今 %d 天，明显高于其平均 %d 天的复购周期。",
                candidate.customerName(),
                candidate.lastOrderDays(),
                candidate.avgCycleDays()
        ));
        advice.setSuggestion("建议销售本周主动回访该客户，优先围绕其历史常购项目或近期交付过的型号做二次跟进。");
        advice.setRoute("/function/customer");
        return advice;
    }

    private DashboardAiAdviceVO buildDeliveryAdvice(String tenantCode) {
        String limitDate = LocalDate.now().plusDays(3).toString();
        List<DueOrderRiskRowVO> rows = aiAnalysisMapper.selectDueSalesOrders(tenantCode, limitDate, 3);
        if (rows == null || rows.isEmpty()) {
            return null;
        }

        DueOrderRiskRowVO first = rows.get(0);
        DashboardAiAdviceVO advice = baseAdvice("delivery", "warning", "local_shipping");
        advice.setTitle("交付风险提醒");
        advice.setSummary(String.format(
                "有 %d 张销售订单交付日期已临近但仍未完成发货，最早的是 %s（客户：%s）。",
                rows.size(),
                defaultText(first.getOrderId(), "--"),
                defaultText(first.getCustomerName(), "未登记客户")
        ));
        advice.setSuggestion("建议先核对临近交付订单的生产进度和物流准备情况，必要时提前通知客户调整交付预期。");
        advice.setRoute("/function/order");
        return advice;
    }

    private DashboardAiAdviceVO buildBadProductAdvice(String tenantCode) {
        BadProductTypeSummaryRowVO row = aiAnalysisMapper.selectTopBadProductType(tenantCode, LocalDateTime.now().minusDays(30));
        if (row == null || row.getRecordCount() == null || row.getRecordCount() <= 0) {
            return null;
        }

        DashboardAiAdviceVO advice = baseAdvice("quality", "warning", "assignment_late");
        advice.setTitle("质量分析建议");
        advice.setSummary(String.format(
                "近 30 天次品主要集中在“%s”，共 %d 条，累计损失约 ¥%s。",
                typeLabel(row.getType()),
                row.getRecordCount(),
                formatNumber(row.getTotalLossAmount())
        ));
        advice.setSuggestion("建议优先排查该类型问题的发生环节，并把最近处理方式与责任班组一起复盘，减少同类损耗继续放大。");
        advice.setRoute("/function/bad-product");
        return advice;
    }

    private DashboardAiAdviceVO buildStableAdvice() {
        DashboardAiAdviceVO advice = baseAdvice("overview", "success", "check_circle");
        advice.setTitle("经营数据整体平稳");
        advice.setSummary("当前未识别出明显的库存、交付、客户复购或次品异常波动。");
        advice.setSuggestion("建议继续保持日常巡检节奏，并观察未来 7 天订单和库存变化，必要时再触发专项分析。");
        advice.setRoute("/dashboard");
        return advice;
    }

    private DashboardAiAdviceVO baseAdvice(String category, String level, String icon) {
        DashboardAiAdviceVO advice = new DashboardAiAdviceVO();
        advice.setCategory(category);
        advice.setLevel(level);
        advice.setIcon(icon);
        return advice;
    }

    private CustomerFollowUpCandidate toFollowUpCandidate(String customerName, List<CustomerOrderDigestRowVO> orders) {
        if (orders == null || orders.size() < 2) {
            return null;
        }

        orders.sort(Comparator.comparing(CustomerOrderDigestRowVO::getCreateTime));
        long intervalTotal = 0;
        int intervalCount = 0;
        for (int index = 1; index < orders.size(); index++) {
            long days = ChronoUnit.DAYS.between(
                    orders.get(index - 1).getCreateTime().toLocalDate(),
                    orders.get(index).getCreateTime().toLocalDate()
            );
            if (days > 0) {
                intervalTotal += days;
                intervalCount++;
            }
        }
        if (intervalCount == 0) {
            return null;
        }

        LocalDateTime lastOrderTime = orders.get(orders.size() - 1).getCreateTime();
        long lastOrderDays = Duration.between(lastOrderTime, LocalDateTime.now()).toDays();
        long avgCycleDays = Math.max(Math.round((double) intervalTotal / intervalCount), 1);
        return new CustomerFollowUpCandidate(customerName, orders.size(), lastOrderDays, avgCycleDays);
    }

    private String typeLabel(String type) {
        return switch (type) {
            case "quality" -> "质量问题";
            case "damage" -> "运输破损";
            case "wrong" -> "生产错误";
            case "other" -> "其他原因";
            default -> "未分类问题";
        };
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatNumber(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private record CustomerFollowUpCandidate(String customerName, int orderCount, long lastOrderDays, long avgCycleDays) {
    }
}
