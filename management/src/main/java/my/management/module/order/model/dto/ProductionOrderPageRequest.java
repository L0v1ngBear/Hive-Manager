package my.management.module.order.model.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 生产订单分页查询条件。
 */
@Data
public class ProductionOrderPageRequest {

    private long pageNum = 1;

    private long pageSize = 10;

    private String keyword;

    private String status;

    private String customerName;

    private String brandName;

    private String orderCategory;

    private Integer process;

    private LocalDate deliveryStart;

    private LocalDate deliveryEnd;

    private LocalDate createStart;

    private LocalDate createEnd;

    private Boolean staleOnly;
}
