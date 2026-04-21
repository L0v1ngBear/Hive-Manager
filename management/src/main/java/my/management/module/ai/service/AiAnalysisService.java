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
        return buildAllDashboardAdvices(tenantCode, visibility).stream().limit(4).toList();
    }

    /**
     * 构建完整 AI 建议列表，供“查看更多建议”页面使用。
     */
    public List<DashboardAiAdviceVO> buildAllDashboardAdvices(String tenantCode, DashboardOverviewVO.Visibility visibility) {
        List<DashboardAiAdviceVO> advices = new ArrayList<>();

        if (Boolean.TRUE.equals(visibility.getInventoryVisible())) {
            advices.addAll(buildInventoryAdvices(tenantCode));
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

        advices.add(buildOrderStructureAdvice());
        advices.add(buildInventoryCycleAdvice());

        DashboardAiAdviceVO badProductAdvice = buildBadProductAdvice(tenantCode);
        if (badProductAdvice != null) {
            advices.add(badProductAdvice);
        }

        advices.add(buildFinanceAdvice());
        advices.add(buildOperationAdvice());

        if (advices.isEmpty()) {
            advices.add(buildStableAdvice());
        }

        return advices;
    }

    private List<DashboardAiAdviceVO> buildInventoryAdvices(String tenantCode) {
        List<DashboardInventoryWarningRowVO> warnings = dashboardMapper.selectLowStockModels(tenantCode, INVENTORY_WARNING_THRESHOLD, 8);
        if (warnings == null || warnings.isEmpty()) {
            return List.of(buildInventoryStableAdvice());
        }

        List<DashboardAiAdviceVO> advices = new ArrayList<>();
        DashboardInventoryWarningRowVO first = warnings.get(0);
        DashboardAiAdviceVO advice = baseAdvice("inventory", "warning", "inventory_2");
        advice.setTitle("库存水位预警");
        advice.setSummary(String.format(
                "经库存水位测算，当前有 %d 个布匹型号低于 100 米安全阈值，其中 %s 最为紧张，剩余 %s 米。",
                warnings.size(),
                defaultText(first.getModelCode(), "未命名型号"),
                formatNumber(first.getTotalMeters())
        ));
        advice.setSuggestion("建议仓库先核对该型号实际库存，销售同步确认近期订单需求；如确认存在缺口，应优先安排采购补货或跨批次调拨，避免交付前才暴露断料风险。");
        advice.setRoute("/function/inventory");
        advices.add(advice);

        if (warnings.size() >= 3) {
            DashboardAiAdviceVO structureAdvice = baseAdvice("inventory", "info", "warehouse");
            structureAdvice.setTitle("库存结构优化建议");
            structureAdvice.setSummary(String.format("当前至少有 %d 个型号低于安全阈值，库存风险已从单点缺料转向结构性偏紧。", warnings.size()));
            structureAdvice.setSuggestion("建议建立每日低库存复盘机制，由仓库输出清单，销售确认订单消耗，生产确认排产节奏，形成补货优先级，而不是按发现顺序被动处理。");
            structureAdvice.setRoute("/function/inventory");
            advices.add(structureAdvice);
        }
        return advices;
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
        advice.setTitle("客户活跃度预警");
        advice.setSummary(String.format(
                "重点客户 %s 最近一次下单距今 %d 天，已明显高于其历史平均 %d 天复购周期。",
                candidate.customerName(),
                candidate.lastOrderDays(),
                candidate.avgCycleDays()
        ));
        advice.setSuggestion("建议销售负责人本周内安排定向回访，优先围绕历史合作项目、近期交付体验和下一批需求计划沟通，判断是正常采购间隔还是存在流失苗头。");
        advice.setRoute("/function/customer");
        return advice;
    }

    private DashboardAiAdviceVO buildInventoryStableAdvice() {
        DashboardAiAdviceVO advice = baseAdvice("inventory", "success", "inventory");
        advice.setTitle("库存水位整体平稳");
        advice.setSummary("当前没有发现低于 100 米安全阈值的布匹型号，库存短缺风险暂未显现。");
        advice.setSuggestion("建议继续保持每日出入库流水校验，并重点观察连续多日出库较快的型号，提前把固定阈值升级为动态安全库存。");
        advice.setRoute("/function/inventory");
        return advice;
    }

    private DashboardAiAdviceVO buildOrderStructureAdvice() {
        DashboardAiAdviceVO advice = baseAdvice("order", "info", "receipt_long");
        advice.setTitle("订单闭环复盘");
        advice.setSummary("销售订单、生产订单和出库打印已经形成核心履约链路，建议以订单闭环作为管理层周会固定议题。");
        advice.setSuggestion("重点看交付日期临近但未发货、有关联生产单但未进入生产中的订单，以及已出库但待打印的单据，提前把风险从交付当天前移到生产和出库环节。");
        advice.setRoute("/function/order");
        return advice;
    }

    private DashboardAiAdviceVO buildInventoryCycleAdvice() {
        DashboardAiAdviceVO advice = baseAdvice("inventory", "info", "cycle");
        advice.setTitle("库存周转策略");
        advice.setSummary("当前库存预警已具备型号和剩余米数维度，下一步可结合近 7 天出库速度形成预计可用天数。");
        advice.setSuggestion("建议先对高频型号设置更高安全库存，避免只有低于固定阈值才报警，导致热销型号预警偏晚；低频型号则重点控制积压和资金占用。");
        advice.setRoute("/function/inventory");
        return advice;
    }

    private DashboardAiAdviceVO buildFinanceAdvice() {
        DashboardAiAdviceVO advice = baseAdvice("finance", "info", "payments");
        advice.setTitle("经营金额跟踪");
        advice.setSummary("当前系统已沉淀订单金额、次品损失和审批事项，可作为财务健康分析的第一层数据基础。");
        advice.setSuggestion("建议后续把客户利润贡献、异常损耗占比和逾期未交付金额纳入看板，帮助管理层快速判断现金流压力、履约风险和成本侵蚀点。");
        advice.setRoute("/function/order");
        return advice;
    }

    private DashboardAiAdviceVO buildOperationAdvice() {
        DashboardAiAdviceVO advice = baseAdvice("operation", "info", "fact_check");
        advice.setTitle("生产运营节奏");
        advice.setSummary("库存预警、待审批、待打印和交付风险已经覆盖管理层日常需要关注的关键节点。");
        advice.setSuggestion("建议将这些指标纳入每日早会检查项，形成“发现异常、明确责任、跟进处理、复盘结果”的管理闭环；数据稳定后再接入大模型生成更完整的经营日报。");
        advice.setRoute("/dashboard");
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
        advice.setTitle("订单履约风险");
        advice.setSummary(String.format(
                "检测到 %d 张销售订单交付日期已临近但尚未完成发货，其中最早到期订单为 %s（客户：%s）。",
                rows.size(),
                defaultText(first.getOrderId(), "--"),
                defaultText(first.getCustomerName(), "未登记客户")
        ));
        advice.setSuggestion("建议立即核对该批订单的生产状态、出库准备和物流信息；若无法按期发货，应提前触发客户沟通，避免交付当天被动解释。");
        advice.setRoute("/function/order");
        return advice;
    }

    private DashboardAiAdviceVO buildBadProductAdvice(String tenantCode) {
        BadProductTypeSummaryRowVO row = aiAnalysisMapper.selectTopBadProductType(tenantCode, LocalDateTime.now().minusDays(30));
        if (row == null || row.getRecordCount() == null || row.getRecordCount() <= 0) {
            return null;
        }

        DashboardAiAdviceVO advice = baseAdvice("quality", "warning", "assignment_late");
        advice.setTitle("质量损耗洞察");
        advice.setSummary(String.format(
                "近 30 天次品问题主要集中在“%s”，共 %d 条记录，累计损失约 ¥%s。",
                typeLabel(row.getType()),
                row.getRecordCount(),
                formatNumber(row.getTotalLossAmount())
        ));
        advice.setSuggestion("建议由质量负责人拉通生产和仓库复盘该类型问题，确认高发环节、责任归属和处理方式；对重复出现的问题应沉淀为质检标准或作业提醒。");
        advice.setRoute("/function/bad-product");
        return advice;
    }

    private DashboardAiAdviceVO buildStableAdvice() {
        DashboardAiAdviceVO advice = baseAdvice("overview", "success", "check_circle");
        advice.setTitle("经营态势整体平稳");
        advice.setSummary("当前未识别出明显的库存、交付、客户复购或次品异常波动。");
        advice.setSuggestion("建议继续保持日常巡检节奏，重点观察未来 7 天订单交付、库存消耗和次品损耗变化，必要时再触发专项复盘。");
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
