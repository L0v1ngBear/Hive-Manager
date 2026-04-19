package my.management.module.order.model.dto;

import lombok.Data;

/**
 * 生产订单更新请求，用于状态、工序和备注维护。
 */
@Data
public class ProductionOrderUpdateRequest {

    private String status;

    private Integer process;

    private String remark;
}
