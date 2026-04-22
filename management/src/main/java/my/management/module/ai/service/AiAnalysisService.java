package my.management.module.ai.service;

import jakarta.annotation.Resource;
import my.management.module.ai.mapper.AiAnalysisMapper;
import my.management.module.ai.model.vo.BadProductTypeSummaryRowVO;
import my.management.module.ai.model.vo.AiBusinessSnapshotVO;
import my.management.module.ai.model.vo.CustomerOrderDigestRowVO;
import my.management.module.ai.model.vo.DashboardAiAdviceVO;
import my.management.module.ai.model.vo.DueOrderRiskRowVO;
import my.management.module.ai.provider.AiInsightProvider;
import my.management.module.dashboard.mapper.DashboardMapper;
import my.management.module.dashboard.model.vo.DashboardInventoryWarningRowVO;
import my.management.module.dashboard.model.vo.DashboardOverviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AI 分析服务第一阶段实现，先基于业务规则生成稳定可解释的建议内容。
 */
@Service
public class AiAnalysisService {

    private static final BigDecimal INVENTORY_WARNING_THRESHOLD = new BigDecimal("100");
    private static final DateTimeFormatter ADVICE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private AiAnalysisMapper aiAnalysisMapper;

    @Resource
    private DashboardMapper dashboardMapper;

    @Autowired(required = false)
    private List<AiInsightProvider> aiInsightProviders = List.of();

    public List<DashboardAiAdviceVO> buildDashboardAdvices(String tenantCode, DashboardOverviewVO.Visibility visibility) {
        return buildAllDashboardAdvices(tenantCode, visibility).stream().limit(4).toList();
    }

    /**
     * 构建完整 AI 建议列表，供“查看更多建议”页面使用。
     */
    public List<DashboardAiAdviceVO> buildAllDashboardAdvices(String tenantCode, DashboardOverviewVO.Visibility visibility) {
        AiBusinessSnapshotVO snapshot = buildBusinessSnapshot(tenantCode);
        List<DashboardAiAdviceVO> advices = new ArrayList<>();

        advices.addAll(buildSnapshotAnalyticAdvices(snapshot, visibility));

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

        advices.addAll(buildProviderAdvices(snapshot, advices));

        if (advices.isEmpty()) {
            advices.add(buildStableAdvice());
        }

        return advices;
    }

    /**
     * 调用可选的大模型 Provider 生成增强建议。
     *
     * <p>Provider 失败时直接降级为空列表，避免外部模型异常影响总览大盘加载。</p>
     */
    private List<DashboardAiAdviceVO> buildProviderAdvices(AiBusinessSnapshotVO snapshot, List<DashboardAiAdviceVO> baselineAdvices) {
        if (aiInsightProviders == null || aiInsightProviders.isEmpty()) {
            return List.of();
        }

        List<DashboardAiAdviceVO> result = new ArrayList<>();
        for (AiInsightProvider provider : aiInsightProviders) {
            if (provider == null || !provider.enabled()) {
                continue;
            }
            try {
                List<DashboardAiAdviceVO> generated = provider.generate(snapshot, baselineAdvices);
                if (generated != null) {
                    generated.stream()
                            .filter(Objects::nonNull)
                            .map(this::normalizeProviderAdvice)
                            .forEach(result::add);
                }
            } catch (Exception ignored) {
                // 大模型增强不能阻断本地规则建议，异常统一降级。
            }
        }
        return result;
    }

    /**
     * 兜底补齐模型输出，保证前端拿到的字段结构稳定。
     */
    private DashboardAiAdviceVO normalizeProviderAdvice(DashboardAiAdviceVO advice) {
        if (isBlank(advice.getCategory())) {
            advice.setCategory("overview");
        }
        if (isBlank(advice.getLevel())) {
            advice.setLevel("info");
        }
        if (isBlank(advice.getIcon())) {
            advice.setIcon("tips_and_updates");
        }
        if (isBlank(advice.getRoute())) {
            advice.setRoute(resolveRoute(advice.getCategory()));
        }
        if (isBlank(advice.getPriority())) {
            advice.setPriority(resolvePriority(advice.getLevel()));
        }
        if (isBlank(advice.getOwnerDepartment())) {
            advice.setOwnerDepartment(resolveOwnerDepartment(advice.getCategory()));
        }
        if (isBlank(advice.getActionLabel())) {
            advice.setActionLabel(resolveActionLabel(advice.getCategory()));
        }
        if (isBlank(advice.getMetricText())) {
            advice.setMetricText(resolveMetricText(advice.getCategory()));
        }
        if (isBlank(advice.getGeneratedAt())) {
            advice.setGeneratedAt(LocalDateTime.now().format(ADVICE_TIME_FORMATTER));
        }
        if (isBlank(advice.getTrackingHint())) {
            advice.setTrackingHint(resolveTrackingHint(advice.getCategory(), advice.getLevel()));
        }
        if (isBlank(advice.getSourceType())) {
            advice.setSourceType("llm");
        }
        if (advice.getConfidence() == null) {
            advice.setConfidence(resolveConfidence(advice.getLevel()));
        }
        return advice;
    }

