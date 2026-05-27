package my.management.module.order.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产订单列表行对象。
 */
@Data
public class ProductionOrderPageVO {

    private String orderId;

    private String salesOrderId;

    private String status;

    private String orderCategory;

    private String modelCode;

    private String fabric;

    private BigDecimal weight;

    private BigDecimal width;

    private String color;

    private Integer quantity;

    private Integer process;

    private String processText;

    private String currentProcessText;

    private String completedProcessText;

    private Integer processProgressPercent;

    private List<ProductionProcessStepVO> processSteps;

    private String customerName;

    private String projectName;

    private String brandName;

    private LocalDateTime deliveryDate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean staleWarning = false;

    private Long staleDays = 0L;

    private Integer staleWarningDays;
}
