package my.management.module.inventory.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryWarningSettingVO {

    private BigDecimal warningThresholdMeters = BigDecimal.ZERO;
}
