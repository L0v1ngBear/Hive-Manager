package my.management.module.ai.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 建议学习统计行。
 *
 * <p>该对象来自训练样本表的聚合结果，用于判断某个租户在不同业务维度上更认可哪些建议，
 * 从而让后续建议排序、置信度和跟进提示逐步贴近该租户的管理习惯。</p>
 */
@Data
public class AiAdviceLearningStatVO {

    private String category;

    private Long sampleCount = 0L;

    private Long feedbackCount = 0L;

    private Long positiveCount = 0L;

    private Long resolvedCount = 0L;

    private Long negativeCount = 0L;

    private Long ignoredCount = 0L;

    private BigDecimal avgConfidence = BigDecimal.ZERO;

    private LocalDateTime latestFeedbackTime;
}
