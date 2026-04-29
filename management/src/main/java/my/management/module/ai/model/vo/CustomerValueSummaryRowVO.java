package my.management.module.ai.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户价值汇总行，用于 AI 判断客户集中度和核心客户跟进优先级。
 */
@Data
public class CustomerValueSummaryRowVO {

    private String customerName;

    private Long orderCount;

    private BigDecimal totalAmount;
}
