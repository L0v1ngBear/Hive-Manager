package my.hive.shared.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 默认日志收集器：同时支持写应用日志和写 operation_log 表。
 * 数据库写入失败只记录告警，不影响主业务请求。
 */
@Slf4j
@RequiredArgsConstructor
public class Slf4jOperationLogCollector implements OperationLogCollector {

    private static final String INSERT_SQL = """
            INSERT INTO operation_log (
              trace_id, tenant_code, user_id, module, action, biz_type, biz_no, description, log_level,
              class_name, method_name, request_method, request_uri, client_ip, user_agent,
              args_json, result_json, success, slow, duration_ms, error_type, error_message, create_time
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final OperationLogProperties properties;
    private BlockingQueue<OperationLogEvent> queue;
    private ScheduledExecutorService flushExecutor;

    @PostConstruct
    public void init() {
        if (!properties.isCollectToDb() || !properties.isAsyncDb()) {
            return;
        }
        queue = new ArrayBlockingQueue<>(properties.getQueueCapacity());
        flushExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "hive-operation-log-flusher");
            thread.setDaemon(true);
            return thread;
        });
        flushExecutor.scheduleWithFixedDelay(this::flushAsyncLogs,
                properties.getFlushIntervalMs(),
                properties.getFlushIntervalMs(),
                TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void destroy() {
        if (flushExecutor != null) {
            flushExecutor.shutdown();
        }
        flushAsyncLogs();
    }

    @Override
    public void collect(OperationLogEvent event) {
        if (properties.isCollectToDb()) {
            if (properties.isAsyncDb()) {
                publishAsync(event);
            } else {
                collectToDatabase(event);
            }
        }
        if (!properties.isCollectToSlf4j()) {
            return;
        }
        try {
            log.info("HIVE_OPERATION_LOG {}", objectMapper.writeValueAsString(event));
        } catch (Exception ex) {
            log.info("HIVE_OPERATION_LOG traceId={}, level={}, module={}, action={}, success={}, durationMs={}",
                    event.getTraceId(), event.getLogLevel(), event.getModule(), event.getAction(), event.getSuccess(), event.getDurationMs());
        }
    }

    private void publishAsync(OperationLogEvent event) {
        if (useRabbitMq()) {
            publishToRabbitMq(event);
            return;
        }
        offerToQueue(event);
    }

    private boolean useRabbitMq() {
        return "rabbitmq".equalsIgnoreCase(properties.getQueueType());
    }

    private void publishToRabbitMq(OperationLogEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(properties.getRabbitExchange(), properties.getRabbitRoutingKey(), eventJson);
        } catch (Exception ex) {
            log.warn("operation_log 投递 RabbitMQ 失败，traceId={}，降级进入本机内存队列", event.getTraceId(), ex);
            offerToQueue(event);
        }
    }

    private void offerToQueue(OperationLogEvent event) {
        if (queue == null || !queue.offer(event)) {
            log.warn("operation_log 异步队列已满，丢弃日志，traceId={}", event.getTraceId());
        }
    }

    private void flushAsyncLogs() {
        flushQueuedLogs();
    }

    private void flushQueuedLogs() {
        if (queue == null || queue.isEmpty()) {
            return;
        }
        List<OperationLogEvent> batch = new ArrayList<>(properties.getBatchSize());
        queue.drainTo(batch, properties.getBatchSize());
        if (batch.isEmpty()) {
            return;
        }
        collectBatchToDatabase(batch);
    }

    private void collectBatchToDatabase(List<OperationLogEvent> events) {
        try {
            jdbcTemplate.batchUpdate(INSERT_SQL, events, events.size(), (ps, event) -> {
                ps.setObject(1, event.getTraceId());
                ps.setObject(2, event.getTenantCode());
                ps.setObject(3, event.getUserId());
                ps.setObject(4, event.getModule());
                ps.setObject(5, event.getAction());
                ps.setObject(6, event.getBizType());
                ps.setObject(7, event.getBizNo());
                ps.setObject(8, event.getDescription());
                ps.setObject(9, event.getLogLevel());
                ps.setObject(10, event.getClassName());
                ps.setObject(11, event.getMethodName());
                ps.setObject(12, event.getRequestMethod());
                ps.setObject(13, event.getRequestUri());
                ps.setObject(14, event.getClientIp());
                ps.setObject(15, event.getUserAgent());
                ps.setObject(16, event.getArgsJson());
                ps.setObject(17, event.getResultJson());
                ps.setObject(18, Boolean.TRUE.equals(event.getSuccess()) ? 1 : 0);
                ps.setObject(19, Boolean.TRUE.equals(event.getSlow()) ? 1 : 0);
                ps.setObject(20, event.getDurationMs());
                ps.setObject(21, event.getErrorType());
                ps.setObject(22, event.getErrorMessage());
                ps.setObject(23, event.getCreateTime());
            });
        } catch (DataAccessException ex) {
            log.warn("operation_log 批量写入失败，请确认已执行 operation_log.sql，本批数量={}", events.size(), ex);
        } catch (Exception ex) {
            log.warn("operation_log 批量收集异常，本批数量={}", events.size(), ex);
        }
    }

    private void collectToDatabase(OperationLogEvent event) {
        try {
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
        } catch (DataAccessException ex) {
            log.warn("operation_log 写入失败，请确认已执行 operation_log.sql，traceId={}", event.getTraceId(), ex);
        } catch (Exception ex) {
            log.warn("operation_log 收集异常，traceId={}", event.getTraceId(), ex);
        }
    }
}
