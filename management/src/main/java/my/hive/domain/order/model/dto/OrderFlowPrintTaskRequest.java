package my.hive.domain.order.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订单流转码打印任务创建请求。
 */
@Data
public class OrderFlowPrintTaskRequest {

    @NotBlank(message = "订单号不能为空")
    private String orderId;
}
