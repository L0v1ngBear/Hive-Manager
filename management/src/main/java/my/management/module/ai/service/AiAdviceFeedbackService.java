package my.management.module.ai.service;

import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.module.ai.mapper.AiAdviceTrainingSampleMapper;
import my.management.module.ai.model.dto.AiAdviceFeedbackRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * AI 建议反馈服务。
 *
 * <p>反馈不是普通点赞，而是把管理人员的判断沉淀为训练标签，帮助后续模型知道哪些建议值得保留、强化或降权。</p>
 */
@Service
public class AiAdviceFeedbackService {

    private static final int MAX_FEEDBACK_TEXT_LENGTH = 500;
    private static final String AI_ADVICE_CACHE_KEY_PREFIX = "management:dashboard:ai-advice:";
    private static final Map<String, String> LABEL_STATUS_MAP = Map.of(
            "useful", "positive",
            "resolved", "resolved",
            "irrelevant", "negative",
            "ignored", "ignored"
    );

    @Resource
    private AiAdviceTrainingSampleMapper aiAdviceTrainingSampleMapper;

    @Resource
    private AiAdvicePermissionService aiAdvicePermissionService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void feedback(AiAdviceFeedbackRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        if (tenantCode == null || tenantCode.isBlank() || request == null || isBlank(request.getSampleKey())) {
            return;
        }

        feedbackBySampleKey(tenantCode, userId, request.getSampleKey(), request.getFeedbackType(), request.getFeedbackText());
    }

    public void feedbackBySampleKey(String tenantCode,
                                    Long userId,
                                    String sampleKey,
                                    String feedbackType,
                                    String feedbackText) {
        if (tenantCode == null || tenantCode.isBlank() || isBlank(sampleKey)) {
            return;
        }

        aiAdvicePermissionService.requireAnyView();
        String normalizedSampleKey = sampleKey.trim();
        String category = aiAdviceTrainingSampleMapper.selectCategoryBySampleKey(tenantCode, normalizedSampleKey);
        if (category != null && !aiAdvicePermissionService.canViewCategory(category)) {
            throw new BusinessException(403, "您没有权限反馈该维度 AI 建议");
        }

        String normalizedFeedbackType = normalizeFeedbackType(feedbackType);
        aiAdviceTrainingSampleMapper.updateFeedback(
                tenantCode,
                normalizedSampleKey,
                LABEL_STATUS_MAP.get(normalizedFeedbackType),
                normalizedFeedbackType,
                limit(feedbackText, MAX_FEEDBACK_TEXT_LENGTH),
                userId
        );
        clearTenantAdviceCache(tenantCode);
    }

    private void clearTenantAdviceCache(String tenantCode) {
        try {
            Set<String> keys = stringRedisTemplate.keys(AI_ADVICE_CACHE_KEY_PREFIX + tenantCode + ":*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception ignored) {
            // 反馈已经写入数据库，缓存清理失败最多延迟 60 秒生效，不影响主流程。
        }
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
