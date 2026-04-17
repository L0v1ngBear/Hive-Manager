package my.management.module.price.model.vo;

import lombok.Data;

import java.math.BigDecimal;
/**
 * PriceStatsVO 属于管理端后端价格模块，定义出参结构。
 */
@Data
public class PriceStatsVO {

    private Long skuCount;

    private BigDecimal averagePrice;

    private Long pendingCount;

    private Long overrideCount;
}
