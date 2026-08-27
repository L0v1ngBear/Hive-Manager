package my.hive.shared.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalApiResponseDiagnosticLoggerTest {

    @Test
    void summarizesJsonStructureWithoutRetainingSensitiveValues() {
        ExternalApiResponseDiagnosticLogger logger = new ExternalApiResponseDiagnosticLogger(
                new ObjectMapper(), new ExternalApiDiagnosticsProperties());
        String body = "{\"success\":false,\"message\":\"customer mobile 13800000000\",\"data\":{\"token\":\"secret\",\"trace\":[]}}";

        Map<String, Object> summary = logger.summarize(body);

        assertThat(summary).containsKeys("bodyFingerprint", "bodyLength", "responseShape");
        assertThat(String.valueOf(summary)).doesNotContain("13800000000", "secret");
        assertThat(String.valueOf(summary.get("responseShape"))).contains("rootKeys", "data", "trace", "token");
    }

    @Test
    void marksNonJsonPayloadWithoutLoggingItsText() {
        ExternalApiResponseDiagnosticLogger logger = new ExternalApiResponseDiagnosticLogger(
                new ObjectMapper(), new ExternalApiDiagnosticsProperties());

        Map<String, Object> summary = logger.summarize("provider returned token=secret-value");

        assertThat(summary.get("responseShape")).isEqualTo("non-json");
        assertThat(String.valueOf(summary)).doesNotContain("secret-value");
    }
}
