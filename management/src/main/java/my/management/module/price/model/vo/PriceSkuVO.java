package my.management.module.price.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * PriceSkuVO 属于管理端后端价格模块，定义出参结构。
 */
@Data
public class PriceSkuVO {

    private Long id;

    private String modelCode;

    private String batchNo;

    private String spec;

    private BigDecimal basePrice;

    private String currency;

    private LocalDate effectiveDate;

    private Integer status;

    private String statusLabel;

    private String remark;
}
