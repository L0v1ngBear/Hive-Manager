package my.management.module.behavior.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户行为事件实体。
 *
 * <p>所有行为都按 tenant_code 隔离，后续 AI 个性化建议只读取当前租户自己的行为样本。</p>
 */
@Data
public class BehaviorEvent {

    private Long id;

    private String tenantCode;

    private Long userId;

    private String eventType;

    private String pagePath;

    private String module;

    private String targetType;

    private String targetId;

    private String action;

    private String source;

    private String sessionId;

    private String metadataJson;

    private LocalDateTime clientTime;

    private LocalDateTime createTime;
}
