package my.management.module.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import my.management.module.ai.mapper.AiAdviceTrainingSampleMapper;
import my.management.module.behavior.mapper.BehaviorEventMapper;
import my.management.module.behavior.model.vo.BehaviorModulePreferenceVO;
import my.management.module.ai.mapper.AiAnalysisMapper;
import my.management.module.ai.model.entity.AiAdviceTrainingSample;
import my.management.module.ai.model.vo.AiAdviceLearningStatVO;
import my.management.module.ai.model.vo.BadProductTypeSummaryRowVO;
import my.management.module.ai.model.vo.AiBusinessSnapshotVO;
import my.management.module.ai.model.vo.CustomerOrderDigestRowVO;
import my.management.module.ai.model.vo.CustomerValueSummaryRowVO;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
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
    private static final DateTimeFormatter DAY_PREFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Resource
    private AiAnalysisMapper aiAnalysisMapper;

    @Resource
    private DashboardMapper dashboardMapper;

    @Resource
    private AiAdviceTrainingSampleMapper aiAdviceTrainingSampleMapper;

    @Resource
    private BehaviorEventMapper behaviorEventMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private List<AiInsightProvider> aiInsightProviders = List.of();

    public List<DashboardAiAdviceVO> buildDashboardAdvices(String tenantCode, DashboardOverviewVO.Visibility visibility) {
        return buildAllDashboardAdvices(tenantCode, visibility).stream().limit(4).toList();
    }

    /**
     * 构建完整 AI 建议列表，供“查看更多建议”页面使用。
     */
    public List<DashboardAiAdviceVO> buildAllDashboardAdvices(String tenantCode, DashboardOverviewVO.Visibility visibility) {
        if (visibility == null) {
            visibility = new DashboardOverviewVO.Visibility();
        }
        AiBusinessSnapshotVO snapshot = buildBusinessSnapshot(tenantCode);
        List<DashboardAiAdviceVO> advices = new ArrayList<>();

        advices.addAll(buildSnapshotAnalyticAdvices(snapshot, visibility));
        advices.addAll(buildCrossDomainDecisionAdvices(snapshot, visibility));
        advices.addAll(buildTrendDecisionAdvices(snapshot, visibility));

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

        if (isFullAiVisibility(visibility)) {
            advices.addAll(buildProviderAdvices(snapshot, advices));
        }
        Map<String, BehaviorModulePreferenceVO> behaviorPreferences = loadTenantBehaviorPreferences(tenantCode);
        applyTenantBehaviorPersonalization(behaviorPreferences, advices);
        Map<String, AiAdviceLearningStatVO> learningStats = loadTenantLearningStats(tenantCode);
        applyTenantFeedbackPersonalization(learningStats, advices);

        if (advices.isEmpty()) {
            advices.add(buildStableAdvice());
        }

        persistTrainingSamples(tenantCode, snapshot, behaviorPreferences, advices);
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
     * 只有拥有完整 AI 视野时才调用外部大模型 Provider，避免维度权限用户触发全量经营快照外发。
     */
    private boolean isFullAiVisibility(DashboardOverviewVO.Visibility visibility) {
        return Boolean.TRUE.equals(visibility.getOrderVisible())
                && Boolean.TRUE.equals(visibility.getInventoryVisible())
                && Boolean.TRUE.equals(visibility.getApprovalVisible())
                && Boolean.TRUE.equals(visibility.getReceiptVisible())
                && Boolean.TRUE.equals(visibility.getTrendVisible())
                && Boolean.TRUE.equals(visibility.getAttendanceVisible())
                && Boolean.TRUE.equals(visibility.getAiAdviceVisible());
    }

    /**
     * 根据当前租户近期行为偏好微调建议权重。
     *
     * <p>这一步不是模型训练，而是“租户级个性化校准”：同样的经营异常，不同租户的关注点不同。
     * 系统会把用户近期查看、点击、通知打开等行为聚合为偏好分，用于调整建议排序、置信度和跟进提示。</p>
     */
    private Map<String, BehaviorModulePreferenceVO> loadTenantBehaviorPreferences(String tenantCode) {
        Map<String, BehaviorModulePreferenceVO> preferenceMap = new LinkedHashMap<>();
        List<BehaviorModulePreferenceVO> preferences;
        try {
            preferences = behaviorEventMapper.selectTenantPreferences(tenantCode, 30);
        } catch (Exception ignored) {
            return preferenceMap;
        }
        for (BehaviorModulePreferenceVO preference : preferences) {
            if (preference != null && !isBlank(preference.getCategory())) {
                preferenceMap.put(preference.getCategory(), preference);
            }
        }
        return preferenceMap;
    }

    private void applyTenantBehaviorPersonalization(Map<String, BehaviorModulePreferenceVO> preferenceMap, List<DashboardAiAdviceVO> advices) {
        if (preferenceMap == null || preferenceMap.isEmpty() || advices == null || advices.isEmpty()) {
            return;
        }

        for (DashboardAiAdviceVO advice : advices) {
            BehaviorModulePreferenceVO preference = preferenceMap.get(advice.getCategory());
            if (preference == null || preference.getBehaviorScore() == null || preference.getBehaviorScore() <= 0) {
                continue;
            }
            int boost = Math.min((int) Math.round(preference.getBehaviorScore() / 8D), 10);
            advice.setConfidence(Math.min((advice.getConfidence() == null ? 70 : advice.getConfidence()) + boost, 98));
            if (!"success".equals(advice.getLevel()) && "P2".equals(advice.getPriority()) && nvl(preference.getClickCount()) > 0) {
                advice.setPriority("P1");
            }
            advice.setTrackingHint(buildPersonalizedTrackingHint(advice, preference));
        }

        advices.sort((left, right) -> {
            int priorityCompare = Integer.compare(priorityOrder(left.getPriority()), priorityOrder(right.getPriority()));
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            int behaviorCompare = Double.compare(
                    behaviorScore(preferenceMap.get(right.getCategory())),
                    behaviorScore(preferenceMap.get(left.getCategory()))
            );
            if (behaviorCompare != 0) {
                return behaviorCompare;
            }
            return String.valueOf(right.getGeneratedAt()).compareTo(String.valueOf(left.getGeneratedAt()));
        });
    }

    private String buildPersonalizedTrackingHint(DashboardAiAdviceVO advice, BehaviorModulePreferenceVO preference) {
        String base = isBlank(advice.getTrackingHint()) ? resolveTrackingHint(advice.getCategory(), advice.getLevel()) : advice.getTrackingHint();
        return base + String.format(
                " 结合本租户近30天行为，该维度被关注 %d 次、点击/打开 %d 次，建议优先按本租户管理习惯跟进。",
                nvl(preference.getTotalCount()),
                nvl(preference.getClickCount())
        );
    }

    private int priorityOrder(String priority) {
        return switch (priority == null ? "" : priority) {
            case "P0" -> 0;
            case "P1" -> 1;
            case "P2" -> 2;
            case "P3" -> 3;
            default -> 9;
        };
    }

    private double behaviorScore(BehaviorModulePreferenceVO preference) {
        return preference == null || preference.getBehaviorScore() == null ? 0D : preference.getBehaviorScore();
    }

    /**
     * 读取租户自己的 AI 建议反馈画像。
     *
     * <p>该画像只在后端用于排序和置信度校准，不单独展示给普通用户，避免页面增加理解成本。</p>
     */
    private Map<String, AiAdviceLearningStatVO> loadTenantLearningStats(String tenantCode) {
        Map<String, AiAdviceLearningStatVO> statMap = new LinkedHashMap<>();
        if (tenantCode == null || tenantCode.isBlank()) {
            return statMap;
        }
        try {
            List<AiAdviceLearningStatVO> stats = aiAdviceTrainingSampleMapper.selectLearningStats(tenantCode, 90);
            for (AiAdviceLearningStatVO stat : stats) {
                if (stat != null && !isBlank(stat.getCategory())) {
                    statMap.put(stat.getCategory(), stat);
                }
            }
        } catch (Exception ignored) {
            // 学习画像是增强能力，统计失败时不影响 AI 建议主流程。
        }
        return statMap;
    }

    /**
     * 根据历史反馈调整建议权重。
     *
     * <p>“有价值/已处理”的维度会小幅提升置信度和排序；“不准确/暂不采纳”的维度会降权。
     * 这里不把学习过程暴露到页面，只让建议结果逐步贴近租户真实偏好。</p>
     */
    private void applyTenantFeedbackPersonalization(Map<String, AiAdviceLearningStatVO> learningStats,
                                                   List<DashboardAiAdviceVO> advices) {
        if (learningStats == null || learningStats.isEmpty() || advices == null || advices.isEmpty()) {
            return;
        }

        for (DashboardAiAdviceVO advice : advices) {
            AiAdviceLearningStatVO stat = learningStats.get(defaultText(advice.getCategory(), "overview"));
            if (stat == null || nvl(stat.getFeedbackCount()) <= 0) {
                continue;
            }

            int feedbackScore = feedbackScore(stat);
            int confidence = advice.getConfidence() == null ? resolveConfidence(advice.getLevel()) : advice.getConfidence();
            if (feedbackScore > 0) {
                advice.setConfidence(Math.min(confidence + Math.min(feedbackScore * 2, 12), 98));
                if ("P2".equals(advice.getPriority()) && feedbackScore >= 4) {
                    advice.setPriority("P1");
                }
            } else if (feedbackScore < 0) {
                advice.setConfidence(Math.max(confidence + Math.max(feedbackScore * 3, -18), 45));
                if (!"warning".equals(advice.getLevel()) && !"P3".equals(advice.getPriority())) {
                    advice.setPriority("P3");
                }
            }
        }

        advices.sort((left, right) -> {
            int priorityCompare = Integer.compare(priorityOrder(left.getPriority()), priorityOrder(right.getPriority()));
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            int learnedCompare = Integer.compare(
                    learnedAdviceScore(learningStats.get(defaultText(right.getCategory(), "overview")), right),
                    learnedAdviceScore(learningStats.get(defaultText(left.getCategory(), "overview")), left)
            );
            if (learnedCompare != 0) {
                return learnedCompare;
            }
            return Integer.compare(nvlInt(right.getRiskScore()), nvlInt(left.getRiskScore()));
        });
    }

    private int learnedAdviceScore(AiAdviceLearningStatVO stat, DashboardAiAdviceVO advice) {
        int score = nvlInt(advice.getConfidence()) + nvlInt(advice.getRiskScore()) / 2;
        return score + feedbackScore(stat) * 4;
    }

    private int feedbackScore(AiAdviceLearningStatVO stat) {
        if (stat == null) {
            return 0;
        }
        long positive = nvl(stat.getPositiveCount());
        long resolved = nvl(stat.getResolvedCount());
        long negative = nvl(stat.getNegativeCount());
        long ignored = nvl(stat.getIgnoredCount());
        return (int) Math.max(Math.min(positive * 2 + resolved * 3 - negative * 3 - ignored, 10), -10);
    }

    /**
     * 沉淀 AI 训练样本。
     *
     * <p>样本以“当天 + 租户 + 建议标题 + 维度”去重，避免用户频繁刷新导致样本爆炸。
     * 后续微调专用模型时，可直接使用这些样本构造监督微调数据集。</p>
     */
    private void persistTrainingSamples(String tenantCode,
                                        AiBusinessSnapshotVO snapshot,
                                        Map<String, BehaviorModulePreferenceVO> behaviorPreferences,
                                        List<DashboardAiAdviceVO> advices) {
        if (tenantCode == null || tenantCode.isBlank() || advices == null || advices.isEmpty()) {
            return;
        }
        String snapshotJson = toJson(snapshot);
        String behaviorJson = toJson(behaviorPreferences == null ? Map.of() : behaviorPreferences);
        List<AiAdviceTrainingSample> samples = advices.stream()
                .filter(Objects::nonNull)
                .limit(30)
                .map(advice -> toTrainingSample(tenantCode, snapshotJson, behaviorJson, advice))
                .toList();
        if (samples.isEmpty()) {
            return;
        }
        try {
            aiAdviceTrainingSampleMapper.upsertBatch(samples);
        } catch (Exception ignored) {
            // 训练样本沉淀失败不能影响用户查看大盘。
        }
    }

    private AiAdviceTrainingSample toTrainingSample(String tenantCode,
                                                    String snapshotJson,
                                                    String behaviorJson,
                                                    DashboardAiAdviceVO advice) {
        AiAdviceTrainingSample sample = new AiAdviceTrainingSample();
        sample.setTenantCode(tenantCode);
        sample.setSampleKey(buildSampleKey(advice));
        advice.setSampleKey(sample.getSampleKey());
        sample.setCategory(advice.getCategory());
        sample.setTitle(defaultText(advice.getTitle(), "未命名建议"));
        sample.setSourceType(defaultText(advice.getSourceType(), "local_rules"));
        sample.setPriority(advice.getPriority());
        sample.setConfidence(advice.getConfidence());
        sample.setInputSnapshotJson(snapshotJson);
        sample.setBehaviorContextJson(behaviorJson);
        sample.setAdviceJson(toJson(advice));
        sample.setLabelStatus("unlabeled");
        return sample;
    }

    private String buildSampleKey(DashboardAiAdviceVO advice) {
        String raw = String.join("|",
                LocalDate.now().toString(),
                defaultText(advice.getCategory(), "overview"),
                defaultText(advice.getTitle(), ""),
                defaultText(advice.getRoute(), ""),
                defaultText(advice.getSourceType(), "local_rules")
        );
        return "AI_SAMPLE:" + sha256(raw).substring(0, 48);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ignored) {
            return "{}";
        }
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
        if (isBlank(advice.getDecisionType())) {
            advice.setDecisionType(resolveDecisionType(advice.getCategory(), advice.getLevel()));
        }
        if (advice.getRiskScore() == null) {
            advice.setRiskScore(resolveRiskScore(advice.getLevel()));
        }
        if (isBlank(advice.getImpactText())) {
            advice.setImpactText(resolveImpactText(advice.getCategory()));
        }
        if (isBlank(advice.getTimeWindow())) {
            advice.setTimeWindow(resolveTimeWindow(advice.getLevel()));
        }
        if (isBlank(advice.getFirstAction())) {
            advice.setFirstAction(resolveFirstAction(advice.getCategory()));
        }
        if (isBlank(advice.getReviewMetric())) {
            advice.setReviewMetric(resolveReviewMetric(advice.getCategory()));
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
        LocalDateTime start60d = now.minusDays(60);
        LocalDateTime start90d = now.minusDays(90);
        String todayPrefix = LocalDate.now().format(DAY_PREFIX_FORMATTER);

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
        snapshot.getCustomer().setActiveCustomerCount30d(nvl(aiAnalysisMapper.countActiveCustomersSince(tenantCode, start30d)));
        snapshot.getCustomer().setNewCustomerCount30d(nvl(aiAnalysisMapper.countNewCustomersSince(tenantCode, start30d)));
        snapshot.getCustomer().setInactiveCustomerCount90d(Math.max(snapshot.getCustomer().getCustomerCount() - snapshot.getCustomer().getActiveCustomerCount90d(), 0));
        CustomerValueSummaryRowVO topCustomer = aiAnalysisMapper.selectTopCustomerValueSince(tenantCode, start30d);
        if (topCustomer != null) {
            snapshot.getCustomer().setTopCustomerName30d(topCustomer.getCustomerName());
            snapshot.getCustomer().setTopCustomerAmount30d(scale(topCustomer.getTotalAmount()));
        }

        populateTrendMetrics(snapshot, tenantCode, start60d, start30d);

        try {
            snapshot.getEmployee().setTotalEmployeeCount(nvl(aiAnalysisMapper.countEmployees(tenantCode)));
            snapshot.getEmployee().setActiveEmployeeCount(nvl(aiAnalysisMapper.countActiveEmployees(tenantCode)));
            snapshot.getEmployee().setMissingManagerCount(Math.max(nvl(aiAnalysisMapper.countEmployeesWithoutManager(tenantCode)) - 1, 0));
            snapshot.getEmployee().setAttendanceExceptionCountToday(nvl(aiAnalysisMapper.countTodayAttendanceExceptions(tenantCode, todayPrefix)));
            snapshot.getEmployee().setLateCountToday(nvl(aiAnalysisMapper.countTodayLate(tenantCode, todayPrefix)));
            snapshot.getEmployee().setPendingLeaveApprovalCount(nvl(aiAnalysisMapper.countPendingLeaveApprovals(tenantCode)));
            snapshot.getEmployee().setLeaveRequestCount30d(nvl(aiAnalysisMapper.countLeaveRequestsSince(tenantCode, start30d)));
        } catch (Exception ignored) {
            // 员工、考勤或请假模块缺表时，AI 建议降级为经营维度，不影响大盘加载。
        }

        snapshot.getFinance().setPendingFinanceApprovalCount(nvl(aiAnalysisMapper.countPendingFinanceApprovals(tenantCode)));
        snapshot.getFinance().setPendingFinanceAmount(scale(aiAnalysisMapper.sumPendingFinanceAmount(tenantCode)));
        return snapshot;
    }

    /**
     * 补齐上一周期对比指标，帮助 AI 从“当前值异常”升级到“趋势正在变坏/变好”的判断。
     */
    private void populateTrendMetrics(AiBusinessSnapshotVO snapshot,
                                      String tenantCode,
                                      LocalDateTime start60d,
                                      LocalDateTime start30d) {
        try {
            long previousSalesOrders = nvl(aiAnalysisMapper.countSalesOrdersBetween(tenantCode, start60d, start30d));
            long previousProductionOrders = nvl(aiAnalysisMapper.countProductionOrdersBetween(tenantCode, start60d, start30d));
            BigDecimal previousSalesAmount = scale(aiAnalysisMapper.sumSalesAmountBetween(tenantCode, start60d, start30d));
            long previousBadProductCount = nvl(aiAnalysisMapper.countBadProductRecordsBetween(tenantCode, start60d, start30d));
            BigDecimal previousBadProductLoss = scale(aiAnalysisMapper.sumBadProductLossBetween(tenantCode, start60d, start30d));
            long previousActiveCustomers = nvl(aiAnalysisMapper.countActiveCustomersBetween(tenantCode, start60d, start30d));
            long previousNewCustomers = nvl(aiAnalysisMapper.countNewCustomersBetween(tenantCode, start60d, start30d));

            snapshot.getTrend().setSalesOrderCountPrevious30d(previousSalesOrders);
            snapshot.getTrend().setProductionOrderCountPrevious30d(previousProductionOrders);
            snapshot.getTrend().setSalesAmountPrevious30d(previousSalesAmount);
            snapshot.getTrend().setBadProductCountPrevious30d(previousBadProductCount);
            snapshot.getTrend().setBadProductLossPrevious30d(previousBadProductLoss);
            snapshot.getTrend().setActiveCustomerCountPrevious30d(previousActiveCustomers);
            snapshot.getTrend().setNewCustomerCountPrevious30d(previousNewCustomers);
            snapshot.getTrend().setSalesOrderGrowthRate(growthRate(snapshot.getOrder().getSalesOrderCount30d(), previousSalesOrders));
            snapshot.getTrend().setSalesAmountGrowthRate(growthRate(snapshot.getOrder().getSalesAmount30d(), previousSalesAmount));
            snapshot.getTrend().setProductionOrderGrowthRate(growthRate(snapshot.getOrder().getProductionOrderCount30d(), previousProductionOrders));
            snapshot.getTrend().setBadProductLossGrowthRate(growthRate(snapshot.getQuality().getBadProductLoss30d(), previousBadProductLoss));
            snapshot.getTrend().setActiveCustomerGrowthRate(growthRate(snapshot.getCustomer().getActiveCustomerCount30d(), previousActiveCustomers));
        } catch (Exception ignored) {
            // 趋势对比属于增强指标，任一历史表结构不完整时降级为当前周期分析，避免影响大盘加载。
        }
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

        DashboardAiAdviceVO customerValue = buildCustomerValueStructureAdvice(snapshot);
        if (customerValue != null) {
            advices.add(customerValue);
        }

        DashboardAiAdviceVO financePressure = buildFinancePressureAdvice(snapshot);
        if (financePressure != null) {
            advices.add(financePressure);
        }

        if (Boolean.TRUE.equals(visibility.getAttendanceVisible())) {
            DashboardAiAdviceVO employeeStability = buildEmployeeStabilityAdvice(snapshot);
            if (employeeStability != null) {
                advices.add(employeeStability);
            }

            DashboardAiAdviceVO organizationGovernance = buildOrganizationGovernanceAdvice(snapshot);
            if (organizationGovernance != null) {
                advices.add(organizationGovernance);
            }
        }
        return advices;
    }

    /**
     * 生成跨维度决策建议。
     *
     * <p>这一层不是单表阈值提醒，而是把订单、库存、客户、员工、质量和财务放在一起看，
     * 用来发现“单个指标不算严重，但组合起来会影响经营结果”的隐性风险。</p>
     */
    private List<DashboardAiAdviceVO> buildCrossDomainDecisionAdvices(AiBusinessSnapshotVO snapshot, DashboardOverviewVO.Visibility visibility) {
        List<DashboardAiAdviceVO> advices = new ArrayList<>();

        if (Boolean.TRUE.equals(visibility.getOrderVisible())) {
            addIfNotNull(advices, buildFulfillmentInventoryCouplingAdvice(snapshot));
            addIfNotNull(advices, buildCoreCustomerDeliveryAdvice(snapshot));
            if (Boolean.TRUE.equals(visibility.getAttendanceVisible())) {
                addIfNotNull(advices, buildEmployeeDeliveryCapacityAdvice(snapshot));
            }
        }

        addIfNotNull(advices, buildQualityFinanceDragAdvice(snapshot));
        addIfNotNull(advices, buildCustomerGrowthPipelineAdvice(snapshot));
        return advices;
    }

    /**
     * 生成趋势型建议。
     *
     * <p>这一层会比较“最近 30 天”和“上一个 30 天”，用于捕捉订单、客户、质量这些指标的方向性变化。
     * 它比单点阈值更早发现问题，适合给老板和管理层做提前干预。</p>
     */
    private List<DashboardAiAdviceVO> buildTrendDecisionAdvices(AiBusinessSnapshotVO snapshot, DashboardOverviewVO.Visibility visibility) {
        List<DashboardAiAdviceVO> advices = new ArrayList<>();

        if (Boolean.TRUE.equals(visibility.getOrderVisible())) {
            addIfNotNull(advices, buildSalesMomentumTrendAdvice(snapshot));
            addIfNotNull(advices, buildProductionSalesRhythmTrendAdvice(snapshot));
        }

        addIfNotNull(advices, buildQualityDeteriorationTrendAdvice(snapshot));
        addIfNotNull(advices, buildCustomerActivityTrendAdvice(snapshot));
        return advices;
    }

    private DashboardAiAdviceVO buildSalesMomentumTrendAdvice(AiBusinessSnapshotVO snapshot) {
        BigDecimal currentAmount = snapshot.getOrder().getSalesAmount30d();
        BigDecimal previousAmount = snapshot.getTrend().getSalesAmountPrevious30d();
        long currentOrders = snapshot.getOrder().getSalesOrderCount30d();
        long previousOrders = snapshot.getTrend().getSalesOrderCountPrevious30d();
        BigDecimal amountGrowth = snapshot.getTrend().getSalesAmountGrowthRate();
        BigDecimal orderGrowth = snapshot.getTrend().getSalesOrderGrowthRate();

        if (previousAmount.compareTo(BigDecimal.ZERO) <= 0 && currentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        if (previousAmount.compareTo(BigDecimal.ZERO) > 0 && amountGrowth.compareTo(new BigDecimal("-20")) <= 0) {
            int riskScore = boundedRiskScore(66 + Math.min(amountGrowth.abs().intValue(), 24));
            DashboardAiAdviceVO advice = baseAdvice("finance", "warning", "trending_down");
            advice.setTitle("销售动能下滑预警");
            advice.setSummary(String.format(
                    "最近 30 天销售额约 %s 元，上一个 30 天约 %s 元，环比下降 %s%%；订单数从 %d 单变为 %d 单。",
                    formatNumber(currentAmount),
                    formatNumber(previousAmount),
                    formatNumber(amountGrowth.abs()),
                    previousOrders,
                    currentOrders
            ));
            advice.setSuggestion("建议销售负责人先核对重点客户是否有复购延后、报价未跟进或交付体验导致的下单放缓；同时把近 30 天未成交但有沟通记录的客户拉成清单，判断是淡季波动、价格竞争还是客户流失苗头。");
            advice.setReasoning(String.format(
                    "趋势快照显示：销售额环比=%s%%，订单数环比=%s%%，当前销售额=%s，上一周期销售额=%s。该建议由销售金额与订单数量双指标趋势共同判断生成。",
                    formatNumber(amountGrowth),
                    formatNumber(orderGrowth),
                    formatNumber(currentAmount),
                    formatNumber(previousAmount)
            ));
            advice.setRoute("/function/order");
            enrichDecisionMetadata(
                    advice,
                    "趋势预警",
                    riskScore,
                    "可能影响未来订单来源、现金流节奏和生产排程稳定性",
                    "本周内完成原因拆解",
                    "拉出最近 30 天销售订单和重点客户跟进记录，标记下滑来源。",
                    "销售额环比、订单数环比、重点客户复购率、报价转化率"
            );
            if (riskScore >= 88) {
                advice.setPriority("P0");
            }
            return advice;
        }

        if (currentAmount.compareTo(BigDecimal.ZERO) > 0 && amountGrowth.compareTo(new BigDecimal("30")) >= 0) {
            DashboardAiAdviceVO advice = baseAdvice("order", "info", "trending_up");
            advice.setTitle("销售增长动能增强");
            advice.setSummary(String.format(
                    "最近 30 天销售额约 %s 元，较上一周期增长 %s%%；订单数环比变化 %s%%，需要确认产能和库存是否能承接增长。",
                    formatNumber(currentAmount),
                    formatNumber(amountGrowth),
                    formatNumber(orderGrowth)
            ));
            advice.setSuggestion("建议管理层不要只看增长结果，而是同步检查库存水位、生产排程和核心客户交付承诺，避免订单增长后在仓库或生产环节形成新的瓶颈。");
            advice.setReasoning(String.format(
                    "趋势快照显示：当前销售额=%s，上一周期销售额=%s，销售额环比=%s%%，订单数环比=%s%%。",
                    formatNumber(currentAmount),
                    formatNumber(previousAmount),
                    formatNumber(amountGrowth),
                    formatNumber(orderGrowth)
            ));
            advice.setRoute("/function/order");
            enrichDecisionMetadata(
                    advice,
                    "增长信号",
                    boundedRiskScore(48 + Math.min(amountGrowth.intValue() / 2, 28)),
                    "增长会带来产能、库存和交付承诺的同步压力",
                    "本周内确认承接能力",
                    "把新增订单按客户、型号和交付日期拆分，确认是否需要调整排产优先级。",
                    "新增订单承接率、准时发货率、低库存型号变化"
            );
            return advice;
        }

        return null;
    }

    private DashboardAiAdviceVO buildProductionSalesRhythmTrendAdvice(AiBusinessSnapshotVO snapshot) {
        BigDecimal salesGrowth = snapshot.getTrend().getSalesOrderGrowthRate();
        BigDecimal productionGrowth = snapshot.getTrend().getProductionOrderGrowthRate();
        long currentSalesOrders = snapshot.getOrder().getSalesOrderCount30d();
        long currentProductionOrders = snapshot.getOrder().getProductionOrderCount30d();
        long previousProductionOrders = snapshot.getTrend().getProductionOrderCountPrevious30d();

        if (currentSalesOrders <= 0 && currentProductionOrders <= 0) {
            return null;
        }

        if (salesGrowth.compareTo(new BigDecimal("20")) >= 0 && productionGrowth.compareTo(BigDecimal.ZERO) < 0) {
            int riskScore = boundedRiskScore(70 + Math.min(salesGrowth.intValue() / 3, 16) + Math.min(productionGrowth.abs().intValue() / 3, 12));
            DashboardAiAdviceVO advice = baseAdvice("operation", "warning", "sync_problem");
            advice.setTitle("销售增长与生产承接错配");
            advice.setSummary(String.format(
                    "销售订单数环比增长 %s%%，但生产订单数环比下降 %s%%；当前 30 天销售订单 %d 单，生产订单 %d 单。",
                    formatNumber(salesGrowth),
                    formatNumber(productionGrowth.abs()),
                    currentSalesOrders,
                    currentProductionOrders
            ));
            advice.setSuggestion("建议生产负责人和销售负责人共同核对新增销售订单是否已经同步创建生产单，重点检查交付日期临近但未进入生产中的订单，避免订单增长只停留在销售端，生产端没有及时承接。");
            advice.setReasoning(String.format(
                    "趋势快照显示：销售订单环比=%s%%，生产订单环比=%s%%，上一周期生产订单=%d，当前生产订单=%d。",
                    formatNumber(salesGrowth),
                    formatNumber(productionGrowth),
                    previousProductionOrders,
                    currentProductionOrders
            ));
            advice.setRoute("/function/order");
            enrichDecisionMetadata(
                    advice,
                    "节奏错配",
                    riskScore,
                    "可能造成销售承诺已经形成，但生产排程没有同步跟上的履约风险",
                    "今日内完成订单-生产单核对",
                    "导出销售订单与生产订单关联关系，优先确认交付日期最近的订单。",
                    "销售单生产单关联率、临期未排产订单数、生产订单创建滞后天数"
            );
            return advice;
        }

        if (salesGrowth.compareTo(new BigDecimal("-25")) <= 0 && productionGrowth.compareTo(new BigDecimal("20")) >= 0) {
            DashboardAiAdviceVO advice = baseAdvice("operation", "info", "inventory");
            advice.setTitle("生产节奏可能快于销售消化");
            advice.setSummary(String.format(
                    "销售订单数环比下降 %s%%，生产订单数环比增长 %s%%，需要关注是否形成过多在制品或库存压力。",
                    formatNumber(salesGrowth.abs()),
                    formatNumber(productionGrowth)
            ));
            advice.setSuggestion("建议生产负责人复核当前排产是否来自已确认订单，如果部分生产为备货，应同步仓库评估库存占用和热销型号匹配度，避免生产节奏与销售消化节奏脱节。");
            advice.setReasoning(String.format(
                    "趋势快照显示：销售订单环比=%s%%，生产订单环比=%s%%，当前销售订单=%d，当前生产订单=%d。",
                    formatNumber(salesGrowth),
                    formatNumber(productionGrowth),
                    currentSalesOrders,
                    currentProductionOrders
            ));
            advice.setRoute("/function/order");
            enrichDecisionMetadata(
                    advice,
                    "节奏校准",
                    boundedRiskScore(60 + Math.min(productionGrowth.intValue() / 3, 20)),
                    "可能增加库存占用、在制品堆积和资金周转压力",
                    "本周内完成排产复盘",
                    "区分订单生产和备货生产，确认备货型号是否有明确销售消化路径。",
                    "备货订单占比、在制品数量、库存周转天数"
            );
            return advice;
        }

        return null;
    }

    private DashboardAiAdviceVO buildQualityDeteriorationTrendAdvice(AiBusinessSnapshotVO snapshot) {
        long currentBadCount = snapshot.getQuality().getBadProductCount30d();
        long previousBadCount = snapshot.getTrend().getBadProductCountPrevious30d();
        BigDecimal currentLoss = snapshot.getQuality().getBadProductLoss30d();
        BigDecimal previousLoss = snapshot.getTrend().getBadProductLossPrevious30d();
        BigDecimal lossGrowth = snapshot.getTrend().getBadProductLossGrowthRate();

        boolean newLossSignal = previousLoss.compareTo(BigDecimal.ZERO) <= 0
                && currentLoss.compareTo(BigDecimal.ZERO) > 0
                && currentBadCount >= 2;
        boolean deteriorationSignal = previousLoss.compareTo(BigDecimal.ZERO) > 0
                && lossGrowth.compareTo(new BigDecimal("50")) >= 0
                && currentLoss.compareTo(BigDecimal.ZERO) > 0;
        if (!newLossSignal && !deteriorationSignal) {
            return null;
        }

        int riskScore = boundedRiskScore(72 + Math.min(lossGrowth.max(BigDecimal.ZERO).intValue() / 3, 20) + (currentBadCount >= previousBadCount + 3 ? 6 : 0));
        DashboardAiAdviceVO advice = baseAdvice("quality", "warning", "report_problem");
        advice.setTitle("质量损耗趋势恶化");
        advice.setSummary(String.format(
                "最近 30 天次品记录 %d 条、损失约 %s 元；上一周期为 %d 条、损失约 %s 元，损失金额环比 %s%%。",
                currentBadCount,
                formatNumber(currentLoss),
                previousBadCount,
                formatNumber(previousLoss),
                formatNumber(lossGrowth)
        ));
        advice.setSuggestion("建议质检负责人把最近 30 天次品按类型、订单、责任环节和处理结果拆分，优先判断是否集中在同一型号、同一供应商、同一工序或同一班组；如果集中度高，应立即形成专项整改，而不是继续按单处理。");
        advice.setReasoning(String.format(
                "趋势快照显示：当前次品=%d，上一周期次品=%d，当前损失=%s，上一周期损失=%s，损失环比=%s%%。",
                currentBadCount,
                previousBadCount,
                formatNumber(currentLoss),
                formatNumber(previousLoss),
                formatNumber(lossGrowth)
        ));
        advice.setRoute("/function/bad-product");
        enrichDecisionMetadata(
                advice,
                "异常放大",
                riskScore,
                "可能影响毛利、返工成本、客户满意度和后续交付稳定性",
                "今日内定位主要损耗来源",
                "按次品类型和订单来源导出明细，找出损失金额最高的前三类问题。",
                "次品损失环比、重复问题次数、整改后复发率"
        );
        if (riskScore >= 90) {
            advice.setPriority("P0");
        }
        return advice;
    }

    private DashboardAiAdviceVO buildCustomerActivityTrendAdvice(AiBusinessSnapshotVO snapshot) {
        long currentActive = snapshot.getCustomer().getActiveCustomerCount30d();
        long previousActive = snapshot.getTrend().getActiveCustomerCountPrevious30d();
        long currentNew = snapshot.getCustomer().getNewCustomerCount30d();
        long previousNew = snapshot.getTrend().getNewCustomerCountPrevious30d();
        BigDecimal activeGrowth = snapshot.getTrend().getActiveCustomerGrowthRate();

        if (snapshot.getCustomer().getCustomerCount() <= 0 || previousActive <= 0) {
            return null;
        }

        if (activeGrowth.compareTo(new BigDecimal("-20")) <= 0) {
            int riskScore = boundedRiskScore(64 + Math.min(activeGrowth.abs().intValue(), 26));
            DashboardAiAdviceVO advice = baseAdvice("customer", "warning", "person_search");
            advice.setTitle("客户活跃度下行预警");
            advice.setSummary(String.format(
                    "最近 30 天活跃客户 %d 个，上一周期 %d 个，环比下降 %s%%；新增客户从 %d 个变为 %d 个。",
                    currentActive,
                    previousActive,
                    formatNumber(activeGrowth.abs()),
                    previousNew,
                    currentNew
            ));
            advice.setSuggestion("建议销售负责人先区分老客户复购减少和新客户开发放缓两类原因：对上一周期活跃但本周期未下单的客户安排回访，对有报价但未成交的客户补齐未成交原因，避免客户活跃度下行只在月底销售额里才暴露。");
            advice.setReasoning(String.format(
                    "趋势快照显示：当前活跃客户=%d，上一周期活跃客户=%d，活跃客户环比=%s%%，当前新增客户=%d，上一周期新增客户=%d。",
                    currentActive,
                    previousActive,
                    formatNumber(activeGrowth),
                    currentNew,
                    previousNew
            ));
            advice.setRoute("/function/customer");
            enrichDecisionMetadata(
                    advice,
                    "客户风险",
                    riskScore,
                    "可能影响复购稳定性、销售预测准确性和未来订单池厚度",
                    "本周内完成客户分层回访",
                    "拉出上一周期活跃但本周期未下单客户，逐个标注未复购原因。",
                    "活跃客户环比、客户回访完成率、报价转化率、沉睡客户唤醒数"
            );
            return advice;
        }

        if (currentNew == 0 && previousNew > 0) {
            DashboardAiAdviceVO advice = baseAdvice("customer", "info", "playlist_add_check");
            advice.setTitle("新增客户线索出现断层");
            advice.setSummary(String.format(
                    "上一周期新增客户 %d 个，最近 30 天暂无新增客户；当前活跃客户 %d 个，客户池需要持续补充。",
                    previousNew,
                    currentActive
            ));
            advice.setSuggestion("建议销售团队把线索来源、拜访计划和报价反馈维护到客户管理中，让系统后续能判断是客户开发节奏下降，还是只存在登记不及时的问题。");
            advice.setReasoning(String.format(
                    "趋势快照显示：当前新增客户=%d，上一周期新增客户=%d，当前活跃客户=%d。",
                    currentNew,
                    previousNew,
                    currentActive
            ));
            advice.setRoute("/function/customer");
            enrichDecisionMetadata(
                    advice,
                    "增长断层",
                    62,
                    "可能影响未来订单池、客户结构健康度和销售增长连续性",
                    "本周内补齐线索登记",
                    "把近期沟通过但未建档的客户补录，并标记下一步跟进动作。",
                    "新增客户数、线索录入数、线索转客户率"
            );
            return advice;
        }

        return null;
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

    private DashboardAiAdviceVO buildFulfillmentInventoryCouplingAdvice(AiBusinessSnapshotVO snapshot) {
        long dueSoon = snapshot.getOrder().getUnshippedDueSoonCount();
        long lowStockModelCount = snapshot.getInventory().getLowStockModelCount();
        if (dueSoon <= 0 || lowStockModelCount <= 0) {
            return null;
        }

        int riskScore = boundedRiskScore(68 + (int) Math.min(dueSoon * 5, 18) + (int) Math.min(lowStockModelCount * 4, 18));
        DashboardAiAdviceVO advice = baseAdvice("operation", "warning", "hub");
        advice.setTitle("交付与库存联动风险");
        advice.setSummary(String.format(
                "未来 3 天仍有 %d 张销售订单未完成发货，同时 %d 个布匹型号低于安全水位，存在交付风险和临时调货压力叠加。",
                dueSoon,
                lowStockModelCount
        ));
        advice.setSuggestion("建议把临近交付订单与低库存型号做一次交叉核对，优先确认是否存在同型号缺料、替代物料、部分发货或补货时效问题；如果交付风险集中在重点客户订单上，应提前由销售同步客户预期。");
        advice.setReasoning(String.format(
                "经营快照显示：临近交付未发货=%d，低库存型号=%d，生产中订单=%d，库存总量=%s 米。该建议不是单点库存预警，而是把交付时限和库存安全水位做联动判断。",
                dueSoon,
                lowStockModelCount,
                snapshot.getOrder().getProducingCount(),
                formatNumber(snapshot.getInventory().getTotalMeters())
        ));
        advice.setRoute("/function/order");
        enrichDecisionMetadata(
                advice,
                "跨域风险",
                riskScore,
                "可能影响订单交付、临时采购成本和客户体验",
                "今日内完成排查",
                "导出未来 3 天未发货订单，并与低库存型号逐项交叉核对。",
                "临期未发货订单数、低库存型号数、替代/补货方案完成率"
        );
        if (riskScore >= 90) {
            advice.setPriority("P0");
        }
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

    private DashboardAiAdviceVO buildCustomerValueStructureAdvice(AiBusinessSnapshotVO snapshot) {
        BigDecimal salesAmount = snapshot.getOrder().getSalesAmount30d();
        BigDecimal topAmount = snapshot.getCustomer().getTopCustomerAmount30d();
        String topCustomerName = snapshot.getCustomer().getTopCustomerName30d();
        if (salesAmount.compareTo(BigDecimal.ZERO) <= 0
                || topAmount.compareTo(BigDecimal.ZERO) <= 0
                || isBlank(topCustomerName)) {
            return null;
        }

        BigDecimal concentrationRatio = topAmount
                .multiply(new BigDecimal("100"))
                .divide(salesAmount, 2, RoundingMode.HALF_UP);
        if (concentrationRatio.compareTo(new BigDecimal("45")) < 0) {
            return null;
        }

        DashboardAiAdviceVO advice = baseAdvice(
                "customer",
                concentrationRatio.compareTo(new BigDecimal("60")) >= 0 ? "warning" : "info",
                "diversity_3"
        );
        advice.setTitle("核心客户集中度洞察");
        advice.setSummary(String.format(
                "近 30 天销售额约 ￥%s，其中客户 %s 贡献约 ￥%s，占比约 %s%%。",
                formatNumber(salesAmount),
                topCustomerName,
                formatNumber(topAmount),
                formatNumber(concentrationRatio)
        ));
        advice.setSuggestion("建议销售负责人把核心客户维护和新客户开拓分开跟进：一方面确认该客户后续项目节奏、回款与交付满意度，另一方面推动至少 2 到 3 个潜力客户进入稳定复购，降低单一客户波动对产能和现金流的影响。");
        advice.setReasoning(String.format(
                "经营快照显示：30天销售额=￥%s，最大客户贡献=￥%s，贡献占比=%s%%，30天活跃客户=%d，新客户=%d。该建议由客户销售集中度与活跃客户结构共同判断生成。",
                formatNumber(salesAmount),
                formatNumber(topAmount),
                formatNumber(concentrationRatio),
                snapshot.getCustomer().getActiveCustomerCount30d(),
                snapshot.getCustomer().getNewCustomerCount30d()
        ));
        advice.setRoute("/function/customer");
        return advice;
    }

    private DashboardAiAdviceVO buildCoreCustomerDeliveryAdvice(AiBusinessSnapshotVO snapshot) {
        long dueSoon = snapshot.getOrder().getUnshippedDueSoonCount();
        BigDecimal salesAmount = snapshot.getOrder().getSalesAmount30d();
        BigDecimal topAmount = snapshot.getCustomer().getTopCustomerAmount30d();
        String topCustomerName = snapshot.getCustomer().getTopCustomerName30d();
        if (dueSoon <= 0
                || salesAmount.compareTo(BigDecimal.ZERO) <= 0
                || topAmount.compareTo(BigDecimal.ZERO) <= 0
                || isBlank(topCustomerName)) {
            return null;
        }

        BigDecimal concentrationRatio = topAmount
                .multiply(new BigDecimal("100"))
                .divide(salesAmount, 2, RoundingMode.HALF_UP);
        if (concentrationRatio.compareTo(new BigDecimal("40")) < 0) {
            return null;
        }

        int riskScore = boundedRiskScore(62 + concentrationRatio.intValue() / 2 + (int) Math.min(dueSoon * 4, 16));
        DashboardAiAdviceVO advice = baseAdvice("customer", riskScore >= 80 ? "warning" : "info", "shield_person");
        advice.setTitle("核心客户交付影响面评估");
        advice.setSummary(String.format(
                "客户 %s 近 30 天贡献销售额占比约 %s%%，当前又存在 %d 张临近交付未发货订单，需要防止交付风险传导到核心客户关系。",
                topCustomerName,
                formatNumber(concentrationRatio),
                dueSoon
        ));
        advice.setSuggestion("建议销售负责人把核心客户订单单独拉清单，确认交付日期、生产进度、出库准备和客户沟通口径；如果确实存在延期可能，应在客户追问前主动给出替代方案或分批交付方案。");
        advice.setReasoning(String.format(
                "经营快照显示：核心客户=%s，核心客户30天销售额=￥%s，销售占比=%s%%，临近交付未发货=%d。该建议由客户价值集中度和交付风险叠加生成。",
                topCustomerName,
                formatNumber(topAmount),
                formatNumber(concentrationRatio),
                dueSoon
        ));
        advice.setRoute("/function/order");
        enrichDecisionMetadata(
                advice,
                "客户风控",
                riskScore,
                "可能影响重点客户满意度、复购节奏和销售稳定性",
                "今日内确认客户沟通口径",
                "筛出核心客户相关订单，逐单确认生产、出库和预计发货时间。",
                "核心客户临期订单完成率、延期沟通提前量、客户复购变化"
        );
        return advice;
    }

    private DashboardAiAdviceVO buildEmployeeStabilityAdvice(AiBusinessSnapshotVO snapshot) {
        long activeEmployeeCount = snapshot.getEmployee().getActiveEmployeeCount();
        if (activeEmployeeCount <= 0) {
            return null;
        }

        long attendanceExceptionCount = snapshot.getEmployee().getAttendanceExceptionCountToday();
        long lateCount = snapshot.getEmployee().getLateCountToday();
        if (attendanceExceptionCount <= 0 && lateCount <= 0) {
            DashboardAiAdviceVO advice = baseAdvice("employee", "success", "groups");
            advice.setTitle("员工出勤节奏稳定");
            advice.setSummary(String.format("当前在职员工 %d 人，今日暂未发现明显考勤异常。", activeEmployeeCount));
            advice.setSuggestion("建议继续保持考勤规则与请假审批同步，重点观察连续异常、跨部门调班和外勤打卡场景，避免规则配置和真实生产节奏脱节。");
            advice.setReasoning(String.format("经营快照显示：在职员工=%d，今日考勤异常=%d，迟到=%d。该建议由员工状态与考勤规则执行结果生成。", activeEmployeeCount, attendanceExceptionCount, lateCount));
            advice.setRoute("/function/attendance");
            return advice;
        }

        BigDecimal exceptionRatio = BigDecimal.valueOf(attendanceExceptionCount)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(activeEmployeeCount), 2, RoundingMode.HALF_UP);
        DashboardAiAdviceVO advice = baseAdvice(
                "employee",
                attendanceExceptionCount >= 3 || exceptionRatio.compareTo(new BigDecimal("10")) >= 0 ? "warning" : "info",
                "badge"
        );
        advice.setTitle("员工出勤异常研判");
        advice.setSummary(String.format(
                "当前在职员工 %d 人，今日考勤异常 %d 人次，其中迟到 %d 人次，异常占比约 %s%%。",
                activeEmployeeCount,
                attendanceExceptionCount,
                lateCount,
                formatNumber(exceptionRatio)
        ));
        advice.setSuggestion("建议人事先区分真实迟到、请假未同步、设备定位异常和外勤场景；部门负责人同步确认生产班次是否临时调整，避免把规则配置问题误判为员工纪律问题。");
        advice.setReasoning(String.format(
                "经营快照显示：在职员工=%d，今日考勤异常=%d，迟到=%d，异常率=%s%%。该建议由考勤异常比例和员工规模共同判断生成。",
                activeEmployeeCount,
                attendanceExceptionCount,
                lateCount,
                formatNumber(exceptionRatio)
        ));
        advice.setRoute("/function/attendance");
        return advice;
    }

    private DashboardAiAdviceVO buildEmployeeDeliveryCapacityAdvice(AiBusinessSnapshotVO snapshot) {
        long dueSoon = snapshot.getOrder().getUnshippedDueSoonCount();
        long activeEmployeeCount = snapshot.getEmployee().getActiveEmployeeCount();
        long attendanceExceptionCount = snapshot.getEmployee().getAttendanceExceptionCountToday();
        if (dueSoon <= 0 || activeEmployeeCount <= 0 || attendanceExceptionCount <= 0) {
            return null;
        }

        BigDecimal exceptionRatio = BigDecimal.valueOf(attendanceExceptionCount)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(activeEmployeeCount), 2, RoundingMode.HALF_UP);
        if (exceptionRatio.compareTo(new BigDecimal("5")) < 0 && dueSoon < 3) {
            return null;
        }

        int riskScore = boundedRiskScore(58 + exceptionRatio.intValue() + (int) Math.min(dueSoon * 5, 20));
        DashboardAiAdviceVO advice = baseAdvice("employee", riskScore >= 80 ? "warning" : "info", "engineering");
        advice.setTitle("人员出勤与交付承接能力");
        advice.setSummary(String.format(
                "今日考勤异常 %d 人次，占在职员工约 %s%%；同时未来 3 天有 %d 张销售订单尚未发货，人员波动可能放大交付压力。",
                attendanceExceptionCount,
                formatNumber(exceptionRatio),
                dueSoon
        ));
        advice.setSuggestion("建议人事和生产负责人先确认异常人员是否属于仓库、生产或质检关键岗位；如关键岗位缺口明显，应安排班组调配、临时替岗或调整出库优先级，避免订单已经具备发货条件但卡在人手安排上。");
        advice.setReasoning(String.format(
                "经营快照显示：在职员工=%d，考勤异常=%d，异常率=%s%%，临近未发货=%d。该建议由人员稳定性和订单交付压力交叉生成。",
                activeEmployeeCount,
                attendanceExceptionCount,
                formatNumber(exceptionRatio),
                dueSoon
        ));
        advice.setRoute("/function/attendance");
        enrichDecisionMetadata(
                advice,
                "产能风险",
                riskScore,
                "可能影响生产排班、仓库发货和订单准时率",
                "今日早会前确认",
                "按岗位查看今日异常人员，优先确认生产、质检、仓库岗位是否缺口。",
                "关键岗位到岗率、临期订单发货率、异常原因闭环率"
        );
        return advice;
    }

    private DashboardAiAdviceVO buildOrganizationGovernanceAdvice(AiBusinessSnapshotVO snapshot) {
        long activeEmployeeCount = snapshot.getEmployee().getActiveEmployeeCount();
        long missingManagerCount = snapshot.getEmployee().getMissingManagerCount();
        long pendingLeaveCount = snapshot.getEmployee().getPendingLeaveApprovalCount();
        if (activeEmployeeCount <= 0 || (missingManagerCount <= 0 && pendingLeaveCount <= 0)) {
            return null;
        }

        long warningThreshold = Math.max(3, Math.round(Math.ceil(activeEmployeeCount * 0.2D)));
        DashboardAiAdviceVO advice = baseAdvice(
                "employee",
                missingManagerCount >= warningThreshold || pendingLeaveCount >= 3 ? "warning" : "info",
                "account_tree"
        );
        advice.setTitle("组织责任链路完整性");
        advice.setSummary(String.format(
                "当前在职员工 %d 人，仍有 %d 人缺少直属上级配置，待处理请假审批 %d 条。",
                activeEmployeeCount,
                missingManagerCount,
                pendingLeaveCount
        ));
        advice.setSuggestion("建议优先补齐员工直属上级关系，并把请假、考勤异常、订单跟进等事项绑定到部门负责人；这样后续 AI 在识别异常时，才能直接给出可落地的责任链路，而不是只停留在系统提示。");
        advice.setReasoning(String.format(
                "经营快照显示：在职员工=%d，缺少直属上级=%d，待审批请假=%d，30天请假申请=%d。该建议由组织架构完整度与审批积压共同判断生成。",
                activeEmployeeCount,
                missingManagerCount,
                pendingLeaveCount,
                snapshot.getEmployee().getLeaveRequestCount30d()
        ));
        advice.setRoute(missingManagerCount > 0 ? "/function/employee" : "/function/approval");
        return advice;
    }

    private DashboardAiAdviceVO buildQualityFinanceDragAdvice(AiBusinessSnapshotVO snapshot) {
        BigDecimal lossAmount = snapshot.getQuality().getBadProductLoss30d();
        BigDecimal salesAmount = snapshot.getOrder().getSalesAmount30d();
        BigDecimal pendingFinanceAmount = snapshot.getFinance().getPendingFinanceAmount();
        long badCount = snapshot.getQuality().getBadProductCount30d();
        if (lossAmount.compareTo(BigDecimal.ZERO) <= 0 || badCount <= 0) {
            return null;
        }

        BigDecimal lossRatio = salesAmount.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO
                : lossAmount.multiply(new BigDecimal("100")).divide(salesAmount, 2, RoundingMode.HALF_UP);
        if (lossRatio.compareTo(new BigDecimal("2")) < 0 && pendingFinanceAmount.compareTo(new BigDecimal("5000")) < 0) {
            return null;
        }

        int riskScore = boundedRiskScore(55 + lossRatio.intValue() * 8 + (pendingFinanceAmount.compareTo(new BigDecimal("10000")) >= 0 ? 12 : 0));
        DashboardAiAdviceVO advice = baseAdvice("finance", riskScore >= 80 ? "warning" : "info", "account_balance_wallet");
        advice.setTitle("质量损耗对经营利润的拖拽");
        advice.setSummary(String.format(
                "近 30 天次品损失约 ￥%s，按同期销售额测算损耗占比约 %s%%；当前财务待审批金额约 ￥%s。",
                formatNumber(lossAmount),
                formatNumber(lossRatio),
                formatNumber(pendingFinanceAmount)
        ));
        advice.setSuggestion("建议把次品损耗从“质检问题”上升到“经营成本问题”处理：财务负责量化损失，质检定位高发原因，生产复盘工艺和责任归属；若同类问题持续出现，应对对应供应商、工序或班组建立专项改进。");
        advice.setReasoning(String.format(
                "经营快照显示：次品记录=%d，次品损失=￥%s，30天销售额=￥%s，损耗占比=%s%%，待审批金额=￥%s。该建议由质量成本和财务审批压力交叉生成。",
                badCount,
                formatNumber(lossAmount),
                formatNumber(salesAmount),
                formatNumber(lossRatio),
                formatNumber(pendingFinanceAmount)
        ));
        advice.setRoute("/function/bad-product");
        enrichDecisionMetadata(
                advice,
                "利润风险",
                riskScore,
                "可能影响毛利、返工成本、客户赔付和供应商结算",
                "本周内形成专项复盘",
                "按次品类型、订单来源和责任环节导出明细，确认最高损耗来源。",
                "次品损耗率、重复问题次数、整改后同类问题下降比例"
        );
        return advice;
    }

    private DashboardAiAdviceVO buildCustomerGrowthPipelineAdvice(AiBusinessSnapshotVO snapshot) {
        long customerCount = snapshot.getCustomer().getCustomerCount();
        long active90d = snapshot.getCustomer().getActiveCustomerCount90d();
        long new30d = snapshot.getCustomer().getNewCustomerCount30d();
        if (customerCount <= 0 || new30d > 0) {
            return null;
        }

        BigDecimal activeRatio = BigDecimal.valueOf(active90d)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(customerCount), 2, RoundingMode.HALF_UP);
        if (activeRatio.compareTo(new BigDecimal("70")) >= 0) {
            return null;
        }

        int riskScore = boundedRiskScore(52 + Math.max(new BigDecimal("70").subtract(activeRatio).intValue(), 0));
        DashboardAiAdviceVO advice = baseAdvice("customer", "info", "person_add");
        advice.setTitle("客户增长管道需要补强");
        advice.setSummary(String.format(
                "近 30 天暂无新增客户，当前客户 90 天活跃率约 %s%%，客户增长和复购两端都需要持续跟进。",
                formatNumber(activeRatio)
        ));
        advice.setSuggestion("建议销售负责人把客户池拆成老客户复购、沉睡客户唤醒和新客户开发三张清单，每周至少固定跟进一批高潜客户，并把拜访结果、报价反馈和下一步动作维护到客户管理里，为后续 AI 判断客户流失和增长机会提供数据。");
        advice.setReasoning(String.format(
                "经营快照显示：客户总数=%d，90天活跃客户=%d，30天新增客户=%d，活跃率=%s%%。该建议由客户增长速度与存量客户活跃度共同判断生成。",
                customerCount,
                active90d,
                new30d,
                formatNumber(activeRatio)
        ));
        advice.setRoute("/function/customer");
        enrichDecisionMetadata(
                advice,
                "增长机会",
                riskScore,
                "可能影响未来订单来源、销售稳定性和客户结构健康度",
                "本周内建立客户跟进清单",
                "从沉睡客户和潜在客户中选出首批 10 个对象，补齐跟进状态。",
                "新增客户数、沉睡客户唤醒数、客户跟进完成率"
        );
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
        advice.setTitle("全局运营节奏");
        advice.setSummary("库存预警、订单履约、客户活跃、员工考勤、审批和打印任务已经覆盖管理层日常需要关注的关键节点。");
        advice.setSuggestion("建议将这些指标纳入每日早会检查项，形成“发现异常、明确责任、跟进处理、复盘结果”的管理闭环；数据稳定后再接入大模型生成覆盖经营、客户和员工的管理日报。");
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
        advice.setSummary("当前未识别出明显的库存、交付、客户复购、员工出勤或次品异常波动。");
        advice.setSuggestion("建议继续保持日常巡检节奏，重点观察未来 7 天订单交付、库存消耗、客户复购、员工考勤和次品损耗变化，必要时再触发专项复盘。");
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
        advice.setDecisionType(resolveDecisionType(category, level));
        advice.setRiskScore(resolveRiskScore(level));
        advice.setImpactText(resolveImpactText(category));
        advice.setTimeWindow(resolveTimeWindow(level));
        advice.setFirstAction(resolveFirstAction(category));
        advice.setReviewMetric(resolveReviewMetric(category));
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
            case "employee" -> "人事 / 部门负责人";
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
            case "employee" -> "查看员工/考勤";
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
            case "customer" -> "客户复购周期、活跃客户、核心客户贡献";
            case "employee" -> "员工状态、考勤异常、请假审批、上下级关系";
            case "quality" -> "近 30 天次品数量与损失金额";
            case "finance" -> "订单金额、损耗金额、审批事项";
            case "operation" -> "库存、订单、客户、员工、审批、打印任务联动";
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
            case "employee" -> "建议纳入人事周报和部门晨会，持续沉淀考勤异常与组织责任链路。";
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

    private String resolveDecisionType(String category, String level) {
        if ("warning".equals(level)) {
            return "风险预警";
        }
        return switch (category) {
            case "customer" -> "增长机会";
            case "employee" -> "组织效率";
            case "quality", "finance" -> "成本治理";
            case "operation" -> "运营治理";
            default -> "决策辅助";
        };
    }

    private Integer resolveRiskScore(String level) {
        if ("warning".equals(level)) {
            return 80;
        }
        if ("success".equals(level)) {
            return 25;
        }
        return 55;
    }

    private String resolveImpactText(String category) {
        return switch (category) {
            case "inventory" -> "影响库存可用性、补货节奏和交付准备";
            case "order", "delivery" -> "影响订单准时率、客户体验和生产排程";
            case "customer" -> "影响客户复购、销售稳定性和增长来源";
            case "employee" -> "影响人员稳定、岗位承接和组织执行效率";
            case "quality" -> "影响质量成本、返工成本和客户满意度";
            case "finance" -> "影响现金流、成本控制和审批周转";
            case "operation" -> "影响跨部门协同和管理闭环效率";
            default -> "影响管理层对业务状态的判断";
        };
    }

    private String resolveTimeWindow(String level) {
        if ("warning".equals(level)) {
            return "今日内处理";
        }
        if ("success".equals(level)) {
            return "持续观察";
        }
        return "本周内跟进";
    }

    private String resolveFirstAction(String category) {
        return switch (category) {
            case "inventory" -> "先核对异常型号的实际库存和近 7 天出库速度。";
            case "order", "delivery" -> "先拉出临近交付订单，逐单确认生产、出库和物流状态。";
            case "customer" -> "先筛出核心客户和沉睡客户，确认最近一次跟进记录。";
            case "employee" -> "先核对今日考勤异常和直属上级缺失名单。";
            case "quality" -> "先按次品类型和订单来源导出损耗明细。";
            case "finance" -> "先按金额从高到低处理待审批事项。";
            default -> "先明确责任人、截止时间和复盘指标。";
        };
    }

    private String resolveReviewMetric(String category) {
        return switch (category) {
            case "inventory" -> "低库存型号数、补货完成率、缺料导致的延期次数";
            case "order", "delivery" -> "临期未发货订单数、准时发货率、延期提前沟通率";
            case "customer" -> "客户活跃率、复购间隔、新增客户数、重点客户跟进完成率";
            case "employee" -> "考勤异常率、关键岗位到岗率、直属上级完整率";
            case "quality" -> "次品损耗率、重复问题次数、整改后复发率";
            case "finance" -> "审批周转时长、待审批金额、成本异常处理率";
            default -> "异常关闭率、复盘完成率、同类问题复发率";
        };
    }

    private String resolveRoute(String category) {
        return switch (category) {
            case "inventory" -> "/function/inventory";
            case "order", "delivery" -> "/function/order";
            case "customer" -> "/function/customer";
            case "employee" -> "/function/employee";
            case "quality" -> "/function/bad-product";
            case "finance" -> "/function/approval";
            default -> "/dashboard";
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void addIfNotNull(List<DashboardAiAdviceVO> advices, DashboardAiAdviceVO advice) {
        if (advice != null) {
            advices.add(advice);
        }
    }

    private void enrichDecisionMetadata(DashboardAiAdviceVO advice,
                                        String decisionType,
                                        int riskScore,
                                        String impactText,
                                        String timeWindow,
                                        String firstAction,
                                        String reviewMetric) {
        advice.setDecisionType(decisionType);
        advice.setRiskScore(boundedRiskScore(riskScore));
        advice.setImpactText(impactText);
        advice.setTimeWindow(timeWindow);
        advice.setFirstAction(firstAction);
        advice.setReviewMetric(reviewMetric);
        if (riskScore >= 85 && !"success".equals(advice.getLevel())) {
            advice.setPriority("P1");
            advice.setConfidence(Math.max(advice.getConfidence() == null ? 0 : advice.getConfidence(), 90));
        }
    }

    private int boundedRiskScore(int riskScore) {
        return Math.max(0, Math.min(riskScore, 100));
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

    private int nvlInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal growthRate(long current, long previous) {
        return growthRate(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }

    private BigDecimal growthRate(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = current == null ? BigDecimal.ZERO : current;
        BigDecimal safePrevious = previous == null ? BigDecimal.ZERO : previous;
        if (safePrevious.compareTo(BigDecimal.ZERO) <= 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("100.00") : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return safeCurrent.subtract(safePrevious)
                .multiply(new BigDecimal("100"))
                .divide(safePrevious, 2, RoundingMode.HALF_UP);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ignored) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private record CustomerFollowUpCandidate(String customerName, int orderCount, long lastOrderDays, long avgCycleDays) {
    }
}
