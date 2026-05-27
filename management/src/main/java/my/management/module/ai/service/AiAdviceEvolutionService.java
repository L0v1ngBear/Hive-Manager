package my.management.module.ai.service;

import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.module.ai.mapper.AiAdviceTrainingSampleMapper;
import my.management.module.ai.model.vo.AiAdviceEvolutionReportVO;
import my.management.module.ai.model.vo.AiAdviceLearningStatVO;
import my.management.module.ai.model.vo.AiAdviceRuleDailyLearningStatVO;
import my.management.module.ai.model.vo.AiAdviceRuleLearningStatVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AI 建议自进化评估服务。
 *
 * <p>只做样本质量、反馈质量和影子评估判断，不自动替换线上建议规则。</p>
 */
@Service
public class AiAdviceEvolutionService {

    private static final int WINDOW_DAYS = 90;
    private static final int MIN_ADAPTIVE_FEEDBACK_COUNT = 10;
    private static final int MIN_ADAPTIVE_QUALITY_SCORE = 65;
    private static final int MIN_SHADOW_FEEDBACK_COUNT = 20;
    private static final int MIN_SHADOW_QUALITY_SCORE = 75;
    private static final int MAX_RULE_SHADOW_COMPARISONS = 12;
    private static final int MAX_RULE_DAILY_ROWS = 1200;
    private static final int MIN_PROMOTION_OBSERVATION_DAYS = 5;
    private static final int MIN_PROMOTION_WIN_DAYS = 4;
    private static final int MIN_PROMOTION_CONSECUTIVE_WIN_DAYS = 3;
    private static final int MIN_ROLLOUT_OBSERVATION_DAYS = 7;
    private static final int DEFAULT_ROLLOUT_TRAFFIC_PERCENT = 5;
    private static final int MAX_ROLLOUT_TRAFFIC_PERCENT = 20;
    private static final int ROLLOUT_ROLLBACK_NEGATIVE_RATE = 35;
    private static final int ROLLOUT_ROLLBACK_LOSS_DAYS = 1;
    private static final int MAX_ROLLOUT_CANDIDATES = 5;
    private static final String CURRENT_STRATEGY_VERSION = "feedback_weighted_v1_online";
    private static final String CANDIDATE_STRATEGY_VERSION = "rule_pattern_weighted_v2_shadow";

    @Resource
    private AiAdviceTrainingSampleMapper aiAdviceTrainingSampleMapper;

    @Resource
    private AiAdvicePermissionService aiAdvicePermissionService;

    public AiAdviceEvolutionReportVO report() {
        if (!aiAdvicePermissionService.hasGlobalPermission()) {
            throw new BusinessException(403, "您没有权限查看经营建议评估");
        }

        AiAdviceEvolutionReportVO report = emptyReport();
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (tenantCode == null || tenantCode.isBlank()) {
            return report;
        }

        List<AiAdviceLearningStatVO> stats;
        try {
            stats = aiAdviceTrainingSampleMapper.selectLearningStats(tenantCode, WINDOW_DAYS);
        } catch (Exception ignored) {
            report.setLearningStage("COLLECTING");
            report.setStageText("训练样本表暂不可用，系统会继续使用本地规则生成建议。");
            report.setShadowEvaluation("暂不进入影子评估，等待训练样本表恢复后再重新计算。");
            return report;
        }

        if (stats == null || stats.isEmpty()) {
            return report;
        }

        for (AiAdviceLearningStatVO stat : stats) {
            accumulate(report, stat);
            AiAdviceEvolutionReportVO.CategoryEvolution categoryEvolution = toCategoryEvolution(stat);
            report.getCategories().add(categoryEvolution);
            report.getShadowComparisons().add(toShadowComparison(categoryEvolution));
        }
        fillRuleShadowComparisons(tenantCode, report);
        report.getCategories().sort(Comparator
                .comparing(AiAdviceEvolutionReportVO.CategoryEvolution::getQualityScore, Comparator.reverseOrder())
                .thenComparing(AiAdviceEvolutionReportVO.CategoryEvolution::getFeedbackCount, Comparator.reverseOrder()));
        report.getShadowComparisons().sort(Comparator
                .comparing(AiAdviceEvolutionReportVO.ShadowStrategyComparison::getDelta, Comparator.reverseOrder())
                .thenComparing(AiAdviceEvolutionReportVO.ShadowStrategyComparison::getCandidateScore, Comparator.reverseOrder()));
        report.getRuleShadowComparisons().sort(Comparator
                .comparing(this::ruleShadowDecisionOrder)
                .thenComparing(item -> Math.abs(nvlInt(item.getDelta())), Comparator.reverseOrder())
                .thenComparing(AiAdviceEvolutionReportVO.ShadowStrategyComparison::getFeedbackCount, Comparator.reverseOrder()));

        fillRolloutGovernance(report);
        fillRatesAndDecision(report);
        return report;
    }

