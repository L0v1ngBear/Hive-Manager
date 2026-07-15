package my.hive.domain.order.service;

import my.hive.domain.order.model.enums.OrderInvoiceStatusEnum;
import my.hive.domain.order.model.enums.OrderStatusEnum;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 固定七天的未开票预警规则，与订单未更新预警相互独立。
 */
public final class OrderInvoiceWarningPolicy {

    public static final int WARNING_DAYS = 7;

    private OrderInvoiceWarningPolicy() {
    }

    public static WarningResult evaluate(Integer invoiceStatus,
                                         String orderStatus,
                                         LocalDateTime createTime,
                                         LocalDateTime now) {
        Integer normalizedStatus = OrderInvoiceStatusEnum.normalize(invoiceStatus);
        LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
        long ageDays = createTime == null
                ? 0L
                : Math.max(0L, ChronoUnit.DAYS.between(createTime, effectiveNow));
        boolean excludedStatus = OrderStatusEnum.PENDING_CANCEL.matches(orderStatus)
                || OrderStatusEnum.CANCELLED.matches(orderStatus);
        boolean warning = OrderInvoiceStatusEnum.UNISSUED.getCode().equals(normalizedStatus)
                && !excludedStatus
                && createTime != null
                && !createTime.isAfter(effectiveNow.minusDays(WARNING_DAYS));
        return new WarningResult(warning, ageDays, WARNING_DAYS);
    }

    public record WarningResult(boolean warning, long ageDays, int warningDays) {
    }
}
