package my.hive.shared.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import jakarta.annotation.PostConstruct;
import my.hive.shared.auth.AuthUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
/**
 * TokenUtil 属于管理端后端通用能力层，提供可复用的工具方法。
 */
@Component
public class TokenUtil {

    public static final String HEADER_RENEWED_TOKEN = "X-Auth-Renewed-Token";
    public static final String HEADER_RENEWED_EXPIRE_AT = "X-Auth-Renewed-Expire-At";
    public static final String HEADER_RENEWED_RESPONSE_KEY = "X-Auth-Renewed-Response-Key";

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final long MIN_RENEW_BEFORE_SECONDS = 60L;
    private static final long MAX_RENEW_BEFORE_SECONDS = 30L * 24L * 60L * 60L;

    @Value("${auth.token.secret:hive-manager-token-secret}")
    private String tokenSecret;

    @Value("${auth.token.expire-hours:24}")
    private Long tokenExpireHours;

    private static String secret;
    private static Long expireHours;
    private static Mac macTemplate;

    @PostConstruct
    public void init() {
        secret = tokenSecret;
        expireHours = tokenExpireHours;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            macTemplate = Mac.getInstance(HMAC_ALGORITHM);
            macTemplate.init(keySpec);
        } catch (Exception ex) {
            throw new IllegalStateException("token util init failed", ex);
        }
    }

    public static String createToken(Long userId, String tenantCode, Long authVersion) {
        if (authVersion == null || authVersion <= 0) {
            throw new IllegalArgumentException("authVersion must be positive");
        }
        long now = Instant.now().getEpochSecond();
        long expireAt = Instant.now().plus(Duration.ofHours(expireHours)).getEpochSecond();

        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
        );
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("tenantCode", tenantCode);
        payload.put("authVersion", authVersion);
        payload.put("iat", now);
        payload.put("exp", expireAt);

        String headerPart = encode(JSON.toJSONString(header));
        String payloadPart = encode(JSON.toJSONString(payload));
        String content = headerPart + "." + payloadPart;
        String signature = sign(content);
        return content + "." + signature;
    }

    public static AuthUserInfo parseToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }

        String content = parts[0] + "." + parts[1];
        if (!Objects.equals(sign(content), parts[2])) {
            return null;
        }

        Map<String, Object> payload = JSON.parseObject(
                new String(URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8),
                new TypeReference<Map<String, Object>>() {}
        );
        if (payload == null) {
            return null;
        }

        Long expireAtValue = toLong(payload.get("exp"));
        Long authVersion = toLong(payload.get("authVersion"));
        if (expireAtValue == null || authVersion == null || authVersion <= 0
                || Instant.now().getEpochSecond() > expireAtValue) {
            return null;
        }

        AuthUserInfo authUserInfo = new AuthUserInfo();
        authUserInfo.setUserId(toLong(payload.get("userId")));
        authUserInfo.setTenantCode((String) payload.get("tenantCode"));
        authUserInfo.setAuthVersion(authVersion);
        authUserInfo.setExpireAt(expireAtValue);
        return authUserInfo;
    }

    public static boolean shouldRenew(AuthUserInfo authUserInfo, long renewBeforeMinutes) {
        if (authUserInfo == null || authUserInfo.getExpireAt() == null) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        long remainingSeconds = authUserInfo.getExpireAt() - now;
        if (remainingSeconds <= 0) {
            return false;
        }
        return remainingSeconds <= normalizeRenewBeforeSeconds(renewBeforeMinutes);
    }

    public static long normalizeRenewBeforeSeconds(long renewBeforeMinutes) {
        long safeMinutes = Math.max(1L, renewBeforeMinutes);
        long seconds;
        try {
            seconds = Math.multiplyExact(safeMinutes, 60L);
        } catch (ArithmeticException ex) {
            seconds = MAX_RENEW_BEFORE_SECONDS;
        }
        long boundedSeconds = Math.min(Math.max(seconds, MIN_RENEW_BEFORE_SECONDS), MAX_RENEW_BEFORE_SECONDS);
        if (expireHours == null || expireHours <= 0) {
            return boundedSeconds;
        }
        long tokenLifetimeSeconds = Duration.ofHours(expireHours).getSeconds();
        long maxRenewWindowSeconds = Math.max(MIN_RENEW_BEFORE_SECONDS, tokenLifetimeSeconds / 2L);
        return Math.min(boundedSeconds, maxRenewWindowSeconds);
    }

    private static String encode(String raw) {
        return URL_ENCODER.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String content) {
        try {
            Mac mac = (Mac) macTemplate.clone();
            byte[] signature = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(signature);
        } catch (CloneNotSupportedException ex) {
            try {
                SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
                Mac mac = Mac.getInstance(HMAC_ALGORITHM);
                mac.init(keySpec);
                return URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception innerEx) {
                throw new IllegalStateException("token sign failed", innerEx);
            }
        }
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
