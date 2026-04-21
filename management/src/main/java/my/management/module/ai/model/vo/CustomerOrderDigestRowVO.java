package my.management.module.ai.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户订单分析原始行对象，供规则层计算复购周期和跟进建议。
 */
@Data
public class CustomerOrderDigestRowVO {

    private String customerName;

    private LocalDateTime createTime;

    private BigDecimal totalAmount;
}
