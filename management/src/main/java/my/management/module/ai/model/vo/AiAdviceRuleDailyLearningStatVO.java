package my.management.module.ai.model.vo;

import lombok.Data;

/**
 * AI 建议规则级每日学习统计。
 *
 * <p>用于判断候选策略是否连续多天优于当前策略。只有稳定胜出才允许进入灰度候选，
 * 避免某一天的偶然反馈把线上策略带偏。</p>
 */
@Data
public class AiAdviceRuleDailyLearningStatVO {

    private String sampleDay;

    private String category;

    private String title;

    private String sourceType;

    private Long sampleCount = 0L;

    private Long feedbackCount = 0L;

    private Long positiveCount = 0L;

    private Long resolvedCount = 0L;

    private Long negativeCount = 0L;

    private Long ignoredCount = 0L;
}
