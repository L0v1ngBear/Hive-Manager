package my.hive.domain.order.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 销售订单更新请求，主要用于状态流转和轻量业务字段维护。
 */
@Data
public class SalesOrderUpdateRequest {

    private String status;

    private String informationChannel;

    private Integer isInvoice;

    private String remark;

    private List<Long> auditorIds;

    @Valid
    @Size(max = 50, message = "每个订单最多添加50条物流信息")
    private List<SalesOrderShipmentSaveRequest> shipments;
}
