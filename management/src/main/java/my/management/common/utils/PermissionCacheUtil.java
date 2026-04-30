package my.management.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import my.hive.common.redis.HiveRedisKeyBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
/**
 * PermissionCacheUtil 属于管理端后端通用能力层，提供可复用的工具方法。
 */
@Component
public class PermissionCacheUtil {

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

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
        try {
            stringRedisTemplate.delete(List.of(
                    buildManagementKey(tenantCode, userId),
                    buildMiniKey(tenantCode, userId)
            ));
        } catch (Exception ignored) {
        }
    }

    private String buildKey(String tenantCode, Long userId) {
        return buildManagementKey(tenantCode, userId);
    }

    private String buildManagementKey(String tenantCode, Long userId) {
        return redisKeyBuilder.cache("management", "perm", tenantCode, String.valueOf(userId));
    }

    private String buildMiniKey(String tenantCode, Long userId) {
        return redisKeyBuilder.cache("mini", "perm", tenantCode, String.valueOf(userId));
    }
}