    /**
     * 生成 AI 分析所需的经营快照。
     *
     * <p>后续接入大模型时，应优先把该快照序列化后交给模型，而不是让模型直接访问数据库。</p>
     */
    public AiBusinessSnapshotVO buildBusinessSnapshot(String tenantCode) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start30d = now.minusDays(30);
        LocalDateTime start90d = now.minusDays(90);

        AiBusinessSnapshotVO snapshot = new AiBusinessSnapshotVO();
        snapshot.setTenantCode(tenantCode);
        snapshot.setGeneratedAt(now);

        snapshot.getOrder().setSalesOrderCount30d(nvl(aiAnalysisMapper.countSalesOrdersSince(tenantCode, start30d)));
        snapshot.getOrder().setProductionOrderCount30d(nvl(aiAnalysisMapper.countProductionOrdersSince(tenantCode, start30d)));
        snapshot.getOrder().setUnshippedDueSoonCount(nvl(aiAnalysisMapper.countDueSoonUnshippedOrders(tenantCode, LocalDate.now().plusDays(3).toString())));
        snapshot.getOrder().setProducingCount(nvl(aiAnalysisMapper.countProducingOrders(tenantCode)));
        snapshot.getOrder().setSalesAmount30d(scale(aiAnalysisMapper.sumSalesAmountSince(tenantCode, start30d)));

        snapshot.getInventory().setTotalMeters(scale(aiAnalysisMapper.sumInventoryMeters(tenantCode)));
        snapshot.getInventory().setLowStockModelCount(nvl(aiAnalysisMapper.countLowStockModels(tenantCode, INVENTORY_WARNING_THRESHOLD)));
        snapshot.getInventory().setBadClothCount(nvl(aiAnalysisMapper.countBadCloth(tenantCode)));

        snapshot.getQuality().setBadProductCount30d(nvl(aiAnalysisMapper.countBadProductRecordsBetween(tenantCode, start30d, now)));
        snapshot.getQuality().setBadProductLoss30d(scale(aiAnalysisMapper.sumBadProductLossSince(tenantCode, start30d)));
        snapshot.getQuality().setPendingBadProductCount(nvl(aiAnalysisMapper.countPendingBadProduct(tenantCode)));

        snapshot.getCustomer().setCustomerCount(nvl(aiAnalysisMapper.countCustomers(tenantCode)));
        snapshot.getCustomer().setActiveCustomerCount90d(nvl(aiAnalysisMapper.countActiveCustomersSince(tenantCode, start90d)));