    private AiAdviceEvolutionReportVO emptyReport() {
        AiAdviceEvolutionReportVO report = new AiAdviceEvolutionReportVO();
        report.setGeneratedAt(LocalDateTime.now());
        report.setWindowDays(WINDOW_DAYS);
        report.setLearningStage("COLLECTING");
        report.setStageText("正在积累样本和反馈，当前仅做本地规则建议与人工反馈学习。");
        report.setShadowEvaluation("样本不足时不进入影子评估，避免用少量反馈误导策略。");
        report.setRolloutPolicy("默认关闭自动晋级；新策略必须先在影子评估中连续优于当前规则。");
        report.setRollbackPolicy("任一维度负反馈率过高或闭环率下降时，保持当前稳定规则并人工复盘。");
        report.setGovernanceSummary("当前没有可灰度候选，系统只做观察和反馈学习。");
        report.setAutoPromotionEnabled(false);
        report.setCurrentStrategyVersion(CURRENT_STRATEGY_VERSION);
        report.setCandidateStrategyVersion(CANDIDATE_STRATEGY_VERSION);
        return report;
    }

    private void accumulate(AiAdviceEvolutionReportVO report, AiAdviceLearningStatVO stat) {
        report.setSampleCount(report.getSampleCount() + nvl(stat.getSampleCount()));
        report.setFeedbackCount(report.getFeedbackCount() + nvl(stat.getFeedbackCount()));
        report.setPositiveCount(report.getPositiveCount() + nvl(stat.getPositiveCount()));
        report.setResolvedCount(report.getResolvedCount() + nvl(stat.getResolvedCount()));
        report.setNegativeCount(report.getNegativeCount() + nvl(stat.getNegativeCount()));
        report.setIgnoredCount(report.getIgnoredCount() + nvl(stat.getIgnoredCount()));
    }

    private AiAdviceEvolutionReportVO.CategoryEvolution toCategoryEvolution(AiAdviceLearningStatVO stat) {
        AiAdviceEvolutionReportVO.CategoryEvolution item = new AiAdviceEvolutionReportVO.CategoryEvolution();
        long sampleCount = nvl(stat.getSampleCount());
        long feedbackCount = nvl(stat.getFeedbackCount());
        long positiveCount = nvl(stat.getPositiveCount());
        long resolvedCount = nvl(stat.getResolvedCount());
        long negativeCount = nvl(stat.getNegativeCount());
        long ignoredCount = nvl(stat.getIgnoredCount());

        item.setCategory(defaultText(stat.getCategory(), "overview"));
        item.setSampleCount(sampleCount);
        item.setFeedbackCount(feedbackCount);
        item.setFeedbackCoverageRate(percent(feedbackCount, sampleCount));
        item.setPositiveRate(percent(positiveCount + resolvedCount, feedbackCount));
        item.setResolvedRate(percent(resolvedCount, feedbackCount));
        item.setNegativeRate(percent(negativeCount + ignoredCount, feedbackCount));
        item.setAvgConfidence(toPercentInt(stat.getAvgConfidence()));
        item.setQualityScore(calculateQualityScore(
                sampleCount,
                feedbackCount,
                item.getFeedbackCoverageRate(),
                item.getPositiveRate(),
                item.getResolvedRate(),
                item.getNegativeRate(),
                item.getAvgConfidence()
        ));
        item.setLearningStage(resolveLearningStage(sampleCount, feedbackCount, item.getQualityScore(), item.getNegativeRate()));
        item.setSampleQuality(resolveSampleQuality(item));
        item.setRolloutAction(resolveRolloutAction(item));
        return item;
    }

    private AiAdviceEvolutionReportVO.ShadowStrategyComparison toShadowComparison(AiAdviceEvolutionReportVO.CategoryEvolution item) {
        AiAdviceEvolutionReportVO.ShadowStrategyComparison comparison = new AiAdviceEvolutionReportVO.ShadowStrategyComparison();
        comparison.setCategory(defaultText(item.getCategory(), "overview"));
        comparison.setGranularity("category");
        comparison.setCurrentStrategyVersion(CURRENT_STRATEGY_VERSION);
        comparison.setCandidateStrategyVersion(CANDIDATE_STRATEGY_VERSION);

        int currentScore = calculateCurrentStrategyScore(item);
        int candidateScore = calculateCandidateStrategyScore(item);
        int delta = candidateScore - currentScore;
        comparison.setCurrentScore(currentScore);
        comparison.setCandidateScore(candidateScore);
        comparison.setDelta(delta);
        comparison.setDecision(resolveShadowDecision(item, delta));
        comparison.setReason(resolveShadowReason(item, delta));
        comparison.setSampleCount(item.getSampleCount());
        comparison.setFeedbackCount(item.getFeedbackCount());
        comparison.setPositiveRate(item.getPositiveRate());
        comparison.setNegativeRate(item.getNegativeRate());
        return comparison;
    }

