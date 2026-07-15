package my.hive.shared.security;

import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalUploadUrlValidatorTest {

    @Test
    void storesOnlyCurrentTenantSalesOrderUploadUrls() {
        String stored = InternalUploadUrlValidator.normalizeStoredUploadUrl(
                "/api/uploads/sales-order/TENANT_001/20260515/a.xlsx",
                "/api",
                "TENANT_001",
                "sales-order"
        );

        assertEquals("/api/uploads/sales-order/TENANT_001/20260515/a.xlsx", stored);
    }

    @Test
    void rejectsExternalAndCrossTenantUploadUrls() {
        assertThrows(BusinessException.class, () -> InternalUploadUrlValidator.normalizeStoredUploadUrl(
                "https://evil.example.com/uploads/sales-order/TENANT_001/a.xlsx",
                "/api",
                "TENANT_001",
                "sales-order"
        ));
        assertThrows(BusinessException.class, () -> InternalUploadUrlValidator.normalizeStoredUploadUrl(
                "/api/uploads/sales-order/TENANT_002/a.xlsx",
                "/api",
                "TENANT_001",
                "sales-order"
        ));
    }

    @Test
    void optionalFinanceAttachmentCanBeBlank() {
        assertNull(InternalUploadUrlValidator.normalizeOptionalFinanceAttachment(" ", "TENANT_001"));
    }
}
