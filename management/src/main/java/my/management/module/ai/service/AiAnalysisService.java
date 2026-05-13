package my.management.module.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import my.management.module.ai.mapper.AiAdviceTrainingSampleMapper;
import my.management.module.behavior.mapper.BehaviorEventMapper;
import my.management.module.behavior.model.vo.BehaviorModulePreferenceVO;
import my.management.module.ai.mapper.AiAnalysisMapper;
import my.management.module.ai.model.entity.AiAdviceTrainingSample;
import my.management.module.ai.model.vo.AiAdviceDailyBriefVO;
import my.management.module.ai.model.vo.AiAdviceLearningStatVO;
import my.management.module.ai.model.vo.AiAdviceRuleLearningStatVO;
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
 * AI 分析服务。
 *
 * <p>主链路已从本地规则切换为模型 Provider：DeepSeek 用于近期商用推理，
 * self-trained Transformer 用于未来自训练模型。历史本地规则方法仅保留为兼容代码，不再参与主生成链路。</p>
 */
@Service
public class AiAnalysisService {

    private static final BigDecimal INVENTORY_WARNING_THRESHOLD = new BigDecimal("100");
    private static final DateTimeFormatter ADVICE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DAY_PREFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String ONLINE_STRATEGY_VERSION = "transformer_self_training_v1_online";
    private static final String CURRENT_RULE_VERSION = "transformer.2026.05.12.1";
    private static final String ONLINE_EVALUATION_MODE = "ONLINE";
    private static final String DAILY_BRIEF_VERSION = "daily_brief_v1";
    private static final int LEARNING_WINDOW_DAYS = 90;
    private static final int MAX_RULE_LEARNING_PATTERNS = 200;
    private static final int MAX_TRANSFORMER_TRAINING_EXAMPLES = 12;

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

        List<AiAdviceTrainingSample> trainingExamples = loadRecentTransformerTrainingExamples(tenantCode);
        AiBusinessSnapshotVO scopedSnapshot = scopeSnapshotForVisibility(snapshot, visibility);
        advices.addAll(buildProviderAdvices(scopedSnapshot, List.of(), trainingExamples));

        Map<String, BehaviorModulePreferenceVO> behaviorPreferences = loadTenantBehaviorPreferences(tenantCode);
        applyTenantBehaviorPersonalization(behaviorPreferences, advices);
        Map<String, AiAdviceLearningStatVO> learningStats = loadTenantLearningStats(tenantCode);
        Map<String, AiAdviceRuleLearningStatVO> ruleLearningStats = loadTenantRuleLearningStats(tenantCode);
        applyTenantFeedbackPersonalization(learningStats, ruleLearningStats, advices);

