package my.hive.shared.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SensitiveDataSanitizerTest {

    private final SensitiveDataSanitizer sanitizer =
            new SensitiveDataSanitizer(new ObjectMapper(), new OperationLogProperties());

    @Test
    void masksInvitationSmsAndProofCredentials() {
        String safe = sanitizer.toSafeJson(Map.of(
                "organizationCode", "JOIN1234",
                "smsCode", "123456",
                "phoneVerificationTicket", "phone-proof-secret",
                "selectionTicket", "selection-secret"
        ));

        assertThat(safe)
                .doesNotContain("JOIN1234", "123456", "phone-proof-secret", "selection-secret")
                .contains("******");
    }

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

    @Test
    void summarizesMultipartFilesWithoutReadingOrSerializingTheirBytes() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getName()).thenReturn("file");
        when(file.getContentType()).thenReturn("video/mp4");
        when(file.getSize()).thenReturn(178_257_920L);
        when(file.isEmpty()).thenReturn(false);

        String safeJson = sanitizer.toSafeJson(new Object[]{file, "TENANT_001"});

        assertThat(safeJson)
                .contains("\"type\":\"multipart-file\"")
                .contains("\"fieldName\":\"file\"")
                .contains("\"contentType\":\"video/mp4\"")
                .contains("\"size\":178257920")
                .doesNotContain("originalFilename")
                .doesNotContain("bytes");
        verify(file, never()).getBytes();
        verify(file, never()).getInputStream();
    }

    @Test
    void replacesRawBinaryValuesWithTheirLengthOnly() {
        assertThat(sanitizer.toSafeJson(new byte[1024]))
                .isEqualTo("\"[binary 1024 bytes omitted]\"");
    }
}
