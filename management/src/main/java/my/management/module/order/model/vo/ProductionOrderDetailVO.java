package my.management.module.order.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产订单详情对象，包含订单主体和状态日志。
 */
@Data
public class ProductionOrderDetailVO {

    private Long id;

    private String orderId;

    private String salesOrderId;

    private String status;

    private String modelCode;

    private String fabric;

    private BigDecimal weight;

    private BigDecimal width;

    private String color;

    private Integer quantity;

    private BigDecimal price;

    private BigDecimal totalAmount;

    private Integer process;

    private String customerName;

    private String projectName;

    private String contactPhone;

    private LocalDateTime deliveryDate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<ProductionOrderStatusLogVO> logs;
}