        applyGovernanceMetadata(advices);
        persistTrainingSamples(tenantCode, snapshot, behaviorPreferences, advices);
        return advices;
    }

    public AiAdviceDailyBriefVO buildDailyBrief(List<DashboardAiAdviceVO> advices) {
        AiAdviceDailyBriefVO brief = new AiAdviceDailyBriefVO();
        brief.setGeneratedAt(LocalDateTime.now());
        brief.setBriefVersion(DAILY_BRIEF_VERSION);

        List<DashboardAiAdviceVO> safeAdvices = advices == null
                ? List.of()
                : advices.stream().filter(Objects::nonNull).toList();
        if (safeAdvices.isEmpty()) {
            brief.setTitle("今日经营简报");
            brief.setExecutiveSummary("当前暂无可见 AI 建议。请继续沉淀订单、库存、客户、员工和审批数据，系统会在识别到有效信号后生成简报。");
            brief.setTopRiskTitle("暂无重点风险");
            brief.setTopRiskSummary("当前没有可见的高优先级经营风险。");
            brief.setFirstAction("保持日常数据录入和巡检节奏。");
            return brief;
        }

        List<DashboardAiAdviceVO> sorted = safeAdvices.stream()
                .sorted((left, right) -> {
                    int priorityCompare = Integer.compare(priorityOrder(left.getPriority()), priorityOrder(right.getPriority()));
                    if (priorityCompare != 0) {
                        return priorityCompare;
                    }
                    return Integer.compare(nvlInt(right.getRiskScore()), nvlInt(left.getRiskScore()));
                })
                .toList();
        List<DashboardAiAdviceVO> risks = sorted.stream()
                .filter(this::isBriefRisk)
                .toList();
        List<DashboardAiAdviceVO> watchItems = sorted.stream()
                .filter(advice -> !isBriefRisk(advice))
                .toList();

        DashboardAiAdviceVO top = risks.isEmpty() ? sorted.get(0) : risks.get(0);
        brief.setTitle(resolveBriefTitle(risks.size(), sorted.size()));
        brief.setExecutiveSummary(resolveBriefSummary(risks, watchItems, top));
        brief.setTopRiskTitle(defaultText(top.getTitle(), "重点经营事项"));
        brief.setTopRiskSummary(defaultText(top.getSummary(), top.getSuggestion()));
        brief.setFirstAction(defaultText(top.getFirstAction(), defaultText(top.getSuggestion(), "明确责任人、截止时间和复盘指标。")));
        brief.setRiskCount(risks.size());
        brief.setUrgentActionCount(Math.min(risks.size(), 5));
        brief.setWatchCount(watchItems.size());

        risks.stream().limit(5).map(this::toBriefItem).forEach(brief.getUrgentActions()::add);
        watchItems.stream().limit(5).map(this::toBriefItem).forEach(brief.getWatchItems()::add);
        sorted.stream()
                .filter(advice -> !isBlank(advice.getReviewMetric()))
                .limit(5)
                .map(this::toBriefItem)
                .forEach(brief.getReviewItems()::add);
        return brief;
    }

    /**
     * 调用模型 Provider 生成建议。
     *
     * <p>Provider 按优先级执行：自训练 Transformer 优先，DeepSeek 兜底。任一 Provider 成功返回建议后即停止，
     * 避免多模型同时输出导致重复建议和口径不一致。</p>
     */
    private List<DashboardAiAdviceVO> buildProviderAdvices(AiBusinessSnapshotVO snapshot,
                                                           List<DashboardAiAdviceVO> referenceAdvices,
                                                           List<AiAdviceTrainingSample> trainingExamples) {
        if (aiInsightProviders == null || aiInsightProviders.isEmpty()) {
            return List.of();
        }

        for (AiInsightProvider provider : aiInsightProviders) {
            if (provider == null || !provider.enabled()) {
                continue;
            }
            try {
                List<DashboardAiAdviceVO> generated = provider.generate(snapshot, referenceAdvices, trainingExamples);
                if (generated != null) {
                    List<DashboardAiAdviceVO> normalized = generated.stream()
                            .filter(Objects::nonNull)
                            .map(this::normalizeProviderAdvice)
                            .toList();
                    if (!normalized.isEmpty()) {
                        return normalized;
                    }
                }
            } catch (Exception ignored) {
                // 模型推理失败不能阻断大盘加载，继续尝试下一个 Provider。
            }
        }
        return List.of();
    }

    private List<AiAdviceTrainingSample> loadRecentTransformerTrainingExamples(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return List.of();
        }
        try {
            List<AiAdviceTrainingSample> samples = aiAdviceTrainingSampleMapper.selectRecentFeedbackSamples(
                    tenantCode,
                    LEARNING_WINDOW_DAYS,
                    MAX_TRANSFORMER_TRAINING_EXAMPLES
            );
            return samples == null ? List.of() : samples.stream().filter(Objects::nonNull).toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private AiBusinessSnapshotVO scopeSnapshotForVisibility(AiBusinessSnapshotVO snapshot, DashboardOverviewVO.Visibility visibility) {
        if (snapshot == null || visibility == null || isFullAiVisibility(visibility)) {
            return snapshot;
        }
        AiBusinessSnapshotVO scoped = objectMapper.convertValue(snapshot, AiBusinessSnapshotVO.class);
        if (!Boolean.TRUE.equals(visibility.getOrderVisible())) {
            scoped.setOrder(new AiBusinessSnapshotVO.OrderMetrics());
        }
        if (!Boolean.TRUE.equals(visibility.getInventoryVisible())) {
            scoped.setInventory(new AiBusinessSnapshotVO.InventoryMetrics());
            scoped.setTrend(new AiBusinessSnapshotVO.TrendMetrics());
        }
        if (!Boolean.TRUE.equals(visibility.getAttendanceVisible())) {
            scoped.setEmployee(new AiBusinessSnapshotVO.EmployeeMetrics());
        }
        if (!Boolean.TRUE.equals(visibility.getApprovalVisible())) {
            scoped.setFinance(new AiBusinessSnapshotVO.FinanceMetrics());
        }
        scoped.setQuality(new AiBusinessSnapshotVO.QualityMetrics());
        if (!Boolean.TRUE.equals(visibility.getOrderVisible())) {
            scoped.setCustomer(new AiBusinessSnapshotVO.CustomerMetrics());
        }
        scoped.setTenantCode(null);
        return scoped;
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

    private boolean isOperationVisibility(DashboardOverviewVO.Visibility visibility) {
        return visibility != null
                && (Boolean.TRUE.equals(visibility.getApprovalVisible())
                || Boolean.TRUE.equals(visibility.getReceiptVisible())
                || isFullAiVisibility(visibility));
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
            List<AiAdviceLearningStatVO> stats = aiAdviceTrainingSampleMapper.selectLearningStats(tenantCode, LEARNING_WINDOW_DAYS);
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
     * 读取具体建议模式的反馈画像。
     *
     * <p>规则级画像比维度级画像更细：同样是质量建议，只有被多次证明不合适的标题会降权，
     * 其它质量治理建议仍然可以正常出现。</p>
     */
    private Map<String, AiAdviceRuleLearningStatVO> loadTenantRuleLearningStats(String tenantCode) {
        Map<String, AiAdviceRuleLearningStatVO> statMap = new LinkedHashMap<>();
        if (tenantCode == null || tenantCode.isBlank()) {
            return statMap;
        }
        try {
            List<AiAdviceRuleLearningStatVO> stats = aiAdviceTrainingSampleMapper.selectRuleLearningStats(
                    tenantCode,
                    LEARNING_WINDOW_DAYS,
                    MAX_RULE_LEARNING_PATTERNS
            );
            for (AiAdviceRuleLearningStatVO stat : stats) {
                if (stat != null && !isBlank(stat.getTitle())) {
                    statMap.put(ruleLearningKey(stat.getCategory(), stat.getTitle(), stat.getSourceType()), stat);
                }
            }
        } catch (Exception ignored) {
            // 规则级画像是增强能力，查询失败时继续使用维度级学习。
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
                                                   Map<String, AiAdviceRuleLearningStatVO> ruleLearningStats,
                                                   List<DashboardAiAdviceVO> advices) {
        boolean hasCategoryLearning = learningStats != null && !learningStats.isEmpty();
        boolean hasRuleLearning = ruleLearningStats != null && !ruleLearningStats.isEmpty();
        if ((!hasCategoryLearning && !hasRuleLearning) || advices == null || advices.isEmpty()) {
            return;
        }

        for (DashboardAiAdviceVO advice : advices) {
            if (advice == null) {
                continue;
            }
            AiAdviceLearningStatVO categoryStat = hasCategoryLearning
                    ? learningStats.get(defaultText(advice.getCategory(), "overview"))
                    : null;
            AiAdviceRuleLearningStatVO ruleStat = hasRuleLearning
                    ? ruleLearningStats.get(ruleLearningKey(advice.getCategory(), advice.getTitle(), advice.getSourceType()))
                    : null;
            int feedbackSignal = combinedFeedbackSignal(categoryStat, ruleStat);
            if (feedbackSignal == 0) {
                continue;
            }

            int confidence = advice.getConfidence() == null ? resolveConfidence(advice.getLevel()) : advice.getConfidence();
            if (feedbackSignal > 0) {
                advice.setConfidence(Math.min(confidence + Math.min(feedbackSignal * 2, 14), 98));
                if ("P2".equals(advice.getPriority()) && feedbackSignal >= 4 && nvlInt(advice.getRiskScore()) >= 55) {
                    advice.setPriority("P1");
                }
            } else {
                advice.setConfidence(Math.max(confidence + Math.max(feedbackSignal * 3, -21), 42));
                if (!"warning".equals(advice.getLevel()) && !"P3".equals(advice.getPriority())) {
                    advice.setPriority("P3");
                }
            }
            applyLearningGuidance(advice, feedbackSignal, categoryStat, ruleStat);
        }

        advices.removeIf(advice -> shouldSuppressByLearning(
                advice,
                hasCategoryLearning ? learningStats.get(defaultText(advice.getCategory(), "overview")) : null,
                hasRuleLearning ? ruleLearningStats.get(ruleLearningKey(advice.getCategory(), advice.getTitle(), advice.getSourceType())) : null
        ));

        advices.sort((left, right) -> {
            if (left == right) {
                return 0;
            }
            if (left == null) {
                return 1;
            }
            if (right == null) {
                return -1;
            }
            int priorityCompare = Integer.compare(priorityOrder(left.getPriority()), priorityOrder(right.getPriority()));
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            int learnedCompare = Integer.compare(
                    learnedAdviceScore(
                            hasCategoryLearning ? learningStats.get(defaultText(right.getCategory(), "overview")) : null,
                            hasRuleLearning ? ruleLearningStats.get(ruleLearningKey(right.getCategory(), right.getTitle(), right.getSourceType())) : null,
                            right
                    ),
                    learnedAdviceScore(
                            hasCategoryLearning ? learningStats.get(defaultText(left.getCategory(), "overview")) : null,
                            hasRuleLearning ? ruleLearningStats.get(ruleLearningKey(left.getCategory(), left.getTitle(), left.getSourceType())) : null,
                            left
                    )
            );
            if (learnedCompare != 0) {
                return learnedCompare;
            }
            return Integer.compare(nvlInt(right.getRiskScore()), nvlInt(left.getRiskScore()));
        });
    }

    private int learnedAdviceScore(AiAdviceLearningStatVO categoryStat,
                                   AiAdviceRuleLearningStatVO ruleStat,
                                   DashboardAiAdviceVO advice) {
        int score = nvlInt(advice.getConfidence()) + nvlInt(advice.getRiskScore()) / 2;
        return score + combinedFeedbackSignal(categoryStat, ruleStat) * 5;
    }

    private int combinedFeedbackSignal(AiAdviceLearningStatVO categoryStat, AiAdviceRuleLearningStatVO ruleStat) {
        int categorySignal = categoryFeedbackSignal(categoryStat);
        int ruleSignal = ruleFeedbackSignal(ruleStat);
        if (ruleSignal == 0) {
            return categorySignal;
        }
        if (categorySignal == 0) {
            return ruleSignal;
        }
        return boundSignal(Math.round(ruleSignal * 0.7F + categorySignal * 0.3F));
    }

    private int categoryFeedbackSignal(AiAdviceLearningStatVO stat) {
        if (stat == null) {
            return 0;
        }
        return feedbackSignal(
                nvl(stat.getSampleCount()),
                nvl(stat.getFeedbackCount()),
                nvl(stat.getPositiveCount()),
                nvl(stat.getResolvedCount()),
                nvl(stat.getNegativeCount()),
                nvl(stat.getIgnoredCount()),
                stat.getLatestFeedbackTime(),
                20,
                5
        );
    }

    private int ruleFeedbackSignal(AiAdviceRuleLearningStatVO stat) {
        if (stat == null) {
            return 0;
        }
        return feedbackSignal(
                nvl(stat.getSampleCount()),
                nvl(stat.getFeedbackCount()),
                nvl(stat.getPositiveCount()),
                nvl(stat.getResolvedCount()),
                nvl(stat.getNegativeCount()),
                nvl(stat.getIgnoredCount()),
                stat.getLatestFeedbackTime(),
                6,
                2
        );
    }

    private int feedbackSignal(long sampleCount,
                               long feedbackCount,
                               long positiveCount,
                               long resolvedCount,
                               long negativeCount,
                               long ignoredCount,
                               LocalDateTime latestFeedbackTime,
                               int minSampleCount,
                               int minFeedbackCount) {
        if (sampleCount < minSampleCount || feedbackCount < minFeedbackCount) {
            return 0;
        }
        long effectiveFeedbackCount = Math.max(feedbackCount, 1);
        long positiveTotal = positiveCount + resolvedCount;
        long negativeTotal = negativeCount + ignoredCount;
        int positiveRate = (int) Math.round(positiveTotal * 100D / effectiveFeedbackCount);
        int negativeRate = (int) Math.round(negativeTotal * 100D / effectiveFeedbackCount);
        int raw = (int) Math.round((positiveCount * 2D + resolvedCount * 3D - negativeCount * 4D - ignoredCount * 2D) * 10D / effectiveFeedbackCount);
        int coverageRate = (int) Math.round(feedbackCount * 100D / Math.max(sampleCount, 1));

        if (negativeRate >= 60 && feedbackCount >= minFeedbackCount + 1) {
            raw = Math.min(raw, -8);
        } else if (positiveRate >= 70 && coverageRate >= 10) {
            raw = Math.max(raw, 5);
        }
        if (coverageRate < 8) {
            raw = Math.round(raw * 0.6F);
        }
        if (latestFeedbackTime != null && ChronoUnit.DAYS.between(latestFeedbackTime, LocalDateTime.now()) > 45) {
            raw = Math.round(raw * 0.7F);
        }
        return boundSignal(raw);
    }

    private void applyLearningGuidance(DashboardAiAdviceVO advice,
                                       int feedbackSignal,
                                       AiAdviceLearningStatVO categoryStat,
                                       AiAdviceRuleLearningStatVO ruleStat) {
        if (advice == null || feedbackSignal == 0) {
            return;
        }
        if (feedbackSignal >= 5) {
            addUniqueActionStep(advice, "复用本租户历史有效处理路径，处理完成后继续回写结果。");
            addUniqueSuccessCriteria(advice, "处理结果已回写为可复用样本");
            if (isBlank(advice.getCapabilityMaturity()) || "机制优化".equals(advice.getCapabilityMaturity())) {
                advice.setCapabilityMaturity("流程固化");
            }
            advice.setTrackingHint(appendLearningHint(
                    advice.getTrackingHint(),
                    "历史处理反馈较好，建议将本次处理动作沉淀为标准清单。"
            ));
            return;
        }
        if (feedbackSignal <= -5) {
            addUniqueDataCheckpoint(advice, "先核验该建议是否仍适用当前业务场景");
            addUniqueActionStep(advice, "若核验后不适用，请在待办或建议反馈中标记原因，帮助系统继续降噪。");
            advice.setTrackingHint(appendLearningHint(
                    advice.getTrackingHint(),
                    buildNegativeLearningHint(categoryStat, ruleStat)
            ));
        }
    }

    private String buildNegativeLearningHint(AiAdviceLearningStatVO categoryStat, AiAdviceRuleLearningStatVO ruleStat) {
        int ruleNegativeRate = feedbackNegativeRate(
                nvl(ruleStat == null ? null : ruleStat.getFeedbackCount()),
                nvl(ruleStat == null ? null : ruleStat.getNegativeCount()) + nvl(ruleStat == null ? null : ruleStat.getIgnoredCount())
        );
        if (ruleStat != null && nvl(ruleStat.getFeedbackCount()) >= 2 && ruleNegativeRate >= 50) {
            return "该类建议近期负反馈偏高，请优先核验数据口径和业务适用性。";
        }
        int categoryNegativeRate = feedbackNegativeRate(
                nvl(categoryStat == null ? null : categoryStat.getFeedbackCount()),
                nvl(categoryStat == null ? null : categoryStat.getNegativeCount()) + nvl(categoryStat == null ? null : categoryStat.getIgnoredCount())
        );
        if (categoryNegativeRate >= 50) {
            return "该维度近期反馈分歧较大，请先核验数据完整性后再推进。";
        }
        return "当前建议已按反馈降权，请处理时补充是否适用的原因。";
    }

    private boolean shouldSuppressByLearning(DashboardAiAdviceVO advice,
                                             AiAdviceLearningStatVO categoryStat,
                                             AiAdviceRuleLearningStatVO ruleStat) {
        if (advice == null || !isSuppressibleAdvice(advice)) {
            return false;
        }
        long ruleFeedbackCount = nvl(ruleStat == null ? null : ruleStat.getFeedbackCount());
        long ruleNegativeCount = nvl(ruleStat == null ? null : ruleStat.getNegativeCount())
                + nvl(ruleStat == null ? null : ruleStat.getIgnoredCount());
        int ruleNegativeRate = feedbackNegativeRate(ruleFeedbackCount, ruleNegativeCount);
        if (ruleFeedbackCount >= 3 && ruleNegativeRate >= 60) {
            return true;
        }

        long categoryFeedbackCount = nvl(categoryStat == null ? null : categoryStat.getFeedbackCount());
        long categoryNegativeCount = nvl(categoryStat == null ? null : categoryStat.getNegativeCount())
                + nvl(categoryStat == null ? null : categoryStat.getIgnoredCount());
        int categoryNegativeRate = feedbackNegativeRate(categoryFeedbackCount, categoryNegativeCount);
        return ruleStat == null
                && categoryFeedbackCount >= 12
                && categoryNegativeRate >= 75
                && nvlInt(advice.getConfidence()) < 60;
    }

    private boolean isSuppressibleAdvice(DashboardAiAdviceVO advice) {
        if ("P0".equals(advice.getPriority()) || "P1".equals(advice.getPriority())) {
            return false;
        }
        if ("warning".equals(advice.getLevel()) || "critical".equals(advice.getLevel())) {
            return false;
        }
        return nvlInt(advice.getRiskScore()) < 70;
    }

    private int feedbackNegativeRate(long feedbackCount, long negativeCount) {
        if (feedbackCount <= 0) {
            return 0;
        }
        return (int) Math.round(negativeCount * 100D / feedbackCount);
    }

    private String appendLearningHint(String current, String hint) {
        if (isBlank(hint)) {
            return current;
        }
        String base = defaultText(current, "");
        if (base.contains(hint)) {
            return base;
        }
        return base.isBlank() ? hint : base + " " + hint;
    }

    private void addUniqueActionStep(DashboardAiAdviceVO advice, String value) {
        advice.setActionSteps(addUnique(advice.getActionSteps(), value));
    }

    private void addUniqueSuccessCriteria(DashboardAiAdviceVO advice, String value) {
        advice.setSuccessCriteria(addUnique(advice.getSuccessCriteria(), value));
    }

    private void addUniqueDataCheckpoint(DashboardAiAdviceVO advice, String value) {
        advice.setDataCheckpoints(addUnique(advice.getDataCheckpoints(), value));
    }

    private List<String> addUnique(List<String> values, String value) {
        if (isBlank(value)) {
            return values == null ? new ArrayList<>() : new ArrayList<>(values);
        }
        List<String> result = values == null ? new ArrayList<>() : new ArrayList<>(values);
        if (!result.contains(value)) {
            result.add(value);
        }
        return result;
    }

    private int boundSignal(int value) {
        return Math.max(-10, Math.min(value, 10));
    }

    private String ruleLearningKey(String category, String title, String sourceType) {
        return String.join("|",
                defaultText(category, "overview").trim().toLowerCase(),
                defaultText(title, "untitled").trim(),
                defaultText(sourceType, "transformer").trim().toLowerCase()
        );
    }

    private boolean isBriefRisk(DashboardAiAdviceVO advice) {
        if (advice == null) {
            return false;
        }
        String priority = defaultText(advice.getPriority(), resolvePriority(advice.getLevel()));
        return "P0".equals(priority)
                || "P1".equals(priority)
                || "warning".equals(advice.getLevel())
                || nvlInt(advice.getRiskScore()) >= 75;
    }

    private String resolveBriefTitle(int riskCount, int totalCount) {
        if (riskCount <= 0) {
            return "今日经营简报：整体平稳";
        }
        if (riskCount >= 3) {
            return "今日经营简报：多维风险需联动处理";
        }
        return "今日经营简报：重点事项需跟进";
    }

    private String resolveBriefSummary(List<DashboardAiAdviceVO> risks,
                                       List<DashboardAiAdviceVO> watchItems,
                                       DashboardAiAdviceVO top) {
        int riskCount = risks == null ? 0 : risks.size();
        int watchCount = watchItems == null ? 0 : watchItems.size();
        String topTitle = defaultText(top == null ? null : top.getTitle(), "重点事项");
        if (riskCount <= 0) {
            return String.format("当前共识别 %d 条观察类建议，暂无高优先级风险。建议保持日常巡检，并重点关注“%s”。", watchCount, topTitle);
        }
        return String.format(
                "当前共识别 %d 条重点风险、%d 条观察事项。今日优先处理“%s”，先明确责任人、处理窗口和复盘指标。",
                riskCount,
                watchCount,
                topTitle
        );
    }

    private AiAdviceDailyBriefVO.BriefItem toBriefItem(DashboardAiAdviceVO advice) {
        AiAdviceDailyBriefVO.BriefItem item = new AiAdviceDailyBriefVO.BriefItem();
        item.setCategory(defaultText(advice.getCategory(), "overview"));
        item.setTitle(defaultText(advice.getTitle(), "未命名建议"));
        item.setPriority(defaultText(advice.getPriority(), resolvePriority(advice.getLevel())));
        item.setRiskScore(advice.getRiskScore());
        item.setAction(defaultText(advice.getFirstAction(), advice.getSuggestion()));
        item.setRoute(advice.getRoute());
        item.setOwnerDepartment(advice.getOwnerDepartment());
        item.setReviewMetric(advice.getReviewMetric());
        return item;
    }

    private void applyGovernanceMetadata(List<DashboardAiAdviceVO> advices) {
        if (advices == null || advices.isEmpty()) {
            return;
        }
        for (DashboardAiAdviceVO advice : advices) {
            if (advice == null) {
                continue;
            }
            if (isBlank(advice.getDecisionQuestion())) {
                advice.setDecisionQuestion(resolveDecisionQuestion(advice.getCategory()));
            }
            if (isBlank(advice.getCollaborationPath())) {
                advice.setCollaborationPath(resolveCollaborationPath(advice.getCategory()));
            }
            if (isBlank(advice.getEscalationRule())) {
                advice.setEscalationRule(resolveEscalationRule(advice));
            }
            if (isBlank(advice.getPreventionAction())) {
                advice.setPreventionAction(resolvePreventionAction(advice.getCategory()));
            }
            if (isBlank(advice.getMeetingCadence())) {
                advice.setMeetingCadence(resolveMeetingCadence(advice));
            }
            if (isEmpty(advice.getStakeholderTags())) {
                advice.setStakeholderTags(resolveStakeholderTags(advice.getCategory()));
            }
            if (isEmpty(advice.getActionSteps())) {
                advice.setActionSteps(resolveActionSteps(advice));
            }
            if (isEmpty(advice.getSuccessCriteria())) {
                advice.setSuccessCriteria(resolveSuccessCriteria(advice));
            }
            if (isEmpty(advice.getDataCheckpoints())) {
                advice.setDataCheckpoints(resolveDataCheckpoints(advice.getCategory()));
            }
            if (isBlank(advice.getExpectedOutcome())) {
                advice.setExpectedOutcome(resolveExpectedOutcome(advice.getCategory()));
            }
            if (isBlank(advice.getReviewDeadline())) {
                advice.setReviewDeadline(resolveReviewDeadline(advice));
            }
            if (isBlank(advice.getRiskGuardrail())) {
                advice.setRiskGuardrail(resolveRiskGuardrail(advice.getCategory()));
            }
            if (isBlank(advice.getCapabilityMaturity())) {
                advice.setCapabilityMaturity(resolveCapabilityMaturity(advice));
            }
            if (isBlank(advice.getVisibilityTier())) {
                advice.setVisibilityTier(resolveVisibilityTier(advice));
            }
            if (isBlank(advice.getRuleCode())) {
                advice.setRuleCode(resolveRuleCode(advice));
            }
            if (isBlank(advice.getRuleVersion())) {
                advice.setRuleVersion(CURRENT_RULE_VERSION);
            }
            if (isBlank(advice.getStrategyVersion())) {
                advice.setStrategyVersion(ONLINE_STRATEGY_VERSION);
            }
            if (isBlank(advice.getEvaluationMode())) {
                advice.setEvaluationMode(ONLINE_EVALUATION_MODE);
            }
        }
    }

    private String resolveDecisionQuestion(String category) {
        return switch (defaultText(category, "overview")) {
            case "inventory" -> "当前库存风险会影响哪些订单承诺，是否需要调整补货优先级？";
            case "order", "delivery" -> "当前交付风险是流程卡点、产能不足，还是客户沟通不充分？";
            case "customer" -> "客户变化是短期波动，还是复购信任和增长来源正在变弱？";
            case "employee" -> "员工异常是规则配置问题、班次变化，还是关键岗位承接风险？";
            case "quality" -> "质量问题是偶发损耗，还是已经指向工艺、供应商或班组的重复风险？";
            case "finance" -> "当前资金和审批压力是否会影响采购、交付或供应商协作？";
            case "operation" -> "多个业务异常之间是否已经形成连锁风险，需要管理层统一调度？";
            default -> "当前数据变化最可能影响哪一个经营结果，谁需要负责关闭？";
        };
    }

    private String resolveCollaborationPath(String category) {
        return switch (defaultText(category, "overview")) {
            case "inventory" -> "仓库核实库存，销售确认订单消耗，采购或生产确认补货与排产。";
            case "order", "delivery" -> "销售确认客户承诺，生产确认排程，仓库确认出库，必要时财务确认款项状态。";
            case "customer" -> "销售牵头客户沟通，客服或跟单同步交付体验，生产仓库反馈可承诺时间。";
            case "employee" -> "人事核实规则和异常，部门负责人确认班次与岗位承接，必要时运营协调替岗。";
            case "quality" -> "质检定位问题，生产复盘工艺，财务量化损失，销售同步客户影响。";
            case "finance" -> "财务梳理金额和审批，业务负责人确认影响范围，老板或授权人处理高金额事项。";
            case "operation" -> "运营负责人牵头，销售、生产、仓库、质检、人事、财务按风险清单逐项确认。";
            default -> "先明确责任人，再把处理动作同步到相关部门并设置复盘时间。";
        };
    }

    private String resolveEscalationRule(DashboardAiAdviceVO advice) {
        int riskScore = nvlInt(advice.getRiskScore());
        String priority = defaultText(advice.getPriority(), "P2");
        if ("P0".equals(priority) || riskScore >= 90) {
            return "今日未关闭必须升级到老板或经营负责人，并记录责任人、截止时间和阻塞原因。";
        }
        if ("P1".equals(priority) || riskScore >= 75 || "warning".equals(advice.getLevel())) {
            return "超过一个工作日仍未关闭，升级到部门负责人；涉及核心客户或大额资金时立即升级。";
        }
        return "若连续两次复盘仍未改善，升级到部门负责人确认流程或规则是否需要调整。";
    }

    private String resolvePreventionAction(String category) {
        return switch (defaultText(category, "overview")) {
            case "inventory" -> "把低库存型号从事后提醒升级为安全库存和预计可用天数双阈值。";
            case "order", "delivery" -> "建立临期订单 T-3 巡检机制，提前确认生产、出库和客户沟通状态。";
            case "customer" -> "维护客户分层和跟进记录，让沉睡客户、核心客户和新客户开发形成固定节奏。";
            case "employee" -> "定期校验考勤规则、班次和直属上级关系，避免组织数据影响真实管理判断。";
            case "quality" -> "把重复次品原因沉淀为工艺检查项、供应商复盘项或班组培训项。";
            case "finance" -> "设置高金额审批优先级和超时提醒，避免资金事项卡住采购或交付。";
            case "operation" -> "把跨部门风险雷达固定到晨会和周会，跟踪未关闭事项和复发原因。";
            default -> "将处理结果沉淀为规则、清单或复盘模板，避免同类问题重复靠人工发现。";
        };
    }

    private String resolveMeetingCadence(DashboardAiAdviceVO advice) {
        int riskScore = nvlInt(advice.getRiskScore());
        String priority = defaultText(advice.getPriority(), "P2");
        if ("P0".equals(priority) || riskScore >= 90) {
            return "今日经营会确认，关闭前每日追踪";
        }
        if ("P1".equals(priority) || riskScore >= 75 || "warning".equals(advice.getLevel())) {
            return "每日晨会跟进，关闭后周会复盘";
        }
        if ("success".equals(advice.getLevel())) {
            return "周会观察，月度复盘趋势";
        }
        return "本周例会确认责任人和下一步";
    }

    private String resolveVisibilityTier(DashboardAiAdviceVO advice) {
        String category = defaultText(advice.getCategory(), "overview");
        int riskScore = nvlInt(advice.getRiskScore());
        if ("operation".equals(category) || "P0".equals(advice.getPriority()) || riskScore >= 85) {
            return "老板 / 经营负责人";
        }
        return switch (category) {
            case "finance" -> "老板 / 财务负责人";
            case "employee" -> "老板 / 人事与部门负责人";
            case "customer" -> "老板 / 销售负责人";
            case "quality" -> "老板 / 质检与生产负责人";
            default -> "部门负责人 / 相关执行人";
        };
    }

    private List<String> resolveStakeholderTags(String category) {
        return switch (defaultText(category, "overview")) {
            case "inventory" -> List.of("仓库", "采购", "销售", "生产");
            case "order", "delivery" -> List.of("销售", "生产", "仓库", "跟单");
            case "customer" -> List.of("销售", "客服", "跟单", "老板");
            case "employee" -> List.of("人事", "部门主管", "员工", "运营");
            case "quality" -> List.of("质检", "生产", "仓库", "销售");
            case "finance" -> List.of("财务", "老板", "业务负责人");
            case "operation" -> List.of("老板", "运营", "销售", "生产", "仓库", "财务");
            default -> List.of("管理层", "责任部门");
        };
    }

    private List<String> resolveActionSteps(DashboardAiAdviceVO advice) {
        String category = defaultText(advice.getCategory(), "overview");
        List<String> steps = new ArrayList<>();
        steps.add(defaultText(advice.getFirstAction(), "先明确责任人、截止时间和复盘指标。"));
        switch (category) {
            case "inventory" -> {
                steps.add("核对异常型号的现有库存、在途补货和近 7 天出库速度。");
                steps.add("确认是否影响已承诺订单，并给出补货、调拨或替代方案。");
            }
            case "order", "delivery" -> {
                steps.add("拉出受影响订单清单，逐单确认生产、出库和客户沟通状态。");
                steps.add("对无法按期交付的订单提前生成客户沟通方案。");
            }
            case "customer" -> {
                steps.add("按客户价值和最近下单时间分层，优先处理核心客户和沉睡客户。");
                steps.add("补齐最近一次沟通记录、下一步动作和预计回访时间。");
            }
            case "employee" -> {
                steps.add("核对考勤、请假、班次和直属上级关系，排除配置误判。");
                steps.add("由部门主管确认真实业务原因，并记录改进动作。");
            }
            case "quality" -> {
                steps.add("按次品类型、订单来源、责任环节和损失金额导出明细。");
                steps.add("确认是否需要升级为工艺、供应商或班组专项复盘。");
            }
            case "finance" -> {
                steps.add("按金额和紧急程度排序待处理事项，先处理高影响项。");
                steps.add("同步业务负责人确认对采购、交付和客户承诺的影响。");
            }
            case "operation" -> {
                steps.add("把相关异常合并成一张跨部门风险清单。");
                steps.add("在经营例会上明确 owner、截止时间和阻塞点。");
            }
            default -> {
                steps.add("补齐关键业务数据，确认建议是否具备执行条件。");
                steps.add("把处理结果沉淀为下次可复用的规则或清单。");
            }
        }
        if ("P0".equals(advice.getPriority()) || nvlInt(advice.getRiskScore()) >= 90) {
            steps.add("今日内升级给老板或经营负责人确认，未关闭前每日追踪。");
        } else if ("P1".equals(advice.getPriority()) || "warning".equals(advice.getLevel())) {
            steps.add("一个工作日内反馈处理进度，超过时限自动升级部门负责人。");
        }
        return steps;
    }

    private List<String> resolveSuccessCriteria(DashboardAiAdviceVO advice) {
        return switch (defaultText(advice.getCategory(), "overview")) {
            case "inventory" -> List.of("异常型号已完成库存核对", "影响订单已明确处理方案", "补货或调拨责任人已确认");
            case "order", "delivery" -> List.of("临期订单已逐单确认状态", "延期风险已提前沟通", "预计交付时间已更新");
            case "customer" -> List.of("核心客户已完成回访", "沉睡客户已标记原因", "下一步跟进时间已记录");
            case "employee" -> List.of("异常人员已核实原因", "班次/请假/主管关系已校验", "改进动作已同步责任人");
            case "quality" -> List.of("次品明细已归因", "重复问题已建立预防动作", "损失金额和责任环节已确认");
            case "finance" -> List.of("高金额事项已处理或升级", "业务影响已确认", "审批/付款状态已同步");
            case "operation" -> List.of("跨部门风险清单已建立", "owner 和截止时间已明确", "下次复盘节点已确定");
            default -> List.of("责任人已明确", "截止时间已确认", "复盘指标已记录");
        };
    }

    private List<String> resolveDataCheckpoints(String category) {
        return switch (defaultText(category, "overview")) {
            case "inventory" -> List.of("库存米数", "出入库流水", "在途补货", "关联订单");
            case "order", "delivery" -> List.of("交付日期", "生产状态", "出库状态", "客户沟通记录");
            case "customer" -> List.of("最近下单时间", "历史销售额", "回访记录", "报价未成交原因");
            case "employee" -> List.of("考勤记录", "请假审批", "班次规则", "直属上级");
            case "quality" -> List.of("次品类型", "损失金额", "责任环节", "处理结果");
            case "finance" -> List.of("待审批金额", "付款状态", "订单金额", "成本损耗");
            case "operation" -> List.of("库存", "订单", "客户", "员工", "质量", "财务");
            default -> List.of("核心指标", "异常原因", "处理记录");
        };
    }

    private String resolveExpectedOutcome(String category) {
        return switch (defaultText(category, "overview")) {
            case "inventory" -> "降低缺料、积压和临时调货风险，让库存从事后提醒升级为提前准备。";
            case "order", "delivery" -> "提高准时交付率，减少交付当天才暴露的问题和被动解释。";
            case "customer" -> "提升客户复购稳定性，提前识别沉睡、流失和核心客户波动。";
            case "employee" -> "减少组织数据不准导致的误判，让考勤、主管和岗位承接更可靠。";
            case "quality" -> "把单次次品处理沉淀成可复盘、可预防的质量改进机制。";
            case "finance" -> "减少资金、审批和成本异常对采购、交付和经营节奏的影响。";
            case "operation" -> "把跨部门异常收敛成统一风险清单，提升管理闭环效率。";
            default -> "让管理层更早发现异常，并形成可追踪的处理闭环。";
        };
    }

    private String resolveReviewDeadline(DashboardAiAdviceVO advice) {
        int riskScore = nvlInt(advice.getRiskScore());
        String priority = defaultText(advice.getPriority(), "P2");
        if ("P0".equals(priority) || riskScore >= 90) {
            return "今日下班前完成首次复盘";
        }
        if ("P1".equals(priority) || riskScore >= 75 || "warning".equals(advice.getLevel())) {
            return "下一个工作日晨会前复盘";
        }
        if ("success".equals(advice.getLevel())) {
            return "下次周会观察趋势";
        }
        return "本周五前完成复盘";
    }

    private String resolveRiskGuardrail(String category) {
        return switch (defaultText(category, "overview")) {
            case "inventory" -> "不能为降低库存而影响已承诺订单交付，也不能跳过库存实物核对。";
            case "order", "delivery" -> "不能为了短期交付承诺牺牲客户沟通真实性或生产可执行性。";
            case "customer" -> "客户分层只用于经营跟进，不应暴露给无关人员或形成歧视性服务。";
            case "employee" -> "员工建议必须先排除规则配置、请假同步和设备定位问题，避免直接归责个人。";
            case "quality" -> "质量归因必须保留证据链，不能仅凭单条记录判定责任。";
            case "finance" -> "高金额事项必须保留审批和沟通记录，不能绕过授权流程。";
            case "operation" -> "跨部门调度必须以真实数据为准，不能用未核实异常推动组织调整。";
            default -> "所有建议必须经过责任人确认，不能自动替代人工决策。";
        };
    }

    private String resolveCapabilityMaturity(DashboardAiAdviceVO advice) {
        if ("success".equals(advice.getLevel())) {
            return "稳定观察";
        }
        int riskScore = nvlInt(advice.getRiskScore());
        if ("P0".equals(advice.getPriority()) || riskScore >= 90) {
            return "应急处置";
        }
        if ("P1".equals(advice.getPriority()) || riskScore >= 75 || "warning".equals(advice.getLevel())) {
            return "流程固化";
        }
        return "机制优化";
    }

    private String resolveRuleCode(DashboardAiAdviceVO advice) {
        String category = defaultText(advice.getCategory(), "overview").trim().toLowerCase();
        String title = defaultText(advice.getTitle(), "untitled");
        int hash = title.hashCode() & 0x7fffffff;
        return "AI_RULE_" + category.toUpperCase() + "_" + Integer.toHexString(hash).toUpperCase();
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
        sample.setSourceType(defaultText(advice.getSourceType(), "transformer"));
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
                defaultText(advice.getSourceType(), "transformer")
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
            advice.setSourceType("transformer");
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
                addIfNotNull(advices, buildCustomerEmployeeServiceReliabilityAdvice(snapshot));
            }
        }

        if (isOperationVisibility(visibility)) {
            addIfNotNull(advices, buildExecutiveRiskRadarAdvice(snapshot));
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

    private DashboardAiAdviceVO buildCustomerEmployeeServiceReliabilityAdvice(AiBusinessSnapshotVO snapshot) {
        long dueSoon = snapshot.getOrder().getUnshippedDueSoonCount();
        long activeEmployeeCount = snapshot.getEmployee().getActiveEmployeeCount();
        long attendanceExceptionCount = snapshot.getEmployee().getAttendanceExceptionCountToday();
        long missingManagerCount = snapshot.getEmployee().getMissingManagerCount();
        if (dueSoon <= 0 || activeEmployeeCount <= 0 || (attendanceExceptionCount <= 0 && missingManagerCount <= 0)) {
            return null;
        }

        BigDecimal exceptionRatio = BigDecimal.valueOf(attendanceExceptionCount)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(activeEmployeeCount), 2, RoundingMode.HALF_UP);
        BigDecimal customerConcentrationRatio = BigDecimal.ZERO;
        BigDecimal topCustomerAmount = snapshot.getCustomer().getTopCustomerAmount30d();
        BigDecimal salesAmount = snapshot.getOrder().getSalesAmount30d();
        if (topCustomerAmount.compareTo(BigDecimal.ZERO) > 0 && salesAmount.compareTo(BigDecimal.ZERO) > 0) {
            customerConcentrationRatio = topCustomerAmount.multiply(new BigDecimal("100"))
                    .divide(salesAmount, 2, RoundingMode.HALF_UP);
        }

        if (dueSoon < 2
                && exceptionRatio.compareTo(new BigDecimal("5")) < 0
                && missingManagerCount <= 0
                && customerConcentrationRatio.compareTo(new BigDecimal("35")) < 0) {
            return null;
        }

        int concentrationScore = customerConcentrationRatio.compareTo(new BigDecimal("35")) >= 0 ? 12 : 0;
        int riskScore = boundedRiskScore(58
                + (int) Math.min(dueSoon * 5, 20)
                + Math.min(exceptionRatio.intValue(), 18)
                + (int) Math.min(missingManagerCount * 3, 12)
                + concentrationScore);
        DashboardAiAdviceVO advice = baseAdvice("customer", riskScore >= 82 ? "warning" : "info", "support_agent");
        String topCustomerName = defaultText(snapshot.getCustomer().getTopCustomerName30d(), "核心客户");
        advice.setTitle("客户服务可靠性联动评估");
        advice.setSummary(String.format(
                "未来 3 天有 %d 张销售订单尚未发货，今日考勤异常 %d 人次，缺少直属上级配置 %d 人；核心客户 %s 近 30 天贡献占比约 %s%%。",
                dueSoon,
                attendanceExceptionCount,
                missingManagerCount,
                topCustomerName,
                formatNumber(customerConcentrationRatio)
        ));
        advice.setSuggestion("建议不要只按订单催发货，而是把核心客户订单、关键岗位到岗、负责人链路放在一起处理：销售先统一客户沟通口径，生产和仓库确认排程与出库条件，人事同步核对异常人员是否影响关键岗位。");
        advice.setReasoning(String.format(
                "经营快照显示：临期未发货=%d，考勤异常=%d，异常率=%s%%，缺少直属上级=%d，核心客户金额占比=%s%%。该建议由客户价值、交付压力和组织执行稳定性共同生成。",
                dueSoon,
                attendanceExceptionCount,
                formatNumber(exceptionRatio),
                missingManagerCount,
                formatNumber(customerConcentrationRatio)
        ));
        advice.setRoute("/function/order");
        enrichDecisionMetadata(
                advice,
                "服务可靠性",
                riskScore,
                "可能影响核心客户体验、复购信任和订单准时交付",
                "今日内完成跨部门确认",
                "先列出临期订单中的核心客户订单，并确认每单的生产、仓库、人员到岗状态。",
                "核心客户准时发货率、关键岗位到岗率、客户提前沟通完成率"
        );
        advice.setDecisionQuestion("当前风险是单个订单延期，还是核心客户体验与内部执行稳定性同时变弱？");
        advice.setCollaborationPath("销售统一客户口径，生产确认排程，仓库确认出库条件，人事确认关键岗位到岗。");
        advice.setEscalationRule("若核心客户订单今日仍无法给出明确发货时间，由租户负责人介入协调资源。");
        advice.setPreventionAction("建立核心客户订单每日巡检清单，把客户价值、交付日期和关键岗位到岗状态放在同一张表里复盘。");
        return advice;
    }

    private DashboardAiAdviceVO buildExecutiveRiskRadarAdvice(AiBusinessSnapshotVO snapshot) {
        long dueSoon = snapshot.getOrder().getUnshippedDueSoonCount();
        long lowStock = snapshot.getInventory().getLowStockModelCount();
        long badCount = snapshot.getQuality().getBadProductCount30d();
        long pendingBad = snapshot.getQuality().getPendingBadProductCount();
        long attendanceException = snapshot.getEmployee().getAttendanceExceptionCountToday();
        long missingManager = snapshot.getEmployee().getMissingManagerCount();
        long inactiveCustomers = snapshot.getCustomer().getInactiveCustomerCount90d();
        long customerCount = snapshot.getCustomer().getCustomerCount();
        BigDecimal qualityLoss = snapshot.getQuality().getBadProductLoss30d();
        BigDecimal pendingFinanceAmount = snapshot.getFinance().getPendingFinanceAmount();

        int signalCount = 0;
        signalCount += dueSoon > 0 ? 1 : 0;
        signalCount += lowStock > 0 ? 1 : 0;
        signalCount += badCount > 0 || pendingBad > 0 || qualityLoss.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0;
        signalCount += pendingFinanceAmount.compareTo(new BigDecimal("5000")) >= 0 ? 1 : 0;
        signalCount += attendanceException > 0 || missingManager > 0 ? 1 : 0;
        signalCount += customerCount > 0 && inactiveCustomers > Math.max(3, customerCount / 2) ? 1 : 0;
        if (signalCount < 2) {
            return null;
        }

        int qualityScore = (int) Math.min(badCount * 3L + pendingBad * 4L, 16L);
        if (qualityLoss.compareTo(new BigDecimal("5000")) >= 0) {
            qualityScore += 8;
        }
        int financeScore = pendingFinanceAmount.compareTo(new BigDecimal("20000")) >= 0
                ? 14
                : pendingFinanceAmount.compareTo(new BigDecimal("5000")) >= 0 ? 8 : 0;
        int customerScore = customerCount <= 0
                ? 0
                : (inactiveCustomers > Math.max(3, customerCount / 2) ? 10 : 0);
        int riskScore = boundedRiskScore(38
                + (int) Math.min(dueSoon * 7, 21)
                + (int) Math.min(lowStock * 6, 18)
                + qualityScore
                + financeScore
                + (int) Math.min(attendanceException * 4 + missingManager * 2, 16)
                + customerScore);
        if (riskScore < 68) {
            return null;
        }

        DashboardAiAdviceVO advice = baseAdvice("operation", riskScore >= 85 ? "warning" : "info", "radar");
        advice.setTitle("经营风险雷达");
        advice.setSummary(String.format(
                "当前同时出现 %d 类经营信号：临期未发货 %d 单、低库存型号 %d 个、次品记录 %d 条、待处理次品 %d 条、考勤异常 %d 人次、待审批金额约 ￥%s。",
                signalCount,
                dueSoon,
                lowStock,
                badCount,
                pendingBad,
                attendanceException,
                formatNumber(pendingFinanceAmount)
        ));
        advice.setSuggestion("建议管理层用一张风险雷达做早会决策：先判定今天必须关闭的交付风险，再确认库存和人员是否支撑交付，最后把质量损耗和财务审批列入周会复盘，避免各部门只处理自己看到的单点异常。");
        advice.setReasoning(String.format(
                "经营快照显示：临期未发货=%d，低库存型号=%d，次品记录=%d，待处理次品=%d，质量损失=￥%s，待审批金额=￥%s，考勤异常=%d，缺少直属上级=%d，沉睡客户=%d/%d。该建议由多域信号累计生成。",
                dueSoon,
                lowStock,
                badCount,
                pendingBad,
                formatNumber(qualityLoss),
                formatNumber(pendingFinanceAmount),
                attendanceException,
                missingManager,
                inactiveCustomers,
                customerCount
        ));
        advice.setRoute("/dashboard");
        enrichDecisionMetadata(
                advice,
                "高层风险雷达",
                riskScore,
                "可能同时影响交付承诺、库存可用、人员承接、客户复购和经营成本",
                riskScore >= 85 ? "今日经营会确认" : "本周经营会复盘",
                "把交付、库存、质量、员工、财务五类异常按风险分排序，先处理会影响客户承诺的事项。",
                "P0/P1 风险关闭率、跨部门责任人确认率、同类风险复发率"
        );
        advice.setDecisionQuestion("这些异常是各部门独立波动，还是已经形成会影响客户承诺的连锁风险？");
        advice.setCollaborationPath("老板或运营负责人牵头，销售、生产、仓库、质检、人事、财务按风险清单逐项确认责任人。");
        advice.setEscalationRule("若同一天出现交付、库存、人员三类以上风险，或风险分达到 85 分，直接升级到经营负责人处理。");
        advice.setPreventionAction("将风险雷达固定为每日早会第一项，按红黄绿状态复盘昨日未关闭事项。");
        if (riskScore >= 92) {
            advice.setPriority("P0");
        }
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
        advice.setSourceType("legacy_local_rules");
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

    private boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty();
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
