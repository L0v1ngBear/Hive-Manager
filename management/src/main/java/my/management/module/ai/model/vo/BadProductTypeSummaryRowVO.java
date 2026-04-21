package my.management.module.ai.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 次品类型汇总行对象，用于生成质量风险建议。
 */
@Data
public class BadProductTypeSummaryRowVO {

    private String type;

    private Long recordCount;

    private BigDecimal totalLossAmount;
}
