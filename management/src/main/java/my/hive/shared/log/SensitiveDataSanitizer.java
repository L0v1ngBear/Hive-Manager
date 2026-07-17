package my.hive.shared.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 日志脱敏工具，避免密码、密钥、Token 等敏感信息进入日志系统。
 */
@Component
@RequiredArgsConstructor
public class SensitiveDataSanitizer {

    public static final String DATA_CONSTRAINT_MESSAGE = "Data violates a database constraint";
    private static final String MASK = "******";
    private static final Set<String> SENSITIVE_KEY_PARTS = Set.of(
            "password", "passwd", "pwd", "token", "authorization", "secret",
            "responsekey", "privatekey", "encryptkey", "credential", "cookie",
            "openid", "sessionkey", "phone", "mobile", "idcard", "identity", "trackingno"
    );

    private final ObjectMapper objectMapper;
    private final OperationLogProperties properties;

    public String toSafeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            JsonNode node = toSafeTree(value);
            return truncate(objectMapper.writeValueAsString(node));
        } catch (Exception ex) {
            return truncate(toSafeText(String.valueOf(value)));
        }
    }

    public JsonNode toSafeTree(Object value) {
        JsonNode node = objectMapper.valueToTree(value);
        return sanitizeNode(node);
    }

    public String toSafeExceptionMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        if (isDataConstraintViolation(throwable)) {
            return DATA_CONSTRAINT_MESSAGE;
        }
        return toSafeText(throwable.getMessage());
    }

    public String toSafeText(String text) {
        return isDatabaseConstraintMessage(text) ? DATA_CONSTRAINT_MESSAGE : text;
    }

    public boolean isDataConstraintViolation(Throwable throwable) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        while (current != null && visited.add(current)) {
            if (current instanceof DataIntegrityViolationException
                    || current instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            if (current instanceof SQLException sqlException
                    && sqlException.getSQLState() != null
                    && sqlException.getSQLState().startsWith("23")) {
                return true;
            }
            if (isDatabaseConstraintMessage(current.getMessage())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private JsonNode sanitizeNode(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isTextual()) {
            return objectMapper.getNodeFactory().textNode(toSafeText(node.asText()));
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (isSensitive(entry.getKey())) {
                    objectNode.put(entry.getKey(), MASK);
                } else {
                    objectNode.set(entry.getKey(), sanitizeNode(entry.getValue()));
                }
            }
            return objectNode;
        }
        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int index = 0; index < arrayNode.size(); index += 1) {
                arrayNode.set(index, sanitizeNode(arrayNode.get(index)));
            }
        }
        return node;
    }

    private boolean isDatabaseConstraintMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return (normalized.contains("duplicate entry") && normalized.contains("for key"))
                || (normalized.contains("constraint") && normalized.contains("tracking"));
    }

    private boolean isSensitive(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalizedKey = key.replace("_", "")
                .replace("-", "")
                .replace(".", "")
                .toLowerCase(Locale.ROOT);
        if (properties.getSensitiveKeys().stream()
                .map(item -> item.replace("_", "")
                        .replace("-", "")
                        .replace(".", "")
                        .toLowerCase(Locale.ROOT))
                .anyMatch(item -> item.equals(normalizedKey))) {
            return true;
        }
        // 兼容 defaultPassword、tenantToken、contact_phone 等组合字段，避免敏感值换个前缀就漏进日志。
        return SENSITIVE_KEY_PARTS.stream().anyMatch(normalizedKey::contains);
    }

    private String truncate(String text) {
        if (text == null || text.length() <= properties.getMaxPayloadLength()) {
            return text;
        }
        return text.substring(0, properties.getMaxPayloadLength()) + "...[truncated]";
    }
}
