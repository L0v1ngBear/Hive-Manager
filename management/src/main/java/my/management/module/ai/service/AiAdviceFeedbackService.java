package my.management.module.ai.service;

import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.hive.common.redis.HiveRedisKeyBuilder;
import my.hive.common.utils.RedisCacheHelper;
import my.management.module.ai.mapper.AiAdviceTrainingSampleMapper;
import my.management.module.ai.model.dto.AiAdviceFeedbackRequest;
import my.management.module.ai.model.enums.AiFeedbackTypeEnum;
import org.springframework.stereotype.Service;

/**
 * AI 建议反馈服务。
 *
 * <p>反馈不是普通点赞，而是把管理人员的判断沉淀为训练标签，帮助后续模型知道哪些建议值得保留、强化或降权。</p>
 */
@Service
public class AiAdviceFeedbackService {

    private static final int MAX_FEEDBACK_TEXT_LENGTH = 500;

    @Resource
    private AiAdviceTrainingSampleMapper aiAdviceTrainingSampleMapper;

    @Resource
    private AiAdvicePermissionService aiAdvicePermissionService;

    @Resource
    private RedisCacheHelper redisCacheHelper;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

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
            throw new BusinessException(403, "您没有权限反馈该类经营建议");
        }

        String normalizedFeedbackType = normalizeFeedbackType(feedbackType);
        AiFeedbackTypeEnum feedback = AiFeedbackTypeEnum.of(normalizedFeedbackType);
        aiAdviceTrainingSampleMapper.updateFeedback(
                tenantCode,
                normalizedSampleKey,
                feedback.getLabelStatus(),
                normalizedFeedbackType,
                limit(feedbackText, MAX_FEEDBACK_TEXT_LENGTH),
                userId
        );
        clearTenantAdviceCache(tenantCode);
    }

    private void clearTenantAdviceCache(String tenantCode) {
        try {
            redisCacheHelper.deleteByPattern(redisKeyBuilder.cachePattern("management", "dashboard", "ai-advice", tenantCode, "*"));
        } catch (Exception ignored) {
            // 反馈已经写入数据库，缓存清理失败最多延迟 60 秒生效，不影响主流程。
        }
    }

    private String normalizeFeedbackType(String feedbackType) {
        AiFeedbackTypeEnum feedback = AiFeedbackTypeEnum.of(feedbackType);
        return feedback == null ? AiFeedbackTypeEnum.USEFUL.getCode() : feedback.getCode();
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
