package my.management.module.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import my.hive.common.redis.HiveRedisKeyBuilder;
import my.hive.common.utils.RedisCacheHelper;
import my.management.module.inventory.mapper.ClothMapper;
import my.management.module.inventory.model.vo.InventoryWarningVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class InventoryWarningCacheService {

    private static final Duration WARNING_CACHE_TTL = Duration.ofMinutes(2);
    private static final int SNAPSHOT_LIMIT = 20;

    @Resource
    private ClothMapper clothMapper;

    @Resource
    private InventorySettingService inventorySettingService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

    @Resource
    private RedisCacheHelper redisCacheHelper;

    public long countWarningModels(String tenantCode) {
        return loadSnapshot(tenantCode).count;
    }

    public List<InventoryWarningVO> topWarnings(String tenantCode, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return loadSnapshot(tenantCode).warnings.stream()
                .limit(Math.min(limit, SNAPSHOT_LIMIT))
                .map(this::toWarningVO)
                .toList();
    }

    public void invalidate(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return;
        }
        redisCacheHelper.deleteByPattern(redisKeyBuilder.cachePattern("inventory", "warning", tenantCode, "*"));
    }

    private WarningSnapshot loadSnapshot(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return WarningSnapshot.empty();
        }
        BigDecimal threshold = inventorySettingService.warningThreshold(tenantCode);
        String cacheKey = cacheKey(tenantCode, threshold);

        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                WarningSnapshot snapshot = objectMapper.readValue(cached, WarningSnapshot.class);
                return snapshot == null ? WarningSnapshot.empty() : snapshot.normalized();
            }
        } catch (Exception exception) {
            log.warn("Read inventory warning cache failed, tenantCode={}", tenantCode, exception);
        }

        WarningSnapshot snapshot = querySnapshot(tenantCode, threshold);
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(snapshot), WARNING_CACHE_TTL);
        } catch (Exception exception) {
            log.warn("Write inventory warning cache failed, tenantCode={}", tenantCode, exception);
        }
        return snapshot;
    }

    private WarningSnapshot querySnapshot(String tenantCode, BigDecimal threshold) {
        WarningSnapshot snapshot = new WarningSnapshot();
        Long count = clothMapper.countWarningModels(tenantCode, threshold);
        snapshot.count = count == null ? 0L : count;
        List<InventoryWarningVO> warnings = clothMapper.selectWarnings(tenantCode, threshold, SNAPSHOT_LIMIT);
        snapshot.warnings = warnings == null ? List.of() : warnings.stream().map(this::fromWarningVO).toList();
        return snapshot;
    }

    private WarningItem fromWarningVO(InventoryWarningVO row) {
        WarningItem item = new WarningItem();
        if (row == null) {
            return item;
        }
        item.id = row.getId();
        item.modelCode = row.getModelCode();
        item.totalMeters = row.getTotalMeters();
        item.latestTime = row.getLatestTime();
        return item;
    }

    private InventoryWarningVO toWarningVO(WarningItem item) {
        InventoryWarningVO vo = new InventoryWarningVO();
        if (item == null) {
            return vo;
        }
        vo.setId(item.id);
        vo.setModelCode(item.modelCode);
        vo.setTotalMeters(item.totalMeters == null ? BigDecimal.ZERO : item.totalMeters);
        vo.setLatestTime(item.latestTime);
        return vo;
    }

    private String cacheKey(String tenantCode, BigDecimal threshold) {
        return redisKeyBuilder.cache("inventory", "warning", tenantCode, threshold.stripTrailingZeros().toPlainString());
    }

    public static class WarningSnapshot {
        public long count;
        public List<WarningItem> warnings = List.of();

        static WarningSnapshot empty() {
            return new WarningSnapshot();
        }

        WarningSnapshot normalized() {
            if (warnings == null) {
                warnings = List.of();
            }
            return this;
        }
    }

    public static class WarningItem {
        public Long id;
        public String modelCode;
        public BigDecimal totalMeters;
        public LocalDateTime latestTime;
    }
}
