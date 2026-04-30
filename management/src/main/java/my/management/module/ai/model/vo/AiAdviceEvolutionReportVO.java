package my.management.module.ai.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 建议自进化评估报告。
 *
 * <p>该报告只描述样本、反馈和影子评估结果，不直接改变线上规则，避免 AI 未经验证自动影响业务决策。</p>
 */
@Data
public class AiAdviceEvolutionReportVO {

    private LocalDateTime generatedAt;

    private Integer windowDays = 90;

    private Long sampleCount = 0L;

    private Long feedbackCount = 0L;

    private Long positiveCount = 0L;

    private Long resolvedCount = 0L;

    private Long negativeCount = 0L;

    private Long ignoredCount = 0L;

    private Integer feedbackCoverageRate = 0;

    private Integer positiveRate = 0;

    private Integer resolvedRate = 0;

    private Integer negativeRate = 0;

    private Integer qualityScore = 0;

    private String learningStage;

    private String stageText;

    private String shadowEvaluation;

    private String rolloutPolicy;

    private String rollbackPolicy;

    private Boolean autoPromotionEnabled = false;

    private String currentStrategyVersion;

    private String candidateStrategyVersion;

    private Long shadowSampleCount = 0L;

    private Integer shadowWinRate = 0;

    private Integer shadowAverageDelta = 0;

    private Integer rolloutCandidateCount = 0;

    private String governanceSummary;

    private List<CategoryEvolution> categories = new ArrayList<>();

    private List<ShadowStrategyComparison> shadowComparisons = new ArrayList<>();

    private List<ShadowStrategyComparison> ruleShadowComparisons = new ArrayList<>();

    private List<RolloutCandidate> rolloutCandidates = new ArrayList<>();

    @Data
    public static class CategoryEvolution {

        private String category;

        private Long sampleCount = 0L;

        private Long feedbackCount = 0L;

        private Integer feedbackCoverageRate = 0;

        private Integer positiveRate = 0;

        private Integer resolvedRate = 0;

        private Integer negativeRate = 0;

        private Integer avgConfidence = 0;

        private Integer qualityScore = 0;

        private String learningStage;

        private String sampleQuality;

        private String rolloutAction;
    }

    @Data
    public static class ShadowStrategyComparison {

        private String category;

        private String ruleTitle;

        private String sourceType;

        private String granularity;

        private String currentStrategyVersion;

        private String candidateStrategyVersion;

        private Integer currentScore = 0;

        private Integer candidateScore = 0;

        private Integer delta = 0;

        private String decision;

        private String reason;

        private Long sampleCount = 0L;

        private Long feedbackCount = 0L;

        private Integer positiveRate = 0;

        private Integer negativeRate = 0;

        private Integer observationDays = 0;

        private Integer winDays = 0;

        private Integer lossDays = 0;

        private Integer consecutiveWinDays = 0;

        private String promotionReadiness;

        private String promotionGuardrail;
    }

    @Data
    public static class RolloutCandidate {

        private String category;

        private String ruleTitle;

        private String sourceType;

        private String currentStrategyVersion;

        private String candidateStrategyVersion;

        private Integer suggestedTrafficPercent = 5;

        private Integer maxTrafficPercent = 20;

        private Integer minObservationDays = 7;

        private Integer rollbackNegativeRateThreshold = 35;

        private Integer rollbackLossDaysThreshold = 1;

        private Integer currentScore = 0;

        private Integer candidateScore = 0;

        private Integer delta = 0;

        private Integer observationDays = 0;

        private Integer consecutiveWinDays = 0;

        private String approvalRequiredRole;

        private String governanceStatus;

        private String rolloutReason;

        private String rollbackRule;

        private List<String> manualChecklist = new ArrayList<>();
    }
}
