package my.management.module.ai.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import my.management.module.ai.model.vo.AiBusinessSnapshotVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Removes tenant and personal identifiers before sending snapshots to external AI providers.
 */
final class AiPayloadSanitizer {

    private static final String REDACTED = "[REDACTED]";

    private AiPayloadSanitizer() {
    }

    static Map<String, Object> sanitizeSnapshot(ObjectMapper objectMapper, AiBusinessSnapshotVO snapshot) {
        if (snapshot == null) {
            return Map.of();
        }
        Map<String, Object> map = objectMapper.convertValue(snapshot, new TypeReference<Map<String, Object>>() {
        });
        Object sanitized = sanitizeValue(map);
        if (sanitized instanceof Map<?, ?> sanitizedMap) {
            Map<String, Object> result = new LinkedHashMap<>();
            sanitizedMap.forEach((key, value) -> {
                if (key != null) {
                    result.put(String.valueOf(key), value);
                }
            });
            result.remove("tenantCode");
            return result;
        }
        return Map.of();
    }

    static String sanitizeFreeText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value
                .replaceAll("1[3-9]\\d{9}", REDACTED)
                .replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", REDACTED);
    }

    private static Object sanitizeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            map.forEach((key, rawValue) -> {
                String keyText = key == null ? "" : String.valueOf(key);
                if (isSensitiveKey(keyText)) {
                    sanitized.put(keyText, REDACTED);
                } else {
                    sanitized.put(keyText, sanitizeValue(rawValue));
                }
            });
            return sanitized;
        }
        if (value instanceof List<?> list) {
            List<Object> sanitized = new ArrayList<>(list.size());
            for (Object item : list) {
                sanitized.add(sanitizeValue(item));
            }
            return sanitized;
        }
        if (value instanceof String text) {
            return sanitizeFreeText(text);
        }
        return value;
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.equals("tenantcode")
                || normalized.contains("phone")
                || normalized.contains("mobile")
                || normalized.contains("address")
                || normalized.contains("contact")
                || normalized.endsWith("name")
                || normalized.contains("username")
                || normalized.contains("customername")
                || normalized.contains("employeename")
                || normalized.contains("receivername")
                || normalized.contains("applicantname");
    }
}
