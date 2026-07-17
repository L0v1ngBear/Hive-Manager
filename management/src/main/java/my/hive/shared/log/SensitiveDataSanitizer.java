package my.hive.shared.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
            JsonNode node = objectMapper.valueToTree(value);
            mask(node);
            return truncate(objectMapper.writeValueAsString(node));
        } catch (Exception ex) {
            return truncate(String.valueOf(value));
        }
    }

    private void mask(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (isSensitive(entry.getKey())) {
                    objectNode.put(entry.getKey(), MASK);
                } else {
                    mask(entry.getValue());
                }
            }
            return;
        }
        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            arrayNode.forEach(this::mask);
        }
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
