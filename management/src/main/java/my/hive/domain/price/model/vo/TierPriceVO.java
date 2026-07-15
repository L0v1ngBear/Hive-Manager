package my.hive.domain.price.model.vo;

import lombok.Data;

import java.math.BigDecimal;
/**
 * TierPriceVO 属于管理端后端价格模块，定义出参结构。
 */
@Data
public class TierPriceVO {

    private Long id;

    private String tierCode;

    private String tierName;

    private BigDecimal fixedPrice;

    private BigDecimal discountRate;

    private BigDecimal finalPrice;
}
