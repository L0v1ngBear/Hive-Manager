package my.hive.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.hive.shared.log.OperationLogEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 操作日志 RabbitMQ 消费者。
 * 消费失败会让 RabbitMQ 依据容器策略重试，避免业务线程承担落库开销。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hive.operation-log", name = "queue-type", havingValue = "rabbitmq", matchIfMissing = true)
public class OperationLogRabbitConsumer {

    private static final String INSERT_SQL = """
            INSERT INTO operation_log (
              trace_id, tenant_code, user_id, module, action, biz_type, biz_no, description, log_level,
              class_name, method_name, request_method, request_uri, client_ip, user_agent,
              args_json, result_json, success, slow, duration_ms, error_type, error_message, create_time
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @RabbitListener(queues = "${hive.operation-log.rabbit-queue:hive.operation.log.queue}")
    public void consume(String payload) {
        try {
            OperationLogEvent event = objectMapper.readValue(payload, OperationLogEvent.class);
            jdbcTemplate.update(INSERT_SQL,
                    event.getTraceId(),
                    event.getTenantCode(),
                    event.getUserId(),
                    event.getModule(),
                    event.getAction(),
                    event.getBizType(),
                    event.getBizNo(),
                    event.getDescription(),
                    event.getLogLevel(),
                    event.getClassName(),
                    event.getMethodName(),
                    event.getRequestMethod(),
                    event.getRequestUri(),
                    event.getClientIp(),
                    event.getUserAgent(),
                    event.getArgsJson(),
                    event.getResultJson(),
                    Boolean.TRUE.equals(event.getSuccess()) ? 1 : 0,
                    Boolean.TRUE.equals(event.getSlow()) ? 1 : 0,
                    event.getDurationMs(),
                    event.getErrorType(),
                    event.getErrorMessage(),
                    event.getCreateTime());
        } catch (Exception ex) {
            // 操作日志属于辅助能力，消费失败只告警并确认消息，避免坏消息无限重试拖垮队列。
            log.warn("operation_log RabbitMQ 消费失败，消息已跳过，payload={}", payload, ex);
        }
    }
}
