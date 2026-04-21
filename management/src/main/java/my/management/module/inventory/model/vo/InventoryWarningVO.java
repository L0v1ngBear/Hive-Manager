package my.management.module.inventory.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 低库存预警出参。
 */
@Data
public class InventoryWarningVO {

    private String modelCode;

    private BigDecimal totalMeters;

    private LocalDateTime latestTime;
}
