package my.hive.shared.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import my.hive.shared.redis.HiveRedisKeyBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
/**
 * PermissionCacheUtil 属于管理端后端通用能力层，提供可复用的工具方法。
 */
@Component
public class PermissionCacheUtil {

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final int PERMISSION_CATALOG_VERSION = 3;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

    public Set<String> get(String tenantCode, Long userId, Long permissionVersion) {
        try {
            String value = stringRedisTemplate.opsForValue().get(
                    buildManagementKey(tenantCode, userId, permissionVersion));
            if (value == null || value.isBlank()) {
                return null;
            }
            Set<String> result = objectMapper.readValue(value, new TypeReference<LinkedHashSet<String>>() {});
            return result == null ? Collections.emptySet() : result;
        } catch (Exception ex) {
            return null;
        }
    }

    public void put(String tenantCode, Long userId, Long permissionVersion, Set<String> permCodes) {
        try {
            String value = objectMapper.writeValueAsString(permCodes == null ? Collections.emptySet() : permCodes);
            stringRedisTemplate.opsForValue().set(
                    buildManagementKey(tenantCode, userId, permissionVersion), value, CACHE_TTL);
        } catch (Exception ignored) {
        }
    }

    public void evict(String tenantCode, Long userId) {
        if (tenantCode == null || tenantCode.isBlank() || userId == null) {
            return;
        }
        Runnable eviction = () -> {
            try {
                stringRedisTemplate.delete(redisKeyBuilder.cache(
                        "auth", "account-v3", tenantCode, String.valueOf(userId)));
            } catch (Exception ignored) {
                // Database versions remain authoritative when Redis is unavailable.
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eviction.run();
                }
            });
        } else {
            eviction.run();
        }
    }

    private String buildManagementKey(String tenantCode, Long userId, Long permissionVersion) {
        return redisKeyBuilder.cache(
                "management", "perm-v3", tenantCode, String.valueOf(userId),
                String.valueOf(permissionVersion), String.valueOf(PERMISSION_CATALOG_VERSION));
    }
}
