package my.hive.shared.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HiveRedisKeyBuilder {

    private static final int MAX_SEGMENT_LENGTH = 96;

    private final HiveRedisKeyProperties properties;

    public String cache(String... segments) {
        return key("cache", segments);
    }

    public String cachePattern(String... segments) {
        return keyPattern("cache", segments);
    }

    public String lock(String... segments) {
        return key("lock", segments);
    }

    public String sequence(String tenantCode, String businessCode, String date) {
        return key("seq", tenantCode, businessCode, date);
    }

    public String counter(String... segments) {
        return key("counter", segments);
    }

    private String key(String scope, String... segments) {
        List<String> parts = baseParts(scope);
        appendSegments(parts, false, segments);
        return String.join(":", parts);
    }

    private String keyPattern(String scope, String... segments) {
        List<String> parts = baseParts(scope);
        appendSegments(parts, true, segments);
        return String.join(":", parts);
    }

    private List<String> baseParts(String scope) {
        List<String> parts = new ArrayList<>();
        parts.add(clean(properties.getNamespace(), false));
        parts.add(clean(properties.getEnvironment(), false));
        parts.add(clean(scope, false));
        return parts;
    }

    private void appendSegments(List<String> parts, boolean allowWildcard, String... segments) {
        if (segments == null) {
            return;
        }
        for (String segment : segments) {
            parts.add(clean(segment, allowWildcard));
        }
    }

    private String clean(String value, boolean allowWildcard) {
        if (value == null || value.isBlank()) {
            return "_";
        }
        String trimmed = value.trim();
        if (allowWildcard && "*".equals(trimmed)) {
            return "*";
        }
        String normalized = trimmed.replaceAll("[^A-Za-z0-9._-]", "_");
        if (normalized.isBlank()) {
            return "_";
        }
        if (normalized.length() <= MAX_SEGMENT_LENGTH) {
            return normalized;
        }
        String hash = Integer.toHexString(normalized.hashCode());
        return normalized.substring(0, 64) + "_" + hash;
    }
}
