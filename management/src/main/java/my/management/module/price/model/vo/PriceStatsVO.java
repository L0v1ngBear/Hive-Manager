package my.management.module.price.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PriceStatsVO {

    private Long skuCount;

    private BigDecimal averagePrice;

    private Long pendingCount;

    private Long overrideCount;
}