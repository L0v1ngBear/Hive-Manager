package my.hive.shared.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import my.hive.shared.log.OperationLogProperties;
import my.hive.shared.log.SensitiveDataSanitizer;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcSystemEventPublisherTest {

    @Test
    void sanitizesConstraintMessageAtEventPersistenceBoundary() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        SystemEventProperties properties = new SystemEventProperties();
        SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer(
                new ObjectMapper(), new OperationLogProperties());
        JdbcSystemEventPublisher publisher = new JdbcSystemEventPublisher(
                new ObjectMapper(), jdbcTemplate, properties, sanitizer);
        String rawMessage = "Duplicate entry 'TENANT-ORDER-SF123' for key 'uk_order_shipment_tracking'";

        publisher.publish(SystemEvent.builder()
                .eventType("GLOBAL_EXCEPTION")
                .title("Request failed")
                .content(rawMessage)
                .detail(Map.of("errorMessage", rawMessage, "businessMessage", "Order is already closed"))
                .build());

        assertThat(jdbcTemplate.arguments[7]).isEqualTo(SensitiveDataSanitizer.DATA_CONSTRAINT_MESSAGE);
        assertThat(String.valueOf(jdbcTemplate.arguments[11]))
                .doesNotContain("SF123")
                .contains(SensitiveDataSanitizer.DATA_CONSTRAINT_MESSAGE)
                .contains("Order is already closed");
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private Object[] arguments;

        @Override
        public int update(String sql, Object... args) {
            arguments = args;
            return 1;
        }
    }
}
