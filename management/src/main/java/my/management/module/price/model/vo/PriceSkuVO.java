package my.management.module.price.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PriceSkuVO {

    private Long id;

    private String modelCode;

    private String batchNo;

    private String category;

    private String spec;

    private BigDecimal basePrice;

    private String currency;

    private LocalDate effectiveDate;

    private Integer status;

    private String statusLabel;

    private String imageUrl;

    private String remark;
}