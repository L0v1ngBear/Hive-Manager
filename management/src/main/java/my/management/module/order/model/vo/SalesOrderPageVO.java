package my.management.module.order.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单列表行对象。
 */
@Data
public class SalesOrderPageVO {

    private String orderId;

    private String status;

    private String customerName;

    private String customerPhone;

    private String goodsDesc;

    private BigDecimal totalAmount;

    private Integer totalQuantity;

    private String deliveryDate;

    private String expressCompany;

    private String expressNo;

    private String creator;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer detailCount;
}
