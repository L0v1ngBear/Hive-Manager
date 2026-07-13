package my.management.module.order.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订单流转记录业务时间修正请求。
 */
@Data
public class OrderStatusLogTimeCorrectionRequest {

    @NotBlank(message = "流转记录时间不能为空")
    private String createTime;
}
