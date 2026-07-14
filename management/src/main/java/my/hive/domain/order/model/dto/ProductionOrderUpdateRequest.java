package my.hive.domain.order.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 生产订单更新请求，用于状态、工序和备注维护。
 */
@Data
public class ProductionOrderUpdateRequest {

    private String status;

    private Integer process;

    private String operateType;

    private String remark;

    /** 回退审批人，未传时使用默认订单审批人 */
    private List<Long> auditorIds;
}
