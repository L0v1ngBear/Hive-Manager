package my.management.module.ai.service;

import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.management.module.ai.mapper.AiAdviceTrainingSampleMapper;
import my.management.module.ai.model.dto.AiAdviceFeedbackRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * AI 建议反馈服务。
 *
 * <p>反馈不是普通点赞，而是把管理人员的判断沉淀为训练标签，帮助后续模型知道哪些建议值得保留、强化或降权。</p>
 */
@Service
public class AiAdviceFeedbackService {

    private static final int MAX_FEEDBACK_TEXT_LENGTH = 500;
    private static final Map<String, String> LABEL_STATUS_MAP = Map.of(
            "useful", "positive",
            "resolved", "resolved",
            "irrelevant", "negative"
    );

    @Resource
    private AiAdviceTrainingSampleMapper aiAdviceTrainingSampleMapper;

    public void feedback(AiAdviceFeedbackRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        if (tenantCode == null || tenantCode.isBlank() || request == null || isBlank(request.getSampleKey())) {
            return;
        }

        String feedbackType = normalizeFeedbackType(request.getFeedbackType());
        aiAdviceTrainingSampleMapper.updateFeedback(
                tenantCode,
                request.getSampleKey().trim(),
                LABEL_STATUS_MAP.get(feedbackType),
                feedbackType,
                limit(request.getFeedbackText(), MAX_FEEDBACK_TEXT_LENGTH),
                userId
        );
    }

    private String normalizeFeedbackType(String feedbackType) {
        if (feedbackType != null && LABEL_STATUS_MAP.containsKey(feedbackType.trim())) {
            return feedbackType.trim();
        }
        return "useful";
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
