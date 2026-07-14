package my.hive.domain.inventory.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryWarningSettingVO {

    private BigDecimal warningThresholdMeters = BigDecimal.ZERO;
}