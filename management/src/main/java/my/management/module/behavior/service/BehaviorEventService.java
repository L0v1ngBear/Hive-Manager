package my.management.module.behavior.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.management.module.behavior.mapper.BehaviorEventMapper;
import my.management.module.behavior.model.dto.BehaviorEventRequest;
import my.management.module.behavior.model.entity.BehaviorEvent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 用户行为采集服务。
 *
 * <p>该服务只沉淀当前租户自己的用户行为，为后续“租户级个性化 AI 建议”提供样本。
 * 行为采集采用白名单字段和长度裁剪，避免把敏感内容或大字段写入数据库。</p>
 */
@Service
public class BehaviorEventService {

    private static final int MAX_BATCH_SIZE = 50;
    private static final int MAX_METADATA_LENGTH = 2000;

    @Resource
    private BehaviorEventMapper behaviorEventMapper;

    @Resource
    private ObjectMapper objectMapper;

    public void batchSave(List<BehaviorEventRequest> requests) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        if (tenantCode == null || tenantCode.isBlank() || userId == null || requests == null || requests.isEmpty()) {
            return;
        }

        List<BehaviorEvent> events = requests.stream()
                .limit(MAX_BATCH_SIZE)
                .filter(item -> item != null && hasText(item.getEventType()))
                .map(item -> toEntity(tenantCode, userId, item))
                .toList();
        if (!events.isEmpty()) {
            behaviorEventMapper.batchInsert(events);
        }
    }

    private BehaviorEvent toEntity(String tenantCode, Long userId, BehaviorEventRequest request) {
        BehaviorEvent event = new BehaviorEvent();
        event.setTenantCode(tenantCode);
        event.setUserId(userId);
        event.setEventType(limit(request.getEventType(), 60));
        event.setPagePath(limit(request.getPagePath(), 255));
        event.setModule(limit(request.getModule(), 60));
        event.setTargetType(limit(request.getTargetType(), 60));
        event.setTargetId(limit(request.getTargetId(), 120));
        event.setAction(limit(request.getAction(), 60));
        event.setSource(limit(request.getSource(), 80));
        event.setSessionId(limit(request.getSessionId(), 80));
        event.setClientTime(request.getClientTime());
        event.setMetadataJson(toSafeMetadataJson(request.getMetadata()));
        return event;
    }

    private String toSafeMetadataJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(metadata);
            return limit(json, MAX_METADATA_LENGTH);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
