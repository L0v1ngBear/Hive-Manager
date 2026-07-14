package my.hive.shared.log;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 操作日志采集配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "hive.operation-log")
public class OperationLogProperties {

    /**
     * 总开关。线上建议开启，性能敏感场景可临时关闭。
     */
    private boolean enabled = true;

    /**
     * 默认慢操作阈值。
     */
    private long slowThresholdMs = 1000L;

    /**
     * 单段 JSON 最大长度，避免大对象把日志打爆。
     */
    private int maxPayloadLength = 2000;

    /**
     * 是否写入数据库 operation_log 表，开启后才能真正沉淀日志用于查询。
     */
    private boolean collectToDb = true;

    /**
     * 是否继续输出结构化应用日志，方便容器日志和日志平台采集。
     */
    private boolean collectToSlf4j = true;

    /**
     * 是否异步批量写入数据库。建议线上开启，避免日志表拖慢业务接口。
     */
    private boolean asyncDb = true;

    /**
     * 异步队列类型：rabbitmq-独立消息队列，memory-本机内存队列。
     */
    private String queueType = "rabbitmq";

    /**
     * RabbitMQ 交换机。
     */
    private String rabbitExchange = "hive.operation.log.exchange";

    /**
     * RabbitMQ 路由键。
     */
    private String rabbitRoutingKey = "hive.operation.log";

    /**
     * RabbitMQ 队列。
     */
    private String rabbitQueue = "hive.operation.log.queue";

    /**
     * 本机内存队列容量，仅 queueType=memory 时使用。
     */
    private int queueCapacity = 10000;

    /**
     * 每批最多写入条数。
     */
    private int batchSize = 100;

    /**
     * 后台刷盘间隔，单位毫秒。
     */
    private long flushIntervalMs = 1000L;

    /**
     * 是否开启日志保留清理。线上建议开启，避免 operation_log 无限增长。
     */
    private boolean cleanupEnabled = true;

    /**
     * 运维日志保留天数，超过该天数的历史日志会被定时清理。
     */
    private int retentionDays = 90;

    /**
     * 每次最多清理的日志条数，避免一次大删除拖慢数据库。
     */
    private int cleanupBatchSize = 5000;

    /**
     * 清理任务 Cron，默认每天凌晨 3:20 执行。
     */
    private String cleanupCron = "0 20 3 * * ?";

    /**
     * 需要脱敏的字段名。
     */
    private Set<String> sensitiveKeys = Set.of(
            "password", "oldPassword", "newPassword", "token", "authorization",
            "secret", "authToken", "responseKey", "privateKey", "encryptKey",
            "phone", "mobile", "contactPhone", "customerPhone", "receiverPhone",
            "phoneHash", "phone_hash", "idCard", "idcard", "openId", "openid", "sessionKey"
    );
}
