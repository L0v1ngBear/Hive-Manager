package my.management.module.order.model.dto;

import lombok.Data;

/**
 * 销售订单更新请求，主要用于状态和物流信息维护。
 */
@Data
public class SalesOrderUpdateRequest {

    private String status;

    private String deliveryDate;

    private String expressCompany;

    private String expressNo;

    private String remark;
}
