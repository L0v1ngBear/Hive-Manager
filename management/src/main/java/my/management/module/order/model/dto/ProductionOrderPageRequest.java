package my.management.module.order.model.dto;

import lombok.Data;

/**
 * 生产订单分页查询条件。
 */
@Data
public class ProductionOrderPageRequest {

    private long pageNum = 1;

    private long pageSize = 10;

    private String keyword;

    private String status;
}
