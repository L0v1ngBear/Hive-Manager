package my.hive.shared.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import my.hive.shared.event.JdbcSystemEventPublisher;
import my.hive.shared.event.NoopSystemEventPublisher;
import my.hive.shared.event.SystemEventProperties;
import my.hive.shared.event.SystemEventPublisher;
import my.hive.shared.log.OperationLogCollector;
import my.hive.shared.log.OperationLogProperties;
import my.hive.shared.log.Slf4jOperationLogCollector;
import my.hive.shared.log.SensitiveDataSanitizer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Hive 公共能力自动装配入口。
 * Shared infrastructure configuration owned by the unified backend artifact.
 */
@Configuration
public class HiveCommonAutoConfiguration {

    /**
     * 显式提供默认日志收集器，避免只扫描到日志 AOP、却没有收集器实现导致后端启动失败。
     */
    @Bean
    @ConditionalOnMissingBean(OperationLogCollector.class)
    public OperationLogCollector operationLogCollector(ObjectMapper objectMapper,
                                                       JdbcTemplate jdbcTemplate,
                                                       RabbitTemplate rabbitTemplate,
                                                       OperationLogProperties properties) {
        return new Slf4jOperationLogCollector(objectMapper, jdbcTemplate, rabbitTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean(SystemEventPublisher.class)
    public SystemEventPublisher systemEventPublisher(ObjectMapper objectMapper,
                                                     JdbcTemplate jdbcTemplate,
                                                     SystemEventProperties properties,
                                                     SensitiveDataSanitizer sanitizer) {
        if (!properties.isEnabled()) {
            return new NoopSystemEventPublisher();
        }
        return new JdbcSystemEventPublisher(objectMapper, jdbcTemplate, properties, sanitizer);
    }
}
