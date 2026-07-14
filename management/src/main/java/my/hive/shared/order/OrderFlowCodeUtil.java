package my.hive.shared.order;

import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;

/**
 * Order flow codes use an opaque, deterministic signature instead of exposing a raw order id as the scannable value.
 */
public final class OrderFlowCodeUtil {

    private static final String PREFIX = "HIVE_ORDER_FLOW";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String DEFAULT_SECRET = "hive-local-order-flow-secret";
    private static final int MAX_RAW_CODE_LENGTH = 256;
    private static final int MAX_PART_LENGTH = 120;

    private OrderFlowCodeUtil() {
    }

    public static String generateFlowCode(String secret, String tenantCode, String orderType, String orderId) {
        String normalizedTenant = requirePart(tenantCode, "tenantCode");
        String normalizedType = normalizeOrderType(orderType);
        String normalizedOrderId = requirePart(orderId, "orderId");
        String payload = normalizedTenant + ":" + normalizedType + ":" + normalizedOrderId;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(resolveSecret(secret).getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate order flow code", ex);
        }
    }

    public static String buildScanCode(String orderType, String flowCode, String orderId) {
        return PREFIX + ":" + normalizeOrderType(orderType) + ":" + requirePart(flowCode, "flowCode")
                + ":" + requirePart(orderId, "orderId");
    }

    public static Parsed parse(String rawCode) {
        String raw = rawCode == null ? "" : rawCode.trim();
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("flow code is blank");
        }
        if (raw.length() > MAX_RAW_CODE_LENGTH) {
            throw new IllegalArgumentException("flow code is too long");
        }
        if (!raw.startsWith(PREFIX + ":")) {
            throw new IllegalArgumentException("unsupported flow code");
        }
        String[] parts = raw.split(":", 4);
        if (parts.length < 4) {
            throw new IllegalArgumentException("invalid flow code");
        }
        String orderType = normalizeOrderType(parts[1]);
        String flowCode = requirePart(parts[2], "flowCode");
        String orderId = requirePart(parts[3], "orderId");
        return new Parsed(orderType, flowCode, orderId);
    }

    public static boolean matches(String secret, String tenantCode, Parsed parsed) {
        if (parsed == null) {
            return false;
        }
        String expected = generateFlowCode(secret, tenantCode, parsed.orderType(), parsed.orderId());
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                parsed.flowCode().getBytes(StandardCharsets.UTF_8)
        );
    }

    public static String normalizeOrderType(String orderType) {
        String normalized = orderType == null ? "" : orderType.trim().toLowerCase(Locale.ROOT);
        if ("sales".equals(normalized) || "production".equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("unsupported order type");
    }

    private static String requirePart(String value, String name) {
        String normalized = value == null ? "" : value.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException(name + " is blank");
        }
        if (normalized.length() > MAX_PART_LENGTH) {
            throw new IllegalArgumentException(name + " is too long");
        }
        return normalized;
    }

    private static String resolveSecret(String secret) {
        return StringUtils.hasText(secret) ? secret.trim() : DEFAULT_SECRET;
    }

    public record Parsed(String orderType, String flowCode, String orderId) {
    }
}
