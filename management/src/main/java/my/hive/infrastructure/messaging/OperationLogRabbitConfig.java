package my.hive.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import my.hive.shared.log.OperationLogProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 操作日志 RabbitMQ 拓扑配置。
 * 使用独立交换机和队列承接日志消息，避免日志流量压到 Redis 缓存。
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hive.operation-log", name = "queue-type", havingValue = "rabbitmq", matchIfMissing = true)
public class OperationLogRabbitConfig {

    private final OperationLogProperties properties;

    @Bean
    public DirectExchange operationLogExchange() {
        return new DirectExchange(properties.getRabbitExchange(), true, false);
    }

    @Bean
    public Queue operationLogQueue() {
        return new Queue(properties.getRabbitQueue(), true);
    }

    @Bean
    public Binding operationLogBinding(Queue operationLogQueue, DirectExchange operationLogExchange) {
        return BindingBuilder.bind(operationLogQueue)
                .to(operationLogExchange)
                .with(properties.getRabbitRoutingKey());
    }
}
