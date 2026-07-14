package my.hive.shared.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.hive.shared.event.SystemEvent;
import my.hive.shared.event.SystemEventPublisher;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.redis.HiveRedisKeyBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiGuardService {

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = buildRateLimitScript();

    private final StringRedisTemplate stringRedisTemplate;
    private final HiveRedisKeyBuilder redisKeyBuilder;
    private final ObjectProvider<SystemEventPublisher> systemEventPublisherProvider;

    public void checkRateLimit(String provider, String action, String subject, int maxCalls, Duration window) {
        if (maxCalls <= 0 || window == null || window.isZero() || window.isNegative()) {
            return;
        }
        String key = redisKeyBuilder.counter("external-api", safe(provider), safe(action), safe(subject));
        try {
            Long current = stringRedisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    List.of(key),
                    String.valueOf(Math.max(1L, window.toSeconds()))
            );
            if (current != null && current > maxCalls) {
                recordCallEvent(provider, action, "RATE_LIMITED", subject, null, null,
                        "external api rate limited",
                        Map.of("current", current, "maxCalls", maxCalls, "windowSeconds", window.toSeconds()));
                throw new BusinessException(429, "外部接口调用过于频繁，请稍后再试");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            // Redis must not block core business, but the failure still needs to be visible online.
            log.warn("external api rate limit unavailable, provider={}, action={}, subject={}",
                    provider, action, subject, exception);
        }
    }

    public String getCachedResponse(String provider, String action, String cacheKey) {
        if (isBlank(cacheKey)) {
            return null;
        }
        try {
            String cached = stringRedisTemplate.opsForValue().get(responseCacheKey(provider, action, cacheKey));
            if (cached != null && isObservedProvider(provider)) {
                recordCallEvent(provider, action, "CACHE_HIT", null, null, null,
                        "external api response cache hit", Map.of("cacheKey", fingerprint(cacheKey)));
            }
            return cached;
        } catch (Exception exception) {
            log.warn("external api response cache read failed, provider={}, action={}", provider, action, exception);
            return null;
        }
    }

    public void cacheResponse(String provider, String action, String cacheKey, String responseBody, Duration ttl) {
        if (isBlank(cacheKey) || isBlank(responseBody) || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(responseCacheKey(provider, action, cacheKey), responseBody, ttl);
        } catch (Exception exception) {
            log.warn("external api response cache write failed, provider={}, action={}", provider, action, exception);
        }
    }

    public void evictCachedResponse(String provider, String action, String cacheKey) {
        if (isBlank(cacheKey)) {
            return;
        }
        try {
            stringRedisTemplate.delete(responseCacheKey(provider, action, cacheKey));
        } catch (Exception exception) {
            log.warn("external api response cache evict failed, provider={}, action={}", provider, action, exception);
        }
    }

    public String fingerprint(String rawValue) {
        if (rawValue == null) {
            return "_";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toHexString(rawValue.hashCode());
        }
    }

    public void recordCallEvent(String provider,
                                String action,
                                String status,
                                String subject,
                                Integer httpStatus,
                                Long durationMillis,
                                String message,
                                Map<String, Object> detail) {
        try {
            SystemEventPublisher publisher = systemEventPublisherProvider.getIfAvailable();
            if (publisher == null) {
                return;
            }

            String safeProvider = safe(provider);
            String safeAction = safe(action);
            String safeStatus = safe(status).toUpperCase();
            Map<String, Object> eventDetail = new LinkedHashMap<>();
            eventDetail.put("provider", safeProvider);
            eventDetail.put("action", safeAction);
            eventDetail.put("status", safeStatus);
            if (!isBlank(subject)) {
                eventDetail.put("subject", subject.trim());
            }
            if (httpStatus != null) {
                eventDetail.put("httpStatus", httpStatus);
            }
            if (durationMillis != null) {
                eventDetail.put("durationMillis", durationMillis);
            }
            if (detail != null && !detail.isEmpty()) {
                eventDetail.putAll(detail);
            }

            publisher.publish(SystemEvent.builder()
                    .eventType("EXTERNAL_API_CALL")
                    .level(resolveLevel(safeStatus))
                    .tenantCode(isBlank(subject) ? null : subject.trim())
                    .module("external-api")
                    .title("External API " + safeStatus + ": " + safeProvider + "/" + safeAction)
                    .content(truncate(message, 1000))
                    .bizType(safeProvider)
                    .bizNo(safeAction)
                    .detail(eventDetail)
                    .build());
        } catch (Exception exception) {
            log.warn("external api event publish failed, provider={}, action={}, status={}",
                    provider, action, status, exception);
        }
    }

    private String responseCacheKey(String provider, String action, String cacheKey) {
        return redisKeyBuilder.cache("external-api", safe(provider), safe(action), safe(cacheKey));
    }

    private boolean isObservedProvider(String provider) {
        if (isBlank(provider)) {
            return false;
        }
        String normalized = provider.trim().toLowerCase();
        return normalized.contains("deepseek")
                || normalized.contains("transformer")
                || normalized.contains("wechat")
                || normalized.contains("aliyun")
                || normalized.contains("oss");
    }

    private String resolveLevel(String status) {
        return switch (safe(status).toUpperCase()) {
            case "SUCCESS", "CACHE_HIT" -> "INFO";
            case "RATE_LIMITED", "HTTP_ERROR" -> "WARN";
            default -> "ERROR";
        };
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength));
    }

    private String safe(String value) {
        return isBlank(value) ? "_" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static DefaultRedisScript<Long> buildRateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local current = redis.call('INCR', KEYS[1])
                if current == 1 then
                    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
                end
                return current
                """);
        return script;
    }
}
