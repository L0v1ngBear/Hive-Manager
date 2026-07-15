package my.hive.domain.price.model.dto;

import lombok.Data;

import java.math.BigDecimal;
/**
 * TierPriceRequest 属于管理端后端价格模块，定义入参结构。
 */
@Data
public class TierPriceRequest {

    private String tierCode;

    private String tierName;

    private BigDecimal fixedPrice;

    private BigDecimal discountRate;
}
