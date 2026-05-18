package my.management.module.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import my.management.module.ai.model.vo.AiBusinessSnapshotVO;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AiPayloadSanitizerTest {

    @Test
    void removesTenantAndCustomerIdentifiersBeforeExternalAiCalls() {
        AiBusinessSnapshotVO snapshot = new AiBusinessSnapshotVO();
        snapshot.setTenantCode("TENANT_001");
        snapshot.getCustomer().setTopCustomerName30d("Alice Textile 13800138000");

        Map<String, Object> sanitized = AiPayloadSanitizer.sanitizeSnapshot(new ObjectMapper(), snapshot);
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) sanitized.get("customer");

        assertFalse(sanitized.containsKey("tenantCode"));
        assertEquals("[REDACTED]", customer.get("topCustomerName30d"));
    }

    @Test
    void masksPhonesAndEmailsInFeedbackText() {
        String sanitized = AiPayloadSanitizer.sanitizeFreeText("call 13800138000 or a@example.com");

        assertEquals("call [REDACTED] or [REDACTED]", sanitized);
    }
}
