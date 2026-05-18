package my.management.module.inventory.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryWarningSettingUpdateRequest {

    @NotNull(message = "库存预警阈值不能为空")
    @DecimalMin(value = "0.00", message = "库存预警阈值不能小于0")
    @DecimalMax(value = "999999999.99", message = "库存预警阈值不能超过999999999.99")
    @Digits(integer = 9, fraction = 2, message = "库存预警阈值最多保留2位小数")
    private BigDecimal warningThresholdMeters;
}
