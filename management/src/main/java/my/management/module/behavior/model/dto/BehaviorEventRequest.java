package my.management.module.behavior.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 前端行为事件上报请求。
 */
@Data
public class BehaviorEventRequest {

    /**
     * 事件类型，例如 page_view、ai_advice_click、notification_open、global_search。
     */
    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    /**
     * 当前页面路径，用于还原用户行为发生的业务场景。
     */
    private String pagePath;

    /**
     * 业务模块，例如 dashboard、inventory、order、ai_advice。
     */
    private String module;

    /**
     * 目标类型，例如 advice、notification、menu、order。
     */
    private String targetType;

    /**
     * 目标标识，建议使用脱敏后的业务编号或系统内部 ID。
     */
    private String targetId;

    /**
     * 具体动作，例如 view、click、search、refresh、open。
     */
    private String action;

    /**
     * 触发来源，例如 navbar、dashboard、ai_center。
     */
    private String source;

    /**
     * 前端会话 ID，用于串联一次浏览过程。
     */
    private String sessionId;

    /**
     * 客户端产生事件的时间。
     */
    private LocalDateTime clientTime;

    /**
     * 可选扩展信息，只允许放低敏业务标签、分类、优先级等元数据。
     */
    private Map<String, Object> metadata;
}
