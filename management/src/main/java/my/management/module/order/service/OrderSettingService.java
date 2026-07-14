package my.management.module.order.service;

import jakarta.annotation.Resource;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.redis.HiveRedisKeyBuilder;
import my.hive.shared.utils.RedisCacheHelper;
import my.management.module.order.mapper.OrderSettingMapper;
import my.management.module.order.model.dto.OrderWarningSettingUpdateRequest;
import my.management.module.order.model.entity.OrderSetting;
import my.management.module.order.model.enums.OrderCategoryEnum;
import my.management.module.order.model.vo.OrderWarningSettingVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class OrderSettingService {

    public static final int DEFAULT_STALE_WARNING_DAYS = 3;
    private static final int MIN_STALE_WARNING_DAYS = 1;
    private static final int MAX_STALE_WARNING_DAYS = 365;
    private static final Duration STALE_WARNING_DAYS_CACHE_TTL = Duration.ofHours(6);

    @Resource
    private OrderSettingMapper orderSettingMapper;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

    @Resource
    private RedisCacheHelper redisCacheHelper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public int staleWarningDays(String tenantCode) {
        return settingForTenant(tenantCode).getStaleWarningDays();
    }

    public int staleWarningDays(String tenantCode, String orderCategory) {
        OrderWarningSettingVO setting = settingForTenant(tenantCode);
        if (orderCategory == null || orderCategory.isBlank()) {
            return setting.getStaleWarningDays();
        }
        String normalizedCategory = orderCategory.trim();
        if (OrderCategoryEnum.SAMPLE_ROOM.getCode().equals(normalizedCategory)) {
            return setting.getSampleRoomStaleWarningDays();
        }
        if (OrderCategoryEnum.BULK.getCode().equals(normalizedCategory)) {
            return setting.getBulkStaleWarningDays();
        }
        if (OrderCategoryEnum.REPLENISHMENT.getCode().equals(normalizedCategory)) {
            return setting.getReplenishmentStaleWarningDays();
        }
        if (OrderCategoryEnum.DRAWING_BUDGET.getCode().equals(normalizedCategory)) {
            return setting.getDrawingBudgetStaleWarningDays();
        }
        return setting.getStaleWarningDays();
    }

    public OrderWarningSettingVO currentSetting() {
        return settingForTenant(TenantPermissionContext.getTenantCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderWarningSettingVO updateCurrentSetting(OrderWarningSettingUpdateRequest request) {
        if (request == null) {
            throw new BusinessException("订单预警设置不能为空");
        }
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException("当前用户未绑定组织，不能维护订单预警设置");
        }

        int defaultDays = normalizeDaysOrDefault(request.getStaleWarningDays(), DEFAULT_STALE_WARNING_DAYS);
        int sampleRoomDays = normalizeDaysOrDefault(request.getSampleRoomStaleWarningDays(), defaultDays);
        int bulkDays = normalizeDaysOrDefault(request.getBulkStaleWarningDays(), defaultDays);
        int replenishmentDays = normalizeDaysOrDefault(request.getReplenishmentStaleWarningDays(), defaultDays);
        int drawingBudgetDays = normalizeDaysOrDefault(request.getDrawingBudgetStaleWarningDays(), defaultDays);

        orderSettingMapper.upsertStaleWarningDays(tenantCode, defaultDays, sampleRoomDays, bulkDays,
                replenishmentDays, drawingBudgetDays);
        invalidateOrderWarningCache(tenantCode);

        OrderWarningSettingVO vo = new OrderWarningSettingVO();
        fillSettingVO(vo, defaultDays, sampleRoomDays, bulkDays, replenishmentDays, drawingBudgetDays);
        return vo;
    }

    private OrderWarningSettingVO settingForTenant(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return defaultSettingVO();
        }
        String cacheKey = staleWarningDaysCacheKey(tenantCode);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                OrderWarningSettingVO cachedSetting = parseCachedSetting(cached);
                if (cachedSetting != null) {
                    return cachedSetting;
                }
            }
        } catch (Exception ignored) {
            try {
                stringRedisTemplate.delete(cacheKey);
            } catch (Exception deleteIgnored) {
            }
        }

        OrderSetting setting = orderSettingMapper.selectByTenantCode(tenantCode);
        int days = setting == null
                ? DEFAULT_STALE_WARNING_DAYS
                : normalizeDaysOrDefault(setting.getStaleWarningDays(), DEFAULT_STALE_WARNING_DAYS);
        int sampleRoomDays = setting == null
                ? days
                : normalizeDaysOrDefault(setting.getSampleRoomStaleWarningDays(), days);
        int bulkDays = setting == null
                ? days
                : normalizeDaysOrDefault(setting.getBulkStaleWarningDays(), days);
        int replenishmentDays = setting == null
                ? days
                : normalizeDaysOrDefault(setting.getReplenishmentStaleWarningDays(), days);
        int drawingBudgetDays = setting == null
                ? days
                : normalizeDaysOrDefault(setting.getDrawingBudgetStaleWarningDays(), days);
        OrderWarningSettingVO vo = new OrderWarningSettingVO();
        fillSettingVO(vo, days, sampleRoomDays, bulkDays, replenishmentDays, drawingBudgetDays);
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, serializeSetting(vo), STALE_WARNING_DAYS_CACHE_TTL);
        } catch (Exception ignored) {
        }
        return vo;
    }

    private OrderWarningSettingVO parseCachedSetting(String cached) {
        String[] parts = cached.split("\\|");
        if (parts.length == 1) {
            int days = normalizeDays(Integer.parseInt(parts[0]));
            OrderWarningSettingVO vo = new OrderWarningSettingVO();
            fillSettingVO(vo, days, days, days, days, days);
            return vo;
        }
        if (parts.length < 4) {
            return null;
        }
        OrderWarningSettingVO vo = new OrderWarningSettingVO();
        int days = normalizeDays(Integer.parseInt(parts[0]));
        fillSettingVO(
                vo,
                days,
                normalizeDaysOrDefault(Integer.parseInt(parts[1]), days),
                normalizeDaysOrDefault(Integer.parseInt(parts[2]), days),
                normalizeDaysOrDefault(Integer.parseInt(parts[3]), days),
                normalizeDaysOrDefault(parts.length > 4 ? Integer.parseInt(parts[4]) : days, days)
        );
        return vo;
    }

    private String serializeSetting(OrderWarningSettingVO vo) {
        return vo.getStaleWarningDays() + "|"
                + vo.getSampleRoomStaleWarningDays() + "|"
                + vo.getBulkStaleWarningDays() + "|"
                + vo.getReplenishmentStaleWarningDays() + "|"
                + vo.getDrawingBudgetStaleWarningDays();
    }

    private OrderWarningSettingVO defaultSettingVO() {
        OrderWarningSettingVO vo = new OrderWarningSettingVO();
        fillSettingVO(vo, DEFAULT_STALE_WARNING_DAYS, DEFAULT_STALE_WARNING_DAYS,
                DEFAULT_STALE_WARNING_DAYS, DEFAULT_STALE_WARNING_DAYS, DEFAULT_STALE_WARNING_DAYS);
        return vo;
    }

    private void fillSettingVO(OrderWarningSettingVO vo, int days, int sampleRoomDays, int bulkDays,
                               int replenishmentDays, int drawingBudgetDays) {
        vo.setStaleWarningDays(days);
        vo.setSampleRoomStaleWarningDays(sampleRoomDays);
        vo.setBulkStaleWarningDays(bulkDays);
        vo.setReplenishmentStaleWarningDays(replenishmentDays);
        vo.setDrawingBudgetStaleWarningDays(drawingBudgetDays);
    }

    private int normalizeDaysOrDefault(Integer value, int fallback) {
        return value == null ? normalizeDays(fallback) : normalizeDays(value);
    }

    private int normalizeDays(Integer value) {
        if (value == null) {
            throw new BusinessException("订单未更新预警天数不能为空");
        }
        if (value < MIN_STALE_WARNING_DAYS) {
            throw new BusinessException("订单未更新预警天数不能小于1天");
        }
        if (value > MAX_STALE_WARNING_DAYS) {
            throw new BusinessException("订单未更新预警天数不能超过365天");
        }
        return value;
    }

    private void invalidateOrderWarningCache(String tenantCode) {
        try {
            stringRedisTemplate.delete(staleWarningDaysCacheKey(tenantCode));
            stringRedisTemplate.delete(redisKeyBuilder.cache("order", "stale-warning-days", tenantCode));
        } catch (Exception ignored) {
        }
        redisCacheHelper.deleteByPattern(redisKeyBuilder.cachePattern("order", "warning", tenantCode, "*"));
    }

    private String staleWarningDaysCacheKey(String tenantCode) {
        return redisKeyBuilder.cache("order", "stale-warning-setting", tenantCode);
    }
}
