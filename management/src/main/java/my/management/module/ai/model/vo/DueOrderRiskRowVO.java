package my.management.module.ai.model.vo;

import lombok.Data;

/**
 * 临近交付订单风险行对象，用于生成履约风险建议。
 */
@Data
public class DueOrderRiskRowVO {

    private String orderId;

    private String customerName;

    private String status;

    private String deliveryDate;
}
