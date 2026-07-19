package my.hive.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.redis.HiveRedisKeyBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicAuthRateLimiter {

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = rateLimitScript();

    private final StringRedisTemplate stringRedisTemplate;
    private final HiveRedisKeyBuilder redisKeyBuilder;

    public void check(String flow,
                      String dimension,
                      String subject,
                      int limit,
                      Duration window) {
        if (limit <= 0 || window == null || window.isZero() || window.isNegative()) {
            return;
        }
        String key = redisKeyBuilder.counter(
                "auth", "public", safe(flow), safe(dimension), fingerprint(subject));
        try {
            Long current = stringRedisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    List.of(key),
                    Long.toString(Math.max(1L, window.toSeconds()))
            );
            if (current != null && current > limit) {
                throw new BusinessException(429, "请求过于频繁，请稍后再试");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("public auth rate limit unavailable, flow={}, dimension={}",
                    safe(flow), safe(dimension), exception);
            throw new BusinessException(503, "认证保护服务暂不可用，请稍后再试");
        }
    }

    private String fingerprint(String subject) {
        String value = subject == null ? "_" : subject.trim();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "_" : value.trim();
    }

    private static DefaultRedisScript<Long> rateLimitScript() {
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
