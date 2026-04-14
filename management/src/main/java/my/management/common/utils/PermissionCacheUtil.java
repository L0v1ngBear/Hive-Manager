package my.management.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class PermissionCacheUtil {

    private static final String CACHE_KEY_PREFIX = "management:perm:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    public Set<String> get(String tenantCode, Long userId) {
        try {
            String value = stringRedisTemplate.opsForValue().get(buildKey(tenantCode, userId));
            if (value == null || value.isBlank()) {
                return null;
            }
            Set<String> result = objectMapper.readValue(value, new TypeReference<LinkedHashSet<String>>() {});
            return result == null ? Collections.emptySet() : result;
        } catch (Exception ex) {
            return null;
        }
    }

    public void put(String tenantCode, Long userId, Set<String> permCodes) {
        try {
            String value = objectMapper.writeValueAsString(permCodes == null ? Collections.emptySet() : permCodes);
            stringRedisTemplate.opsForValue().set(buildKey(tenantCode, userId), value, CACHE_TTL);
        } catch (Exception ignored) {
        }
    }

    public void evict(String tenantCode, Long userId) {
        stringRedisTemplate.delete(buildKey(tenantCode, userId));
    }

    private String buildKey(String tenantCode, Long userId) {
        return CACHE_KEY_PREFIX + tenantCode + ":" + userId;
    }
}
