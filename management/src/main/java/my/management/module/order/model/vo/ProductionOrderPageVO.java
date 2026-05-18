package my.management.module.order.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产订单列表行对象。
 */
@Data
public class ProductionOrderPageVO {

    private String orderId;

    private String salesOrderId;

    private String status;

    private String modelCode;

    private String fabric;

    private BigDecimal weight;

    private BigDecimal width;

    private String color;

    private Integer quantity;

    private Integer process;

    private String customerName;

    private String projectName;

    private LocalDateTime deliveryDate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean staleWarning = false;

    private Long staleDays = 0L;

    private Integer staleWarningDays;
}
