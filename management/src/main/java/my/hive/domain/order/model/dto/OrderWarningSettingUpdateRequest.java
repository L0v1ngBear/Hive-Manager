package my.hive.domain.order.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class OrderWarningSettingUpdateRequest {

    @Min(value = 1, message = "订单未更新预警天数不能小于1天")
    @Max(value = 365, message = "订单未更新预警天数不能超过365天")
    private Integer staleWarningDays;

    @Min(value = 1, message = "样板间订单预警天数不能小于1天")
    @Max(value = 365, message = "样板间订单预警天数不能超过365天")
    private Integer sampleRoomStaleWarningDays;

    @Min(value = 1, message = "大货订单预警天数不能小于1天")
    @Max(value = 365, message = "大货订单预警天数不能超过365天")
    private Integer bulkStaleWarningDays;

    @Min(value = 1, message = "补单订单预警天数不能小于1天")
    @Max(value = 365, message = "补单订单预警天数不能超过365天")
    private Integer replenishmentStaleWarningDays;

    @Min(value = 1, message = "图纸预算订单预警天数不能小于1天")
    @Max(value = 365, message = "图纸预算订单预警天数不能超过365天")
    private Integer drawingBudgetStaleWarningDays;
}
