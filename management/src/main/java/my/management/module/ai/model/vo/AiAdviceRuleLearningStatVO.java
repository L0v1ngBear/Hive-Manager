package my.management.module.ai.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 建议规则级学习统计。
 *
 * <p>维度级学习会影响一整类建议，规则级学习只影响同标题、同来源的具体建议模式，
 * 用于避免某条建议被频繁负反馈时误伤整个业务维度。</p>
 */
@Data
public class AiAdviceRuleLearningStatVO {

    private String category;

    private String title;

    private String sourceType;

    private Long sampleCount = 0L;

    private Long feedbackCount = 0L;

    private Long positiveCount = 0L;

    private Long resolvedCount = 0L;

    private Long negativeCount = 0L;

    private Long ignoredCount = 0L;

    private BigDecimal avgConfidence = BigDecimal.ZERO;

    private LocalDateTime latestFeedbackTime;
}
