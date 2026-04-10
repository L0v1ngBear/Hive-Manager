package my.management.module.price.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TierPriceVO {

    private Long id;

    private String tierCode;

    private String tierName;

    private BigDecimal fixedPrice;

    private BigDecimal discountRate;

    private BigDecimal finalPrice;
}