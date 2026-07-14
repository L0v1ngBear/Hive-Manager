package my.hive.domain.inventory.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存趋势出参。
 */
@Data
public class InventoryTrendVO {

    private String statDate;

    private BigDecimal inMeters = BigDecimal.ZERO;

    private BigDecimal outMeters = BigDecimal.ZERO;
}