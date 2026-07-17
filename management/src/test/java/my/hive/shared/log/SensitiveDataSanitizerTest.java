package my.hive.shared.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataSanitizerTest {

    private final SensitiveDataSanitizer sanitizer =
            new SensitiveDataSanitizer(new ObjectMapper(), new OperationLogProperties());

    @Test
    void replacesDatabaseConstraintMessagesButPreservesOrdinaryBusinessMessages() {
        String rawMessage = "Duplicate entry 'TENANT-ORDER-SF123' for key 'uk_order_shipment_tracking'";

        assertThat(sanitizer.toSafeExceptionMessage(new DataIntegrityViolationException(rawMessage)))
                .isEqualTo(SensitiveDataSanitizer.DATA_CONSTRAINT_MESSAGE)
                .doesNotContain("SF123");
        assertThat(sanitizer.toSafeExceptionMessage(new IllegalStateException("Order is already closed")))
                .isEqualTo("Order is already closed");
        assertThat(sanitizer.toSafeJson(Map.of(
                "errorMessage", rawMessage,
                "businessMessage", "Order is already closed")))
                .doesNotContain("SF123")
                .contains(SensitiveDataSanitizer.DATA_CONSTRAINT_MESSAGE)
                .contains("Order is already closed");
    }
}
