package my.management.module.inventory.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存概览出参。
 */
@Data
public class InventorySummaryVO {

    private BigDecimal totalMeters = BigDecimal.ZERO;

    private Long clothCount = 0L;

    private Long warningCount = 0L;

    private BigDecimal warningThresholdMeters = BigDecimal.ZERO;

    private BigDecimal todayInMeters = BigDecimal.ZERO;

    private BigDecimal todayOutMeters = BigDecimal.ZERO;
}
