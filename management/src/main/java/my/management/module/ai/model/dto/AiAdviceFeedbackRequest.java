package my.management.module.ai.model.dto;

import lombok.Data;

/**
 * AI 建议反馈入参。
 *
 * <p>用户对建议的判断会成为训练样本标签，后续用于优化建议排序和行业模型微调。</p>
 */
@Data
public class AiAdviceFeedbackRequest {

    private String sampleKey;

    /**
     * useful：有价值；irrelevant：不准确；ignored：暂不采纳；resolved：已处理。
     */
    private String feedbackType;

    private String feedbackText;
}
