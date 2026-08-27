package my.hive.shared.web;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Boundary access logs intentionally contain only routing and timing metadata.
 * Query values and request bodies are never written here because they often
 * include tokens, one-time codes or personal information.
 */
@Data
@Component
@ConfigurationProperties(prefix = "hive.request-access-log")
public class RequestAccessLogProperties {

    private boolean enabled = true;

    private long slowThresholdMs = 1000L;

    private Set<String> excludedPathPrefixes = Set.of("/actuator/health", "/actuator/prometheus");
}
