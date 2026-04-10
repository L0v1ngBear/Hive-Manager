package my.management.module.price.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TierPriceRequest {

    private String tierCode;

    private String tierName;

    private BigDecimal fixedPrice;

    private BigDecimal discountRate;
}