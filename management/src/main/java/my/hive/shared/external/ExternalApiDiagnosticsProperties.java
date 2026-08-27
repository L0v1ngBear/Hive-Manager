package my.hive.shared.external;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Controls metadata-only diagnostics for outbound HTTP integrations.
 * Response bodies are deliberately never persisted or logged.
 */
@Data
@Component
@ConfigurationProperties(prefix = "hive.external-api-diagnostics")
public class ExternalApiDiagnosticsProperties {

    private boolean enabled = true;

    private int maxResponseKeys = 24;
}
