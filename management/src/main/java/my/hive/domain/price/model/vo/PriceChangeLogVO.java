package my.hive.domain.price.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * PriceChangeLogVO 属于管理端后端价格模块，定义出参结构。
 */
@Data
public class PriceChangeLogVO {

    private BigDecimal oldPrice;

    private BigDecimal newPrice;

    private Long operatorUserId;

    private String remark;

    private LocalDateTime createTime;
}
