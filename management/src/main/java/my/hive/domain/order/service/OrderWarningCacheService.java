package my.hive.domain.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import my.hive.shared.redis.HiveRedisKeyBuilder;
import my.hive.shared.utils.RedisCacheHelper;
import my.hive.domain.order.mapper.SalesOrderMapper;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.enums.OrderCategoryEnum;
import my.hive.domain.order.model.enums.OrderStatusEnum;
import my.hive.domain.order.model.vo.OrderWarningSummaryVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

@Service
@Slf4j
public class OrderWarningCacheService {

    private static final Duration WARNING_CACHE_TTL = Duration.ofMinutes(2);
    private static final String CACHE_VERSION = "unified-v2";

    @Resource
    private SalesOrderMapper salesOrderMapper;

    @Resource
    private OrderSettingService orderSettingService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

    @Resource
    private RedisCacheHelper redisCacheHelper;

    public OrderWarningSummaryVO summary(String tenantCode) {
        return summary(tenantCode, null);
    }

    public OrderWarningSummaryVO summary(String tenantCode, Set<String> permittedStatuses) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return emptySummary(OrderSettingService.DEFAULT_STALE_WARNING_DAYS);
        }
        int days = orderSettingService.staleWarningDays(tenantCode);
        int sampleRoomDays = orderSettingService.staleWarningDays(tenantCode, OrderCategoryEnum.SAMPLE_ROOM.getCode());
        int bulkDays = orderSettingService.staleWarningDays(tenantCode, OrderCategoryEnum.BULK.getCode());
        int replenishmentDays = orderSettingService.staleWarningDays(tenantCode, OrderCategoryEnum.REPLENISHMENT.getCode());
        int drawingBudgetDays = orderSettingService.staleWarningDays(tenantCode, OrderCategoryEnum.DRAWING_BUDGET.getCode());
        Set<String> statusScope = normalizeStatusScope(permittedStatuses);
        if (statusScope != null && statusScope.isEmpty()) {
            return emptySummary(days, sampleRoomDays, bulkDays, replenishmentDays, drawingBudgetDays);
        }
        String cacheKey = cacheKey(tenantCode, days, sampleRoomDays, bulkDays, replenishmentDays,
                drawingBudgetDays, statusScope);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                OrderWarningSummaryVO summary = objectMapper.readValue(cached, OrderWarningSummaryVO.class);
                return summary == null ? emptySummary(days) : normalize(summary, days, sampleRoomDays,
                        bulkDays, replenishmentDays, drawingBudgetDays);
            }
        } catch (Exception exception) {
            log.warn("Read order warning cache failed, tenantCode={}", tenantCode, exception);
        }

        OrderWarningSummaryVO summary = querySummary(tenantCode, days, sampleRoomDays, bulkDays,
                replenishmentDays, drawingBudgetDays, statusScope);
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(summary), WARNING_CACHE_TTL);
        } catch (Exception exception) {
            log.warn("Write order warning cache failed, tenantCode={}", tenantCode, exception);
        }
        return summary;
    }

    public void invalidate(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return;
        }
        redisCacheHelper.deleteByPattern(redisKeyBuilder.cachePattern("order", "warning", tenantCode, "*"));
    }

    private OrderWarningSummaryVO querySummary(String tenantCode,
                                               int days,
                                               int sampleRoomDays,
                                               int bulkDays,
                                               int replenishmentDays,
                                               int drawingBudgetDays,
                                               Set<String> statusScope) {
        long sampleRoomCount = countOrdersByCategory(tenantCode, OrderCategoryEnum.SAMPLE_ROOM.getCode(), sampleRoomDays, statusScope);
        long bulkCount = countOrdersByCategory(tenantCode, OrderCategoryEnum.BULK.getCode(), bulkDays, statusScope);
        long replenishmentCount = countOrdersByCategory(tenantCode, OrderCategoryEnum.REPLENISHMENT.getCode(), replenishmentDays, statusScope);
        long drawingBudgetCount = countOrdersByCategory(tenantCode, OrderCategoryEnum.DRAWING_BUDGET.getCode(), drawingBudgetDays, statusScope);
        long otherCount = countOtherOrders(tenantCode, days, statusScope);
        long orderCount = sampleRoomCount + bulkCount + replenishmentCount + drawingBudgetCount + otherCount;

        OrderWarningSummaryVO summary = new OrderWarningSummaryVO();
        summary.setStaleWarningDays(days);
        summary.setSampleRoomStaleWarningDays(sampleRoomDays);
        summary.setBulkStaleWarningDays(bulkDays);
        summary.setReplenishmentStaleWarningDays(replenishmentDays);
        summary.setDrawingBudgetStaleWarningDays(drawingBudgetDays);
        summary.setOrderCount(orderCount);
        summary.setTotalCount(orderCount);
        summary.setSampleRoomCount(sampleRoomCount);
        summary.setBulkCount(bulkCount);
        summary.setReplenishmentCount(replenishmentCount);
        summary.setDrawingBudgetCount(drawingBudgetCount);
        return summary;
    }

    private long countOrdersByCategory(String tenantCode, String category, int days, Set<String> statusScope) {
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantCode, tenantCode)
                .eq(SalesOrder::getOrderCategory, category)
                .notIn(SalesOrder::getStatus, OrderStatusEnum.COMPLETED.getCode(),
                        OrderStatusEnum.PENDING_CANCEL.getCode(), OrderStatusEnum.CANCELLED.getCode(), OrderStatusEnum.BUDGET_COMPLETED.getCode())
                .apply("COALESCE(update_time, create_time) <= {0}", LocalDateTime.now().minusDays(days));
        applyStatusScope(wrapper, statusScope);
        return safeCount(salesOrderMapper.selectCount(wrapper));
    }

    private long countOtherOrders(String tenantCode, int days, Set<String> statusScope) {
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantCode, tenantCode)
                .and(category -> category.isNull(SalesOrder::getOrderCategory)
                        .or()
                        .notIn(SalesOrder::getOrderCategory,
                                OrderCategoryEnum.SAMPLE_ROOM.getCode(),
                                OrderCategoryEnum.BULK.getCode(),
                                OrderCategoryEnum.REPLENISHMENT.getCode(),
                                OrderCategoryEnum.DRAWING_BUDGET.getCode()))
                .notIn(SalesOrder::getStatus, OrderStatusEnum.COMPLETED.getCode(),
                        OrderStatusEnum.PENDING_CANCEL.getCode(), OrderStatusEnum.CANCELLED.getCode(), OrderStatusEnum.BUDGET_COMPLETED.getCode())
                .apply("COALESCE(update_time, create_time) <= {0}", LocalDateTime.now().minusDays(days));
        applyStatusScope(wrapper, statusScope);
        return safeCount(salesOrderMapper.selectCount(wrapper));
    }

    private void applyStatusScope(LambdaQueryWrapper<SalesOrder> wrapper, Set<String> statusScope) {
        if (statusScope != null) {
            wrapper.in(SalesOrder::getStatus, statusScope);
        }
    }

    private Set<String> normalizeStatusScope(Set<String> permittedStatuses) {
        if (permittedStatuses == null) {
            return null;
        }
        Set<String> normalized = new LinkedHashSet<>();
        permittedStatuses.stream()
                .filter(status -> status != null && !status.isBlank())
                .map(String::trim)
                .sorted()
                .forEach(normalized::add);
        return normalized;
    }

    private OrderWarningSummaryVO normalize(OrderWarningSummaryVO summary,
                                            int days,
                                            int sampleRoomDays,
                                            int bulkDays,
                                            int replenishmentDays,
                                            int drawingBudgetDays) {
        summary.setStaleWarningDays(summary.getStaleWarningDays() == null ? days : summary.getStaleWarningDays());
        summary.setSampleRoomStaleWarningDays(summary.getSampleRoomStaleWarningDays() == null ? sampleRoomDays : summary.getSampleRoomStaleWarningDays());
        summary.setBulkStaleWarningDays(summary.getBulkStaleWarningDays() == null ? bulkDays : summary.getBulkStaleWarningDays());
        summary.setReplenishmentStaleWarningDays(summary.getReplenishmentStaleWarningDays() == null ? replenishmentDays : summary.getReplenishmentStaleWarningDays());
        summary.setDrawingBudgetStaleWarningDays(summary.getDrawingBudgetStaleWarningDays() == null ? drawingBudgetDays : summary.getDrawingBudgetStaleWarningDays());
        summary.setOrderCount(summary.getOrderCount() == null ? 0L : summary.getOrderCount());
        summary.setSampleRoomCount(summary.getSampleRoomCount() == null ? 0L : summary.getSampleRoomCount());
        summary.setBulkCount(summary.getBulkCount() == null ? 0L : summary.getBulkCount());
        summary.setReplenishmentCount(summary.getReplenishmentCount() == null ? 0L : summary.getReplenishmentCount());
        summary.setDrawingBudgetCount(summary.getDrawingBudgetCount() == null ? 0L : summary.getDrawingBudgetCount());
        summary.setTotalCount(summary.getTotalCount() == null ? summary.getOrderCount() : summary.getTotalCount());
        return summary;
    }

    private OrderWarningSummaryVO emptySummary(int days) {
        return emptySummary(days, days, days, days, days);
    }

    private OrderWarningSummaryVO emptySummary(int days,
                                               int sampleRoomDays,
                                               int bulkDays,
                                               int replenishmentDays,
                                               int drawingBudgetDays) {
        OrderWarningSummaryVO summary = new OrderWarningSummaryVO();
        summary.setStaleWarningDays(days);
        summary.setSampleRoomStaleWarningDays(sampleRoomDays);
        summary.setBulkStaleWarningDays(bulkDays);
        summary.setReplenishmentStaleWarningDays(replenishmentDays);
        summary.setDrawingBudgetStaleWarningDays(drawingBudgetDays);
        summary.setOrderCount(0L);
        summary.setTotalCount(0L);
        summary.setSampleRoomCount(0L);
        summary.setBulkCount(0L);
        summary.setReplenishmentCount(0L);
        summary.setDrawingBudgetCount(0L);
        return summary;
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }

    private String cacheKey(String tenantCode, int days, int sampleRoomDays, int bulkDays,
                            int replenishmentDays, int drawingBudgetDays, Set<String> statusScope) {
        String scopeToken = statusScope == null ? "all" : String.join(",", new TreeSet<>(statusScope));
        return redisKeyBuilder.cache("order", "warning", tenantCode,
                CACHE_VERSION + "-" + days + "-" + sampleRoomDays + "-" + bulkDays + "-"
                        + replenishmentDays + "-" + drawingBudgetDays + "-" + Integer.toHexString(scopeToken.hashCode()));
    }
}
