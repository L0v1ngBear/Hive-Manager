package my.hive.domain.order.model.vo;

import lombok.Data;

@Data
public class OrderWarningSettingVO {

    private Integer staleWarningDays;

    private Integer sampleRoomStaleWarningDays;

    private Integer bulkStaleWarningDays;

    private Integer replenishmentStaleWarningDays;

    private Integer drawingBudgetStaleWarningDays;
}
