package my.hive.domain.inventory.service;

import jakarta.annotation.Resource;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.redis.HiveRedisKeyBuilder;
import my.hive.shared.utils.RedisCacheHelper;
import my.hive.domain.inventory.mapper.InventorySettingMapper;
import my.hive.domain.inventory.model.dto.InventoryWarningSettingUpdateRequest;
import my.hive.domain.inventory.model.entity.InventorySetting;
import my.hive.domain.inventory.model.vo.InventoryWarningSettingVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

@Service
public class InventorySettingService {

    public static final BigDecimal DEFAULT_WARNING_THRESHOLD = new BigDecimal("100.00");
    private static final BigDecimal MAX_WARNING_THRESHOLD = new BigDecimal("999999999.99");
    private static final Duration WARNING_THRESHOLD_CACHE_TTL = Duration.ofHours(6);

    @Resource
    private InventorySettingMapper inventorySettingMapper;

    @Resource
    private RedisCacheHelper redisCacheHelper;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public BigDecimal warningThreshold(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return DEFAULT_WARNING_THRESHOLD;
        }
        String cacheKey = warningThresholdCacheKey(tenantCode);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                return normalizeThreshold(new BigDecimal(cached));
            }
        } catch (Exception ignored) {
            try {
                stringRedisTemplate.delete(cacheKey);
            } catch (Exception deleteIgnored) {
            }
        }

        InventorySetting setting = inventorySettingMapper.selectByTenantCode(tenantCode);
        BigDecimal threshold = setting == null || setting.getWarningThresholdMeters() == null
                ? DEFAULT_WARNING_THRESHOLD
                : normalizeThreshold(setting.getWarningThresholdMeters());
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, threshold.toPlainString(), WARNING_THRESHOLD_CACHE_TTL);
        } catch (Exception ignored) {
        }
        return threshold;
    }

    public InventoryWarningSettingVO currentSetting() {
        InventoryWarningSettingVO vo = new InventoryWarningSettingVO();
        vo.setWarningThresholdMeters(warningThreshold(TenantPermissionContext.getTenantCode()));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryWarningSettingVO updateCurrentSetting(InventoryWarningSettingUpdateRequest request) {
        if (request == null) {
            throw new BusinessException("库存预警设置不能为空");
        }
        BigDecimal threshold = normalizeThreshold(request.getWarningThresholdMeters());
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException("当前用户未绑定组织，不能维护库存预警阈值");
        }
        inventorySettingMapper.upsertWarningThreshold(tenantCode, threshold);
        invalidateInventoryWarningCache(tenantCode);
        invalidateDashboardCache(tenantCode);

        InventoryWarningSettingVO vo = new InventoryWarningSettingVO();
        vo.setWarningThresholdMeters(threshold);
        return vo;
    }

    private BigDecimal normalizeThreshold(BigDecimal value) {
        if (value == null) {
            throw new BusinessException("库存预警阈值不能为空");
        }
        BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("库存预警阈值不能小于0");
        }
        if (normalized.compareTo(MAX_WARNING_THRESHOLD) > 0) {
            throw new BusinessException("库存预警阈值不能超过999999999.99");
        }
        return normalized;
    }

    private void invalidateDashboardCache(String tenantCode) {
        redisCacheHelper.deleteByPattern(redisKeyBuilder.cachePattern("management", "dashboard", "overview", tenantCode, "*"));
    }

    private void invalidateInventoryWarningCache(String tenantCode) {
        try {
            stringRedisTemplate.delete(warningThresholdCacheKey(tenantCode));
        } catch (Exception ignored) {
        }
        redisCacheHelper.deleteByPattern(redisKeyBuilder.cachePattern("inventory", "warning", tenantCode, "*"));
    }

    private String warningThresholdCacheKey(String tenantCode) {
        return redisKeyBuilder.cache("inventory", "warning-threshold", tenantCode);
    }
}