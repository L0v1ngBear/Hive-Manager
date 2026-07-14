package my.hive.shared.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis 缓存辅助工具。
 * 删除通配符 key 时使用 SCAN 分批扫描，避免生产环境使用 KEYS 阻塞 Redis。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheHelper {

    private static final int DEFAULT_SCAN_COUNT = 500;
    private static final int DEFAULT_DELETE_BATCH_SIZE = 500;

    private final StringRedisTemplate stringRedisTemplate;

    public long deleteByPattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return 0L;
        }
        try {
            return scanAndDelete(pattern.trim());
        } catch (Exception ex) {
            log.warn("Redis 通配符缓存删除失败，pattern={}", pattern, ex);
            return 0L;
        }
    }

    private long scanAndDelete(String pattern) {
        RedisConnection connection = stringRedisTemplate.getConnectionFactory().getConnection();
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(DEFAULT_SCAN_COUNT)
                .build();
        long deletedCount = 0L;
        List<String> batch = new ArrayList<>(DEFAULT_DELETE_BATCH_SIZE);
        try (Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(new String(cursor.next(), StandardCharsets.UTF_8));
                if (batch.size() >= DEFAULT_DELETE_BATCH_SIZE) {
                    deletedCount += deleteBatch(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                deletedCount += deleteBatch(batch);
            }
        } finally {
            connection.close();
        }
        return deletedCount;
    }

    private long deleteBatch(List<String> keys) {
        Long deleted = stringRedisTemplate.delete(keys);
        return deleted == null ? 0L : deleted;
    }
}
