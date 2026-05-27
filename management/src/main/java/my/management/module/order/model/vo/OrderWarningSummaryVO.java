package my.management.module.order.model.vo;

import lombok.Data;

@Data
public class OrderWarningSummaryVO {

    private Integer staleWarningDays;

    private Integer sampleRoomStaleWarningDays;

    private Integer bulkStaleWarningDays;

    private Integer replenishmentStaleWarningDays;

    private Integer drawingBudgetStaleWarningDays;

    private Long salesCount = 0L;

    private Long productionCount = 0L;

    private Long totalCount = 0L;

    private Long sampleRoomCount = 0L;

    private Long bulkCount = 0L;

    private Long replenishmentCount = 0L;

    private Long drawingBudgetCount = 0L;
}
