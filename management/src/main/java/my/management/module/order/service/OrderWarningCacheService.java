package my.management.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import my.hive.common.redis.HiveRedisKeyBuilder;
import my.hive.common.utils.RedisCacheHelper;
import my.management.module.order.mapper.ProductionOrderMapper;
import my.management.module.order.mapper.SalesOrderMapper;
import my.management.module.order.model.entity.ProductionOrder;
import my.management.module.order.model.entity.SalesOrder;
import my.management.module.order.model.enums.OrderCategoryEnum;
import my.management.module.order.model.enums.OrderStatusEnum;
import my.management.module.order.model.vo.OrderWarningSummaryVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
public class OrderWarningCacheService {

    private static final Duration WARNING_CACHE_TTL = Duration.ofMinutes(2);

    @Resource
    private SalesOrderMapper salesOrderMapper;

    @Resource
    private ProductionOrderMapper productionOrderMapper;

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
        if (tenantCode == null || tenantCode.isBlank()) {
            return emptySummary(OrderSettingService.DEFAULT_STALE_WARNING_DAYS);
        }
        int days = orderSettingService.staleWarningDays(tenantCode);
        int sampleRoomDays = orderSettingService.staleWarningDays(tenantCode, OrderCategoryEnum.SAMPLE_ROOM.getCode());
        int bulkDays = orderSettingService.staleWarningDays(tenantCode, OrderCategoryEnum.BULK.getCode());
        int replenishmentDays = orderSettingService.staleWarningDays(tenantCode, OrderCategoryEnum.REPLENISHMENT.getCode());
        int drawingBudgetDays = orderSettingService.staleWarningDays(tenantCode, OrderCategoryEnum.DRAWING_BUDGET.getCode());
        String cacheKey = cacheKey(tenantCode, days, sampleRoomDays, bulkDays, replenishmentDays, drawingBudgetDays);
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
                replenishmentDays, drawingBudgetDays);
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
                                               int drawingBudgetDays) {
        long sampleRoomSalesCount = countSalesByCategory(tenantCode, OrderCategoryEnum.SAMPLE_ROOM.getCode(), sampleRoomDays);
        long bulkSalesCount = countSalesByCategory(tenantCode, OrderCategoryEnum.BULK.getCode(), bulkDays);
        long replenishmentSalesCount = countSalesByCategory(tenantCode, OrderCategoryEnum.REPLENISHMENT.getCode(), replenishmentDays);
        long drawingBudgetSalesCount = countSalesByCategory(tenantCode, OrderCategoryEnum.DRAWING_BUDGET.getCode(), drawingBudgetDays);
        long otherSalesCount = countOtherSales(tenantCode, days);

        long sampleRoomProductionCount = countProductionByCategory(tenantCode, OrderCategoryEnum.SAMPLE_ROOM.getCode(), sampleRoomDays);
        long bulkProductionCount = countProductionByCategory(tenantCode, OrderCategoryEnum.BULK.getCode(), bulkDays);
        long replenishmentProductionCount = countProductionByCategory(tenantCode, OrderCategoryEnum.REPLENISHMENT.getCode(), replenishmentDays);
        long drawingBudgetProductionCount = countProductionByCategory(tenantCode, OrderCategoryEnum.DRAWING_BUDGET.getCode(), drawingBudgetDays);
        long otherProductionCount = countOtherProduction(tenantCode, days);

        long salesCount = sampleRoomSalesCount + bulkSalesCount + replenishmentSalesCount
                + drawingBudgetSalesCount + otherSalesCount;
        long productionCount = sampleRoomProductionCount + bulkProductionCount + replenishmentProductionCount
                + drawingBudgetProductionCount + otherProductionCount;

        OrderWarningSummaryVO summary = new OrderWarningSummaryVO();
        summary.setStaleWarningDays(days);
        summary.setSampleRoomStaleWarningDays(sampleRoomDays);
        summary.setBulkStaleWarningDays(bulkDays);
        summary.setReplenishmentStaleWarningDays(replenishmentDays);
        summary.setDrawingBudgetStaleWarningDays(drawingBudgetDays);
        summary.setSalesCount(salesCount);
        summary.setProductionCount(productionCount);
        summary.setTotalCount(salesCount + productionCount);
        summary.setSampleRoomCount(sampleRoomSalesCount + sampleRoomProductionCount);
        summary.setBulkCount(bulkSalesCount + bulkProductionCount);
        summary.setReplenishmentCount(replenishmentSalesCount + replenishmentProductionCount);
        summary.setDrawingBudgetCount(drawingBudgetSalesCount + drawingBudgetProductionCount);
        return summary;
    }

    private long countSalesByCategory(String tenantCode, String category, int days) {
        return safeCount(salesOrderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderCategory, category)
                .notIn(SalesOrder::getStatus, OrderStatusEnum.COMPLETED.getCode(),
                        OrderStatusEnum.PENDING_CANCEL.getCode(), OrderStatusEnum.CANCELLED.getCode(), OrderStatusEnum.BUDGET_COMPLETED.getCode())
                .apply("COALESCE(update_time, create_time) <= {0}", LocalDateTime.now().minusDays(days))));
    }

    private long countOtherSales(String tenantCode, int days) {
        return safeCount(salesOrderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                .and(category -> category.isNull(SalesOrder::getOrderCategory)
                        .or()
                        .notIn(SalesOrder::getOrderCategory,
                                OrderCategoryEnum.SAMPLE_ROOM.getCode(),
                                OrderCategoryEnum.BULK.getCode(),
                                OrderCategoryEnum.REPLENISHMENT.getCode(),
                                OrderCategoryEnum.DRAWING_BUDGET.getCode()))
                .notIn(SalesOrder::getStatus, OrderStatusEnum.COMPLETED.getCode(),
                        OrderStatusEnum.PENDING_CANCEL.getCode(), OrderStatusEnum.CANCELLED.getCode(), OrderStatusEnum.BUDGET_COMPLETED.getCode())
                .apply("COALESCE(update_time, create_time) <= {0}", LocalDateTime.now().minusDays(days))));
    }

    private long countProductionByCategory(String tenantCode, String category, int days) {
        return safeCount(productionOrderMapper.selectCount(new LambdaQueryWrapper<ProductionOrder>()
                .eq(ProductionOrder::getOrderCategory, category)
                .ne(ProductionOrder::getStatus, OrderStatusEnum.COMPLETED.getCode())
                .apply("COALESCE(update_time, create_time) <= {0}", LocalDateTime.now().minusDays(days))));
    }

    private long countOtherProduction(String tenantCode, int days) {
        return safeCount(productionOrderMapper.selectCount(new LambdaQueryWrapper<ProductionOrder>()
                .and(category -> category.isNull(ProductionOrder::getOrderCategory)
                        .or()
                        .notIn(ProductionOrder::getOrderCategory,
                                OrderCategoryEnum.SAMPLE_ROOM.getCode(),
                                OrderCategoryEnum.BULK.getCode(),
                                OrderCategoryEnum.REPLENISHMENT.getCode(),
                                OrderCategoryEnum.DRAWING_BUDGET.getCode()))
                .ne(ProductionOrder::getStatus, OrderStatusEnum.COMPLETED.getCode())
                .apply("COALESCE(update_time, create_time) <= {0}", LocalDateTime.now().minusDays(days))));
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
        summary.setSalesCount(summary.getSalesCount() == null ? 0L : summary.getSalesCount());
        summary.setProductionCount(summary.getProductionCount() == null ? 0L : summary.getProductionCount());
        summary.setSampleRoomCount(summary.getSampleRoomCount() == null ? 0L : summary.getSampleRoomCount());
        summary.setBulkCount(summary.getBulkCount() == null ? 0L : summary.getBulkCount());
        summary.setReplenishmentCount(summary.getReplenishmentCount() == null ? 0L : summary.getReplenishmentCount());
        summary.setDrawingBudgetCount(summary.getDrawingBudgetCount() == null ? 0L : summary.getDrawingBudgetCount());
        summary.setTotalCount(summary.getTotalCount() == null
                ? summary.getSalesCount() + summary.getProductionCount()
                : summary.getTotalCount());
        return summary;
    }

    private OrderWarningSummaryVO emptySummary(int days) {
        OrderWarningSummaryVO summary = new OrderWarningSummaryVO();
        summary.setStaleWarningDays(days);
        summary.setSampleRoomStaleWarningDays(days);
        summary.setBulkStaleWarningDays(days);
        summary.setReplenishmentStaleWarningDays(days);
        summary.setDrawingBudgetStaleWarningDays(days);
        summary.setSalesCount(0L);
        summary.setProductionCount(0L);
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
                            int replenishmentDays, int drawingBudgetDays) {
        return redisKeyBuilder.cache("order", "warning", tenantCode,
                days + "-" + sampleRoomDays + "-" + bulkDays + "-" + replenishmentDays + "-" + drawingBudgetDays);
    }
}
