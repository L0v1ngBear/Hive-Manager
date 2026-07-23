package my.hive.shared.log;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * HTTP 接口触发的 Service 层追踪日志配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "hive.service-operation-log")
public class ServiceOperationLogProperties {

    /**
     * 是否记录由 HTTP 请求触发的公共 Service 方法。
     */
    private boolean enabled = true;

    /**
     * 是否额外记录方法开始日志。默认只记录完成或失败，避免日志量翻倍。
     */
    private boolean includeStart = false;

    /**
     * 超过该耗时的成功调用使用 WARN 级别输出。
     */
    private long slowThresholdMs = 1000L;

    /**
     * 失败日志是否包含经过脱敏的异常信息。
     */
    private boolean includeExceptionMessage = true;

    /**
     * 单条异常信息的最大字符数。
     */
    private int maxExceptionMessageLength = 500;
}
