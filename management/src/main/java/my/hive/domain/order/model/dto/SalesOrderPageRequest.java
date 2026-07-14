package my.hive.domain.order.model.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 销售订单分页查询条件。
 */
@Data
public class SalesOrderPageRequest {

    private long pageNum = 1;

    private long pageSize = 10;

    private String keyword;

    private String status;

    private String customerName;

    private String brandName;

    private String orderCategory;

    private Integer isInvoice;

    private String informationChannel;

    private LocalDate createStart;

    private LocalDate createEnd;

    private Boolean staleOnly;
}