    private void fillRuleShadowComparisons(String tenantCode, AiAdviceEvolutionReportVO report) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return;
        }
        List<AiAdviceRuleLearningStatVO> ruleStats;
        try {
            ruleStats = aiAdviceTrainingSampleMapper.selectRuleLearningStats(
                    tenantCode,
                    WINDOW_DAYS,
                    MAX_RULE_SHADOW_COMPARISONS * 4
            );
        } catch (Exception ignored) {
            return;
        }
        if (ruleStats == null || ruleStats.isEmpty()) {
            return;
        }
        Map<String, RuleStabilityStats> stabilityStats = loadRuleStabilityStats(tenantCode);
        ruleStats.stream()
                .filter(Objects::nonNull)
                .map(this::toRuleShadowComparison)
                .filter(Objects::nonNull)
                .peek(item -> applyRuleStability(item, stabilityStats.get(ruleKey(item.getCategory(), item.getRuleTitle(), item.getSourceType()))))
                .limit(MAX_RULE_SHADOW_COMPARISONS)
                .forEach(report.getRuleShadowComparisons()::add);
    }

    private Map<String, RuleStabilityStats> loadRuleStabilityStats(String tenantCode) {
        Map<String, List<AiAdviceRuleDailyLearningStatVO>> grouped = new HashMap<>();
        List<AiAdviceRuleDailyLearningStatVO> dailyStats;
        try {
            dailyStats = aiAdviceTrainingSampleMapper.selectRuleDailyLearningStats(
                    tenantCode,
                    WINDOW_DAYS,
                    MAX_RULE_DAILY_ROWS
            );
        } catch (Exception ignored) {
            return Map.of();
        }
        if (dailyStats == null || dailyStats.isEmpty()) {
            return Map.of();
        }
        for (AiAdviceRuleDailyLearningStatVO dailyStat : dailyStats) {
            if (dailyStat == null || defaultText(dailyStat.getSampleDay(), "").isBlank()) {
                continue;
            }
            grouped.computeIfAbsent(
                    ruleKey(dailyStat.getCategory(), dailyStat.getTitle(), dailyStat.getSourceType()),
                    ignored -> new ArrayList<>()
            ).add(dailyStat);
        }

        Map<String, RuleStabilityStats> result = new HashMap<>();
        for (Map.Entry<String, List<AiAdviceRuleDailyLearningStatVO>> entry : grouped.entrySet()) {
            result.put(entry.getKey(), calculateRuleStability(entry.getValue()));
        }
        return result;
    }

    private RuleStabilityStats calculateRuleStability(List<AiAdviceRuleDailyLearningStatVO> dailyStats) {
        if (dailyStats == null || dailyStats.isEmpty()) {
            return new RuleStabilityStats(0, 0, 0, 0);
        }
        List<AiAdviceRuleDailyLearningStatVO> sortedStats = dailyStats.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        item -> defaultText(item.getSampleDay(), ""),
                        Comparator.reverseOrder()
                ))
                .toList();
        int observationDays = 0;
        int winDays = 0;
        int lossDays = 0;
        int consecutiveWinDays = 0;
        boolean stillCountingConsecutiveWins = true;
        for (AiAdviceRuleDailyLearningStatVO stat : sortedStats) {
            long sampleCount = nvl(stat.getSampleCount());
            long feedbackCount = nvl(stat.getFeedbackCount());
            if (sampleCount <= 0) {
                continue;
            }
            observationDays++;
            int positiveRate = percent(nvl(stat.getPositiveCount()) + nvl(stat.getResolvedCount()), feedbackCount);
            int resolvedRate = percent(nvl(stat.getResolvedCount()), feedbackCount);
            int negativeRate = percent(nvl(stat.getNegativeCount()) + nvl(stat.getIgnoredCount()), feedbackCount);
            int qualityScore = calculateQualityScore(sampleCount, feedbackCount, 100, positiveRate, resolvedRate, negativeRate, 70);
            int currentScore = calculateRuleCurrentStrategyScore(sampleCount, feedbackCount, qualityScore, 70, negativeRate);
            int candidateScore = calculateRuleCandidateStrategyScore(sampleCount, feedbackCount, qualityScore, positiveRate, resolvedRate, negativeRate, 100);
            int delta = candidateScore - currentScore;
            boolean win = feedbackCount > 0 && delta >= 6 && negativeRate < 40;
            boolean loss = feedbackCount > 0 && (delta <= -6 || negativeRate >= 50);
            if (win) {
                winDays++;
                if (stillCountingConsecutiveWins) {
                    consecutiveWinDays++;
                }
            } else {
                stillCountingConsecutiveWins = false;
            }
            if (loss) {
                lossDays++;
            }
        }
        return new RuleStabilityStats(observationDays, winDays, lossDays, consecutiveWinDays);
    }

    private void applyRuleStability(AiAdviceEvolutionReportVO.ShadowStrategyComparison comparison,
                                    RuleStabilityStats stabilityStats) {
        if (comparison == null) {
            return;
        }
        if (stabilityStats == null) {
            comparison.setObservationDays(0);
            comparison.setWinDays(0);
            comparison.setLossDays(0);
            comparison.setConsecutiveWinDays(0);
            comparison.setPromotionReadiness("继续观察");
            comparison.setPromotionGuardrail("缺少按天稳定性样本，暂不允许进入灰度候选。");
            return;
        }
        comparison.setObservationDays(stabilityStats.observationDays());
        comparison.setWinDays(stabilityStats.winDays());
        comparison.setLossDays(stabilityStats.lossDays());
        comparison.setConsecutiveWinDays(stabilityStats.consecutiveWinDays());

        if ("建议降权".equals(comparison.getDecision()) || nvlInt(comparison.getNegativeRate()) >= 50) {
            comparison.setPromotionReadiness("降权保护");
            comparison.setPromotionGuardrail("负反馈过高，只允许影子层降权观察，不进入升权灰度。");
            return;
        }
        if (stabilityStats.observationDays() < MIN_PROMOTION_OBSERVATION_DAYS) {
            comparison.setPromotionReadiness("观察不足");
            comparison.setPromotionGuardrail("观察天数不足，需要至少 " + MIN_PROMOTION_OBSERVATION_DAYS + " 天稳定样本。");
            return;
        }
        if (stabilityStats.lossDays() > 0) {
            comparison.setPromotionReadiness("存在回撤");
            comparison.setPromotionGuardrail("观察窗口内出现候选策略落后或负反馈偏高，继续保持影子评估。");
            return;
        }
        if (stabilityStats.winDays() >= MIN_PROMOTION_WIN_DAYS
                && stabilityStats.consecutiveWinDays() >= MIN_PROMOTION_CONSECUTIVE_WIN_DAYS
                && ("建议升权".equals(comparison.getDecision()) || "候选优先".equals(comparison.getDecision()))) {
            comparison.setPromotionReadiness("可进入灰度候选");
            comparison.setPromotionGuardrail("候选策略已连续胜出，可由高权限用户确认后进入小流量灰度。");
            return;
        }
        comparison.setPromotionReadiness("继续观察");
        comparison.setPromotionGuardrail("候选策略尚未达到连续胜出门槛，继续只做影子对比。");
    }

    private AiAdviceEvolutionReportVO.ShadowStrategyComparison toRuleShadowComparison(AiAdviceRuleLearningStatVO stat) {
        long sampleCount = nvl(stat.getSampleCount());
        long feedbackCount = nvl(stat.getFeedbackCount());
        long positiveCount = nvl(stat.getPositiveCount());
        long resolvedCount = nvl(stat.getResolvedCount());
        long negativeCount = nvl(stat.getNegativeCount());
        long ignoredCount = nvl(stat.getIgnoredCount());
        int feedbackCoverageRate = percent(feedbackCount, sampleCount);
        int positiveRate = percent(positiveCount + resolvedCount, feedbackCount);
        int resolvedRate = percent(resolvedCount, feedbackCount);
        int negativeRate = percent(negativeCount + ignoredCount, feedbackCount);
        int avgConfidence = toPercentInt(stat.getAvgConfidence());
        int qualityScore = calculateQualityScore(
                sampleCount,
                feedbackCount,
                feedbackCoverageRate,
                positiveRate,
                resolvedRate,
                negativeRate,
                avgConfidence
        );
        int currentScore = calculateRuleCurrentStrategyScore(sampleCount, feedbackCount, qualityScore, avgConfidence, negativeRate);
        int candidateScore = calculateRuleCandidateStrategyScore(sampleCount, feedbackCount, qualityScore, positiveRate, resolvedRate, negativeRate, feedbackCoverageRate);
        int delta = candidateScore - currentScore;

        AiAdviceEvolutionReportVO.ShadowStrategyComparison comparison = new AiAdviceEvolutionReportVO.ShadowStrategyComparison();
        comparison.setGranularity("rule");
        comparison.setCategory(defaultText(stat.getCategory(), "overview"));
        comparison.setRuleTitle(defaultText(stat.getTitle(), "未命名建议"));
        comparison.setSourceType(defaultText(stat.getSourceType(), "transformer"));
        comparison.setCurrentStrategyVersion(CURRENT_STRATEGY_VERSION);
        comparison.setCandidateStrategyVersion(CANDIDATE_STRATEGY_VERSION);
        comparison.setCurrentScore(currentScore);
        comparison.setCandidateScore(candidateScore);
        comparison.setDelta(delta);
        comparison.setSampleCount(sampleCount);
        comparison.setFeedbackCount(feedbackCount);
        comparison.setPositiveRate(positiveRate);
        comparison.setNegativeRate(negativeRate);
        comparison.setDecision(resolveRuleShadowDecision(sampleCount, feedbackCount, positiveRate, negativeRate, qualityScore, delta));
        comparison.setReason(resolveRuleShadowReason(comparison));
        return comparison;
    }

    private void fillRolloutGovernance(AiAdviceEvolutionReportVO report) {
        if (report == null) {
            return;
        }
        report.getRolloutCandidates().clear();
        if (report.getRuleShadowComparisons() == null || report.getRuleShadowComparisons().isEmpty()) {
            report.setRolloutCandidateCount(0);
            return;
        }

        report.getRuleShadowComparisons().stream()
                .filter(Objects::nonNull)
                .filter(item -> "可进入灰度候选".equals(item.getPromotionReadiness()))
                .limit(MAX_ROLLOUT_CANDIDATES)
                .map(this::toRolloutCandidate)
                .filter(Objects::nonNull)
                .forEach(report.getRolloutCandidates()::add);

        int candidateCount = report.getRolloutCandidates().size();
        report.setRolloutCandidateCount(candidateCount);
        if (candidateCount <= 0) {
            return;
        }

        report.setGovernanceSummary("发现 " + candidateCount + " 个可灰度候选，系统仅生成方案，不会自动替换线上规则。");
        report.setRolloutPolicy("灰度必须由高权限用户确认，从 "
                + DEFAULT_ROLLOUT_TRAFFIC_PERCENT
                + "% 小流量开始，至少观察 "
                + MIN_ROLLOUT_OBSERVATION_DAYS
                + " 天，单次最高不超过 "
                + MAX_ROLLOUT_TRAFFIC_PERCENT
                + "%。");
        report.setRollbackPolicy("灰度期间任一候选出现负反馈率 >= "
                + ROLLOUT_ROLLBACK_NEGATIVE_RATE
                + "%、出现 "
                + ROLLOUT_ROLLBACK_LOSS_DAYS
                + " 个落后日或核心指标下滑，立即回退到当前稳定策略。");
    }

    private AiAdviceEvolutionReportVO.RolloutCandidate toRolloutCandidate(
            AiAdviceEvolutionReportVO.ShadowStrategyComparison comparison) {
        if (comparison == null || !"可进入灰度候选".equals(comparison.getPromotionReadiness())) {
            return null;
        }
        AiAdviceEvolutionReportVO.RolloutCandidate candidate = new AiAdviceEvolutionReportVO.RolloutCandidate();
        candidate.setCategory(defaultText(comparison.getCategory(), "overview"));
        candidate.setRuleTitle(defaultText(comparison.getRuleTitle(), "未命名建议"));
        candidate.setSourceType(defaultText(comparison.getSourceType(), "transformer"));
        candidate.setCurrentStrategyVersion(defaultText(comparison.getCurrentStrategyVersion(), CURRENT_STRATEGY_VERSION));
        candidate.setCandidateStrategyVersion(defaultText(comparison.getCandidateStrategyVersion(), CANDIDATE_STRATEGY_VERSION));
        candidate.setSuggestedTrafficPercent(resolveSuggestedTrafficPercent(comparison));
        candidate.setMaxTrafficPercent(MAX_ROLLOUT_TRAFFIC_PERCENT);
        candidate.setMinObservationDays(MIN_ROLLOUT_OBSERVATION_DAYS);
        candidate.setRollbackNegativeRateThreshold(ROLLOUT_ROLLBACK_NEGATIVE_RATE);
        candidate.setRollbackLossDaysThreshold(ROLLOUT_ROLLBACK_LOSS_DAYS);
        candidate.setCurrentScore(nvlInt(comparison.getCurrentScore()));
        candidate.setCandidateScore(nvlInt(comparison.getCandidateScore()));
        candidate.setDelta(nvlInt(comparison.getDelta()));
        candidate.setObservationDays(nvlInt(comparison.getObservationDays()));
        candidate.setConsecutiveWinDays(nvlInt(comparison.getConsecutiveWinDays()));
        candidate.setApprovalRequiredRole("老板 / 经营负责人");
        candidate.setGovernanceStatus("待高权限确认");
        candidate.setRolloutReason("该建议模式已连续胜出，候选策略综合评分比当前策略高 "
                + signedDelta(nvlInt(comparison.getDelta()))
                + "，可进入受控小流量验证。");
        candidate.setRollbackRule("负反馈率达到 "
                + ROLLOUT_ROLLBACK_NEGATIVE_RATE
                + "%、出现落后日或闭环效果下降时，立即回退到 "
                + candidate.getCurrentStrategyVersion()
                + "。");
        candidate.setManualChecklist(List.of(
                "确认该建议不会影响低权限用户看不到的经营敏感信息",
                "确认建议口径、责任部门和闭环动作可执行",
                "确认灰度期间每天检查负反馈、处理率和客户/员工影响",
                "确认出现误导性建议时可立即回退"
        ));
        return candidate;
    }

    private int resolveSuggestedTrafficPercent(AiAdviceEvolutionReportVO.ShadowStrategyComparison comparison) {
        if (comparison == null) {
            return DEFAULT_ROLLOUT_TRAFFIC_PERCENT;
        }
        int delta = nvlInt(comparison.getDelta());
        int negativeRate = nvlInt(comparison.getNegativeRate());
        int observationDays = nvlInt(comparison.getObservationDays());
        int consecutiveWinDays = nvlInt(comparison.getConsecutiveWinDays());
        if (delta >= 15 && negativeRate <= 15 && observationDays >= 10 && consecutiveWinDays >= 5) {
            return 10;
        }
        return DEFAULT_ROLLOUT_TRAFFIC_PERCENT;
    }

    private void fillRatesAndDecision(AiAdviceEvolutionReportVO report) {
        long feedbackCount = nvl(report.getFeedbackCount());
        long sampleCount = nvl(report.getSampleCount());
        report.setFeedbackCoverageRate(percent(feedbackCount, sampleCount));
        report.setPositiveRate(percent(nvl(report.getPositiveCount()) + nvl(report.getResolvedCount()), feedbackCount));
        report.setResolvedRate(percent(nvl(report.getResolvedCount()), feedbackCount));
        report.setNegativeRate(percent(nvl(report.getNegativeCount()) + nvl(report.getIgnoredCount()), feedbackCount));

        int avgCategoryScore = report.getCategories().isEmpty()
                ? 0
                : (int) Math.round(report.getCategories().stream()
                .mapToInt(AiAdviceEvolutionReportVO.CategoryEvolution::getQualityScore)
                .average()
                .orElse(0));
        report.setQualityScore(avgCategoryScore);
        report.setLearningStage(resolveLearningStage(sampleCount, feedbackCount, avgCategoryScore, nvlInt(report.getNegativeRate())));
        if (!"NEED_REVIEW".equals(report.getLearningStage()) && hasPromotionCandidate(report)) {
            report.setLearningStage("PROMOTION_CANDIDATE_READY");
        } else if (!"NEED_REVIEW".equals(report.getLearningStage()) && hasComparableRuleShadow(report)) {
            report.setLearningStage("RULE_SHADOW_EVALUATING");
        }
        report.setStageText(resolveStageText(report.getLearningStage()));
        fillShadowSummary(report);
        report.setShadowEvaluation(resolveShadowEvaluation(report));
    }

    private boolean hasComparableRuleShadow(AiAdviceEvolutionReportVO report) {
        if (report == null || report.getRuleShadowComparisons() == null || report.getRuleShadowComparisons().isEmpty()) {
            return false;
        }
        return report.getRuleShadowComparisons().stream()
                .filter(Objects::nonNull)
                .anyMatch(item -> item.getDecision() != null && !"样本不足".equals(item.getDecision()));
    }

    private boolean hasPromotionCandidate(AiAdviceEvolutionReportVO report) {
        if (report == null || report.getRuleShadowComparisons() == null || report.getRuleShadowComparisons().isEmpty()) {
            return false;
        }
        return report.getRuleShadowComparisons().stream()
                .filter(Objects::nonNull)
                .anyMatch(item -> "可进入灰度候选".equals(item.getPromotionReadiness()));
    }

    private int calculateQualityScore(long sampleCount,
                                      long feedbackCount,
                                      int feedbackCoverageRate,
                                      int positiveRate,
                                      int resolvedRate,
                                      int negativeRate,
                                      int avgConfidence) {
        int sampleScore = (int) Math.min(sampleCount * 2, 20);
        int feedbackScore = (int) Math.min(feedbackCount * 3, 20);
        int coverageScore = Math.min(feedbackCoverageRate, 30);
        int positiveScore = positiveRate * 20 / 100;
        int resolvedScore = resolvedRate * 15 / 100;
        int confidenceScore = avgConfidence * 10 / 100;
        int penalty = negativeRate * 30 / 100;
        return bound(sampleScore + feedbackScore + coverageScore + positiveScore + resolvedScore + confidenceScore - penalty);
    }

    private int calculateCurrentStrategyScore(AiAdviceEvolutionReportVO.CategoryEvolution item) {
        int confidenceScore = nvlInt(item.getAvgConfidence()) * 35 / 100;
        int qualityScore = nvlInt(item.getQualityScore()) * 35 / 100;
        int closureScore = nvlInt(item.getResolvedRate()) * 20 / 100;
        int penalty = nvlInt(item.getNegativeRate()) * 25 / 100;
        int lowFeedbackPenalty = item.getFeedbackCount() == null || item.getFeedbackCount() < 5 ? 10 : 0;
        return bound(confidenceScore + qualityScore + closureScore - penalty - lowFeedbackPenalty + 20);
    }

    private int calculateCandidateStrategyScore(AiAdviceEvolutionReportVO.CategoryEvolution item) {
        int qualityScore = nvlInt(item.getQualityScore()) * 40 / 100;
        int positiveScore = nvlInt(item.getPositiveRate()) * 25 / 100;
        int resolvedScore = nvlInt(item.getResolvedRate()) * 25 / 100;
        int coverageScore = nvlInt(item.getFeedbackCoverageRate()) * 10 / 100;
        int negativePenalty = nvlInt(item.getNegativeRate()) * 35 / 100;
        int samplePenalty = item.getSampleCount() == null || item.getSampleCount() < 20 ? 12 : 0;
        int feedbackPenalty = item.getFeedbackCount() == null || item.getFeedbackCount() < 8 ? 10 : 0;
        return bound(qualityScore + positiveScore + resolvedScore + coverageScore - negativePenalty - samplePenalty - feedbackPenalty + 25);
    }

    private int calculateRuleCurrentStrategyScore(long sampleCount,
                                                  long feedbackCount,
                                                  int qualityScore,
                                                  int avgConfidence,
                                                  int negativeRate) {
        int sampleScore = (int) Math.min(sampleCount * 2, 18);
        int confidenceScore = avgConfidence * 35 / 100;
        int qualityPart = qualityScore * 30 / 100;
        int feedbackPenalty = feedbackCount < 2 ? 12 : 0;
        int negativePenalty = negativeRate * 25 / 100;
        return bound(sampleScore + confidenceScore + qualityPart - feedbackPenalty - negativePenalty + 18);
    }

    private int calculateRuleCandidateStrategyScore(long sampleCount,
                                                    long feedbackCount,
                                                    int qualityScore,
                                                    int positiveRate,
                                                    int resolvedRate,
                                                    int negativeRate,
                                                    int feedbackCoverageRate) {
        int sampleScore = (int) Math.min(sampleCount * 2, 16);
        int qualityPart = qualityScore * 30 / 100;
        int positivePart = positiveRate * 24 / 100;
        int resolvedPart = resolvedRate * 20 / 100;
        int coveragePart = feedbackCoverageRate * 10 / 100;
        int negativePenalty = negativeRate * 42 / 100;
        int sparsePenalty = feedbackCount < 3 ? 12 : 0;
        return bound(sampleScore + qualityPart + positivePart + resolvedPart + coveragePart - negativePenalty - sparsePenalty + 20);
    }

    private void fillShadowSummary(AiAdviceEvolutionReportVO report) {
        if (report.getShadowComparisons() == null || report.getShadowComparisons().isEmpty()) {
            return;
        }
        long comparableCount = report.getShadowComparisons().stream()
                .filter(item -> item.getDecision() != null && !"样本不足".equals(item.getDecision()))
                .count();
        long winCount = report.getShadowComparisons().stream()
                .filter(item -> nvlInt(item.getDelta()) > 0)
                .count();
        int averageDelta = (int) Math.round(report.getShadowComparisons().stream()
                .mapToInt(item -> nvlInt(item.getDelta()))
                .average()
                .orElse(0));
        report.setShadowSampleCount(comparableCount);
        report.setShadowWinRate(percent(winCount, Math.max(comparableCount, 1)));
        report.setShadowAverageDelta(averageDelta);
    }

    private String resolveShadowDecision(AiAdviceEvolutionReportVO.CategoryEvolution item, int delta) {
        if (item.getFeedbackCount() == null || item.getFeedbackCount() < 5 || item.getSampleCount() == null || item.getSampleCount() < 20) {
            return "样本不足";
        }
        if (nvlInt(item.getNegativeRate()) >= 35) {
            return "暂停晋级";
        }
        if (delta >= 8 && nvlInt(item.getQualityScore()) >= MIN_SHADOW_QUALITY_SCORE) {
            return "候选优先";
        }
        if (delta <= -8) {
            return "保持当前";
        }
        return "继续观察";
    }

    private String resolveShadowReason(AiAdviceEvolutionReportVO.CategoryEvolution item, int delta) {
        if ("样本不足".equals(resolveShadowDecision(item, delta))) {
            return "样本量或反馈量不足，不能判断候选策略是否更好。";
        }
        if ("暂停晋级".equals(resolveShadowDecision(item, delta))) {
            return "负反馈率偏高，先复盘样本与规则口径。";
        }
        if (delta >= 8) {
            return "候选策略在反馈质量、闭环率和覆盖率上的综合评分更高，但仍只进入影子验证。";
        }
        if (delta <= -8) {
            return "候选策略没有超过当前稳定规则，继续保持当前线上策略。";
        }
        return "两套策略差异不明显，继续收集反馈后再判断。";
    }

    private String resolveRuleShadowDecision(long sampleCount,
                                             long feedbackCount,
                                             int positiveRate,
                                             int negativeRate,
                                             int qualityScore,
                                             int delta) {
        if (sampleCount < 6 || feedbackCount < 2) {
            return "样本不足";
        }
        if (negativeRate >= 50 && feedbackCount >= 3) {
            return "建议降权";
        }
        if (positiveRate >= 70 && qualityScore >= MIN_ADAPTIVE_QUALITY_SCORE && delta >= 6) {
            return "建议升权";
        }
        if (delta >= 8 && qualityScore >= 60) {
            return "候选优先";
        }
        if (delta <= -8) {
            return "保持当前";
        }
        return "继续观察";
    }

    private String resolveRuleShadowReason(AiAdviceEvolutionReportVO.ShadowStrategyComparison comparison) {
        String title = defaultText(comparison.getRuleTitle(), "该建议");
        String decision = defaultText(comparison.getDecision(), "继续观察");
        if ("样本不足".equals(decision)) {
            return title + " 的样本或反馈还不足，暂不影响线上策略。";
        }
        if ("建议降权".equals(decision)) {
            return title + " 的负反馈率偏高，候选策略会先在影子层降低排序权重，避免继续打扰用户。";
        }
        if ("建议升权".equals(decision)) {
            return title + " 的正向反馈和质量分较好，候选策略会提升该建议的排序权重。";
        }
        if ("候选优先".equals(decision)) {
            return title + " 在规则级反馈评分中优于当前策略，可进入连续影子观察。";
        }
        if ("保持当前".equals(decision)) {
            return title + " 的候选策略暂未超过当前稳定策略，继续沿用当前排序。";
        }
        return title + " 暂未形成明确升降权信号，继续收集反馈和闭环结果。";
    }

    private int ruleShadowDecisionOrder(AiAdviceEvolutionReportVO.ShadowStrategyComparison item) {
        String decision = item == null ? "" : defaultText(item.getDecision(), "");
        return switch (decision) {
            case "建议降权" -> 0;
            case "建议升权" -> 1;
            case "候选优先" -> 2;
            case "保持当前" -> 3;
            case "继续观察" -> 4;
            default -> 5;
        };
    }

    private String ruleKey(String category, String title, String sourceType) {
        return String.join("|",
                defaultText(category, "overview").trim().toLowerCase(),
                defaultText(title, "untitled").trim(),
                defaultText(sourceType, "transformer").trim().toLowerCase()
        );
    }

    private String resolveLearningStage(long sampleCount, long feedbackCount, int qualityScore, int negativeRate) {
        if (feedbackCount >= 5 && negativeRate >= 35) {
            return "NEED_REVIEW";
        }
        if (sampleCount < 20 || feedbackCount < 5) {
            return "COLLECTING";
        }
        if (qualityScore >= MIN_SHADOW_QUALITY_SCORE && feedbackCount >= MIN_SHADOW_FEEDBACK_COUNT) {
            return "SHADOW_READY";
        }
        if (qualityScore >= MIN_ADAPTIVE_QUALITY_SCORE && feedbackCount >= MIN_ADAPTIVE_FEEDBACK_COUNT) {
            return "ADAPTIVE_RANKING";
        }
        if (qualityScore >= 60) {
            return "LEARNING";
        }
        return "NEED_MORE_FEEDBACK";
    }

    private String resolveSampleQuality(AiAdviceEvolutionReportVO.CategoryEvolution item) {
        if ("NEED_REVIEW".equals(item.getLearningStage())) {
            return "负反馈偏高，需要人工复盘建议是否过度提醒或口径不准。";
        }
        if ("SHADOW_READY".equals(item.getLearningStage())) {
            return "样本和反馈质量较好，可进入影子评估。";
        }
        if ("ADAPTIVE_RANKING".equals(item.getLearningStage())) {
            return "反馈已可用于在线排序和置信度校准，但仍需影子评估验证下一代策略。";
        }
        if ("COLLECTING".equals(item.getLearningStage())) {
            return "样本或反馈仍不足，继续积累。";
        }
        if ("LEARNING".equals(item.getLearningStage())) {
            return "反馈方向可用，继续观察闭环效果。";
        }
        return "反馈覆盖不足，需要更多有价值、已处理或不准确标签。";
    }

    private String resolveRolloutAction(AiAdviceEvolutionReportVO.CategoryEvolution item) {
        if ("SHADOW_READY".equals(item.getLearningStage())) {
            return "允许进入影子评估，只比较新旧策略效果，不替换线上规则。";
        }
        if ("ADAPTIVE_RANKING".equals(item.getLearningStage())) {
            return "保持当前反馈加权排序，继续沉淀规则级反馈样本。";
        }
        if ("NEED_REVIEW".equals(item.getLearningStage())) {
            return "暂停策略晋级，先人工复盘负反馈样本。";
        }
        return "继续收集样本和反馈，不进入灰度。";
    }

    private String resolveStageText(String stage) {
        return switch (stage) {
            case "PROMOTION_CANDIDATE_READY" -> "已有具体建议模式连续胜出，可由高权限用户确认后进入小流量灰度。";
            case "RULE_SHADOW_EVALUATING" -> "规则级影子评估已启动，系统正在比较具体建议模式的升权/降权效果。";
            case "SHADOW_READY" -> "已有维度满足影子评估条件，可以离线比较新旧建议策略。";
            case "ADAPTIVE_RANKING" -> "反馈加权排序已启用，系统会按维度和具体建议模式动态校准优先级。";
            case "LEARNING" -> "反馈方向已经可用，但仍需继续积累闭环样本。";
            case "NEED_REVIEW" -> "存在明显负反馈，先复盘样本质量，不进入灰度。";
            case "NEED_MORE_FEEDBACK" -> "样本有了，但有效反馈不足，需要更多人工标注。";
            default -> "正在积累样本和反馈，当前保持稳定规则。";
        };
    }

    private String resolveShadowEvaluation(AiAdviceEvolutionReportVO report) {
        if ("PROMOTION_CANDIDATE_READY".equals(report.getLearningStage())) {
            return "已有候选规则通过连续胜出门槛，但系统仍不会自动替换线上策略；需要高权限用户确认后再做小流量灰度。";
        }
        if ("RULE_SHADOW_EVALUATING".equals(report.getLearningStage())) {
            return "当前已进入规则级影子评估：候选策略会对具体建议模式做升权、降权和保持观察判断，但不会自动替换线上规则。";
        }
        if ("SHADOW_READY".equals(report.getLearningStage())) {
            return "建议下一步开启影子评估：新策略只生成对照结果，连续多天优于当前规则后再考虑灰度。";
        }
        if ("ADAPTIVE_RANKING".equals(report.getLearningStage())) {
            return "当前已进入反馈加权排序阶段：先让反馈影响排序和置信度，暂不自动替换规则；下一步继续积累规则级反馈后再进入影子评估。";
        }
        if ("NEED_REVIEW".equals(report.getLearningStage())) {
            return "暂不影子评估，先复盘负反馈最高的维度，避免把错误反馈固化为策略。";
        }
        return "暂不影子评估，继续收集样本、反馈和闭环结果。";
    }

    private int percent(long numerator, long denominator) {
        if (denominator <= 0 || numerator <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(denominator), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int toPercentInt(BigDecimal value) {
        if (value == null) {
            return 0;
        }
        return bound(value.setScale(0, RoundingMode.HALF_UP).intValue());
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private int nvlInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int bound(int value) {
        return Math.max(0, Math.min(value, 100));
    }

    private String signedDelta(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record RuleStabilityStats(int observationDays, int winDays, int lossDays, int consecutiveWinDays) {
    }
}
