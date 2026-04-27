package my.management.module.ai.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 建议训练样本实体。
 *
 * <p>样本按租户隔离，记录“业务输入 + 行为画像 + 建议输出”，用于后续微调专用纺织行业 AI。</p>
 */
@Data
public class AiAdviceTrainingSample {

    private Long id;

    private String tenantCode;

    private String sampleKey;

    private String category;

    private String title;

    private String sourceType;

    private String priority;

    private Integer confidence;

    private String inputSnapshotJson;

    private String behaviorContextJson;

    private String adviceJson;

    private String labelStatus;

    private String feedbackType;

    private String feedbackText;

    private Long feedbackUserId;

    private LocalDateTime feedbackTime;

    private Integer occurrenceCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
