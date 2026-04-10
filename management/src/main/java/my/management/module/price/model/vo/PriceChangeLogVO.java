package my.management.module.price.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PriceChangeLogVO {

    private BigDecimal oldPrice;

    private BigDecimal newPrice;

    private Long operatorUserId;

    private String remark;

    private LocalDateTime createTime;
}