package my.management.module.order.model.dto;

import lombok.Data;

/**
 * 销售订单分页查询条件。
 */
@Data
public class SalesOrderPageRequest {

    private long pageNum = 1;

    private long pageSize = 10;

    private String keyword;

    private String status;
}
