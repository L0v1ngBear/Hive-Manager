package my.hive.shared.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Emits safe diagnostics for external responses.  It records the HTTP outcome,
 * a one-way body fingerprint and the JSON shape; it never writes the response
 * text, credentials, headers or user supplied request parameters.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalApiResponseDiagnosticLogger {

    private final ObjectMapper objectMapper;
    private final ExternalApiDiagnosticsProperties properties;

    public void recordResponse(String provider,
                               String action,
                               int httpStatus,
                               long durationMillis,
                               String responseBody) {
        if (!properties.isEnabled()) {
            return;
        }
        Map<String, Object> summary = summarize(responseBody);
        log.info("HIVE_EXTERNAL_API_RESPONSE provider={}, action={}, httpStatus={}, durationMs={}, bodyFingerprint={}, bodyLength={}, responseShape={}",
                provider, action, httpStatus, durationMillis, summary.get("bodyFingerprint"),
                summary.get("bodyLength"), summary.get("responseShape"));
    }

    public void recordTransportFailure(String provider, String action, long durationMillis, Throwable throwable) {
        if (!properties.isEnabled()) {
            return;
        }
        log.warn("HIVE_EXTERNAL_API_TRANSPORT_FAILURE provider={}, action={}, durationMs={}, exceptionType={}",
                provider, action, durationMillis,
                throwable == null ? "unknown" : throwable.getClass().getSimpleName());
    }

    /** Package-private for regression coverage; returned values contain no body text. */
    Map<String, Object> summarize(String responseBody) {
        Map<String, Object> result = new LinkedHashMap<>();
        String body = responseBody == null ? "" : responseBody;
        result.put("bodyFingerprint", sha256(body));
        result.put("bodyLength", body.length());
        try {
            JsonNode root = objectMapper.readTree(body);
            result.put("responseShape", shape(root));
        } catch (Exception exception) {
            result.put("responseShape", "non-json");
        }
        return result;
    }

    private Object shape(JsonNode node) {
        if (node == null || node.isNull()) {
            return "null";
        }
        if (node.isArray()) {
            return "array(" + node.size() + ")";
        }
        if (!node.isObject()) {
            return node.getNodeType().name().toLowerCase();
        }
        List<String> rootKeys = new ArrayList<>();
        node.fieldNames().forEachRemaining(rootKeys::add);
        rootKeys.sort(Comparator.naturalOrder());
        int keyLimit = Math.max(1, properties.getMaxResponseKeys());
        if (rootKeys.size() > keyLimit) {
            rootKeys = new ArrayList<>(rootKeys.subList(0, keyLimit));
            rootKeys.add("...[truncated]");
        }
        Map<String, Object> shape = new LinkedHashMap<>();
        shape.put("rootKeys", rootKeys);
        for (String key : rootKeys) {
            if (key.startsWith("...")) {
                continue;
            }
            JsonNode child = node.get(key);
            if (child != null && (child.isObject() || child.isArray())) {
                shape.put(key, child.isArray() ? "array(" + child.size() + ")" : objectKeys(child));
            }
        }
        return shape;
    }

    private List<String> objectKeys(JsonNode object) {
        List<String> keys = new ArrayList<>();
        object.fieldNames().forEachRemaining(keys::add);
        keys.sort(Comparator.naturalOrder());
        int keyLimit = Math.max(1, properties.getMaxResponseKeys());
        if (keys.size() > keyLimit) {
            keys = new ArrayList<>(keys.subList(0, keyLimit));
            keys.add("...[truncated]");
        }
        return keys;
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception exception) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
