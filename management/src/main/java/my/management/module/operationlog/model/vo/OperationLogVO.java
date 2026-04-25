package my.management.module.operationlog.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运维日志列表展示对象。
 */
@Data
public class OperationLogVO {

    private Long id;

    private String traceId;

    private String tenantCode;

    private Long userId;

    private String module;

    private String action;

    private String bizType;

    private String bizNo;

    private String description;

    private String logLevel;

    private String requestMethod;

    private String requestUri;

    private String clientIp;

    private String argsJson;

    private String resultJson;

    private Integer success;

    private Integer slow;

    private Long durationMs;

    private String errorType;

    private String errorMessage;

    private LocalDateTime createTime;
}
