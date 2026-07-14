package my.hive.shared.log;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一操作日志事件。
 * 公共包只定义标准结构，具体落库、写文件、推送日志平台由业务后端自行扩展 Collector。
 */
@Data
public class OperationLogEvent {

    private String traceId;

    private String tenantCode;

    private Long userId;

    private String module;

    private String action;

    private String bizType;

    private String bizNo;

    private String description;

    /**
     * 日志级别：INFO-正常信息，WARN-慢操作或可疑行为，ERROR-异常失败。
     */
    private String logLevel;

    private String className;

    private String methodName;

    private String requestMethod;

    private String requestUri;

    private String clientIp;

    private String userAgent;

    private String argsJson;

    private String resultJson;

    private Boolean success;

    private Boolean slow;

    private Long durationMs;

    private String errorType;

    private String errorMessage;

    private LocalDateTime createTime;
}
