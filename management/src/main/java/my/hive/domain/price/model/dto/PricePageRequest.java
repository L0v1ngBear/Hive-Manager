package my.hive.domain.price.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * PricePageRequest 属于管理端后端价格模块，定义入参结构。
 */
@Data
public class PricePageRequest {

    private Integer page = 1;

    private Integer size = 10;

    private String keyword;

    private Integer status;

    private String batchNo;

    private String spec;

    private String currency;

    private BigDecimal priceMin;

    private BigDecimal priceMax;

    private LocalDate effectiveStart;

    private LocalDate effectiveEnd;
}