        snapshot.getFinance().setPendingFinanceApprovalCount(nvl(aiAnalysisMapper.countPendingFinanceApprovals(tenantCode)));
        snapshot.getFinance().setPendingFinanceAmount(scale(aiAnalysisMapper.sumPendingFinanceAmount(tenantCode)));
        return snapshot;
    }

    private List<DashboardAiAdviceVO> buildSnapshotAnalyticAdvices(AiBusinessSnapshotVO snapshot, DashboardOverviewVO.Visibility visibility) {
        List<DashboardAiAdviceVO> advices = new ArrayList<>();

        if (Boolean.TRUE.equals(visibility.getOrderVisible())) {
            DashboardAiAdviceVO fulfillment = buildFulfillmentPressureAdvice(snapshot);
            if (fulfillment != null) {
                advices.add(fulfillment);
            }
        }

        DashboardAiAdviceVO qualityCost = buildQualityCostAdvice(snapshot);
        if (qualityCost != null) {
            advices.add(qualityCost);
        }

        DashboardAiAdviceVO customerActivity = buildCustomerActivityAdvice(snapshot);
        if (customerActivity != null) {
            advices.add(customerActivity);
        }

        DashboardAiAdviceVO financePressure = buildFinancePressureAdvice(snapshot);
        if (financePressure != null) {
            advices.add(financePressure);
        }
        return advices;
    }

    private DashboardAiAdviceVO buildFulfillmentPressureAdvice(AiBusinessSnapshotVO snapshot) {
        long dueSoon = snapshot.getOrder().getUnshippedDueSoonCount();
        long producing = snapshot.getOrder().getProducingCount();
        if (dueSoon <= 0) {
            return null;
        }

        DashboardAiAdviceVO advice = baseAdvice("delivery", dueSoon > Math.max(producing, 1) ? "warning" : "info", "local_shipping");
        advice.setTitle("履约压力交叉研判");
        advice.setSummary(String.format(
                "未来 3 天内仍有 %d 张销售订单未完成发货，当前生产中订单为 %d 张，交付压力需要提前排查。",
                dueSoon,
                producing
        ));
        advice.setSuggestion("建议销售先确认客户交期是否可调整，生产同步核对相关生产单是否已进入关键工序，仓库提前准备可发货明细；若未发货订单数量高于生产中订单，应优先排查是否存在未建生产单或生产状态未同步。");
        advice.setReasoning(String.format("经营快照显示：临近交付未发货=%d，生产中订单=%d。该建议由交付风险与生产承接能力交叉判断生成。", dueSoon, producing));
        advice.setRoute("/function/order");
        return advice;
    }

    private DashboardAiAdviceVO buildQualityCostAdvice(AiBusinessSnapshotVO snapshot) {
        BigDecimal salesAmount = snapshot.getOrder().getSalesAmount30d();
        BigDecimal lossAmount = snapshot.getQuality().getBadProductLoss30d();
        long badCount = snapshot.getQuality().getBadProductCount30d();
        if (badCount <= 0 || lossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal ratio = salesAmount.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO
                : lossAmount.multiply(new BigDecimal("100")).divide(salesAmount, 2, RoundingMode.HALF_UP);

        DashboardAiAdviceVO advice = baseAdvice("quality", ratio.compareTo(new BigDecimal("3")) >= 0 ? "warning" : "info", "assignment_late");
        advice.setTitle("质量成本压力评估");
        advice.setSummary(String.format(
                "近 30 天登记次品 %d 条，损失金额约 ￥%s；按同期销售金额测算，质量损耗占比约 %s%%。",
                badCount,
                formatNumber(lossAmount),
                formatNumber(ratio)
        ));
        advice.setSuggestion("建议质检负责人按次品类型、订单来源和处理方式复盘高损耗记录；若损耗占比持续高于 3%，应把该类问题升级为生产工艺或供应商质量专项，而不是只按单处理。");
        advice.setReasoning(String.format("经营快照显示：近30天销售额=￥%s，次品损失=￥%s，次品记录=%d。该建议由损耗金额与销售规模交叉测算生成。", formatNumber(salesAmount), formatNumber(lossAmount), badCount));
        advice.setRoute("/function/bad-product");
        return advice;
    }

    private DashboardAiAdviceVO buildCustomerActivityAdvice(AiBusinessSnapshotVO snapshot) {
        long customerCount = snapshot.getCustomer().getCustomerCount();
        long activeCount = snapshot.getCustomer().getActiveCustomerCount90d();
        if (customerCount <= 0) {
            return null;
        }

        BigDecimal activeRatio = BigDecimal.valueOf(activeCount)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(customerCount), 2, RoundingMode.HALF_UP);
        if (activeRatio.compareTo(new BigDecimal("40")) >= 0) {
            return null;
        }

        DashboardAiAdviceVO advice = baseAdvice("customer", activeRatio.compareTo(new BigDecimal("25")) < 0 ? "warning" : "info", "handshake");
        advice.setTitle("客户活跃结构评估");
        advice.setSummary(String.format(
                "当前客户档案共 %d 个，近 90 天有下单记录的客户为 %d 个，活跃占比约 %s%%。",
                customerCount,
                activeCount,
                formatNumber(activeRatio)
        ));
        advice.setSuggestion("建议销售负责人把客户分为活跃、沉睡和流失风险三类，优先对历史订单金额较高但 90 天未复购的客户安排回访，判断是正常采购周期、价格因素还是交付体验导致的活跃下降。");
        advice.setReasoning(String.format("经营快照显示：客户总数=%d，90天活跃客户=%d，活跃率=%s%%。该建议由客户资产规模与复购活跃度交叉判断生成。", customerCount, activeCount, formatNumber(activeRatio)));
        advice.setRoute("/function/customer");
        return advice;
    }

    private DashboardAiAdviceVO buildFinancePressureAdvice(AiBusinessSnapshotVO snapshot) {
        long pendingCount = snapshot.getFinance().getPendingFinanceApprovalCount();
        BigDecimal pendingAmount = snapshot.getFinance().getPendingFinanceAmount();
        if (pendingCount <= 0 || pendingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        DashboardAiAdviceVO advice = baseAdvice("finance", pendingAmount.compareTo(new BigDecimal("10000")) >= 0 ? "warning" : "info", "payments");
        advice.setTitle("财务审批资金占用评估");
        advice.setSummary(String.format(
                "当前有 %d 条财务审批待处理，涉及金额约 ￥%s，可能影响采购、报销或供应商付款节奏。",
                pendingCount,
                formatNumber(pendingAmount)
        ));
        advice.setSuggestion("建议财务负责人优先处理金额较大或影响生产采购的审批事项，避免关键物料付款、供应商对账或内部报销积压影响业务流转。");
        advice.setReasoning(String.format("经营快照显示：待审批财务事项=%d，待审批金额=￥%s。该建议由审批积压数量与资金金额共同判断生成。", pendingCount, formatNumber(pendingAmount)));
        advice.setRoute("/function/approval");
        return advice;
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
        advice.setGeneratedAt(LocalDateTime.now().format(ADVICE_TIME_FORMATTER));
        advice.setPriority(resolvePriority(level));
        advice.setOwnerDepartment(resolveOwnerDepartment(category));
        advice.setActionLabel(resolveActionLabel(category));
        advice.setMetricText(resolveMetricText(category));
        advice.setTrackingHint(resolveTrackingHint(category, level));
        advice.setSourceType("local_rules");
        advice.setConfidence(resolveConfidence(level));
        return advice;
    }

    private String resolvePriority(String level) {
        if ("warning".equals(level)) {
            return "P1";
        }
        if ("success".equals(level)) {
            return "P3";
        }
        return "P2";
    }

    private String resolveOwnerDepartment(String category) {
        return switch (category) {
            case "inventory" -> "仓库 / 采购 / 销售";
            case "order", "delivery" -> "销售 / 生产 / 仓库";
            case "customer" -> "销售负责人";
            case "quality" -> "质检 / 生产";
            case "finance" -> "财务 / 经营管理";
            case "operation" -> "运营负责人";
            default -> "管理层";
        };
    }

    private String resolveActionLabel(String category) {
        return switch (category) {
            case "inventory" -> "查看库存";
            case "order", "delivery" -> "跟进订单";
            case "customer" -> "查看客户";
            case "quality" -> "查看次品";
            case "finance" -> "查看订单金额";
            default -> "查看总览";
        };
    }

    private String resolveMetricText(String category) {
        return switch (category) {
            case "inventory" -> "库存余量、低库存型号、近期开单消耗";
            case "order" -> "销售订单、生产订单、履约状态";
            case "delivery" -> "交付日期、发货状态、物流完整度";
            case "customer" -> "客户复购周期、近 180 天下单节奏";
            case "quality" -> "近 30 天次品数量与损失金额";
            case "finance" -> "订单金额、损耗金额、审批事项";
            case "operation" -> "库存、订单、审批、打印任务联动";
            default -> "总览大盘核心指标";
        };
    }

    private String resolveTrackingHint(String category, String level) {
        if ("warning".equals(level)) {
            return "建议今日内确认责任人，并在下一次晨会复盘处理进度。";
        }
        return switch (category) {
            case "inventory" -> "建议纳入每日库存巡检，持续观察高频型号消耗速度。";
            case "customer" -> "建议安排销售跟进记录，持续沉淀客户复购节奏。";
            case "quality" -> "建议把重复问题沉淀为质检标准或生产作业提醒。";
            default -> "建议纳入周会跟进，形成发现、处理、复盘的管理闭环。";
        };
    }

    private Integer resolveConfidence(String level) {
        if ("warning".equals(level)) {
            return 88;
        }
        if ("success".equals(level)) {
            return 80;
        }
        return 72;
    }

    private String resolveRoute(String category) {
        return switch (category) {
            case "inventory" -> "/function/inventory";
            case "order", "delivery" -> "/function/order";
            case "customer" -> "/function/customer";
            case "quality" -> "/function/bad-product";
            case "finance" -> "/function/approval";
            default -> "/dashboard";
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private record CustomerFollowUpCandidate(String customerName, int orderCount, long lastOrderDays, long avgCycleDays) {
    }
}
