package my.management.module.order.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售订单详情对象，包含主单和明细项。
 */
@Data
public class SalesOrderDetailVO {

    private String orderId;

    private String status;

    private String customerName;

    private String customerPhone;

    private String projectName;

    private String goodsDesc;

    private BigDecimal totalAmount;

    private Integer totalQuantity;

    private String deliveryDate;

    private String expressCompany;

    private String expressNo;

    private Integer isInvoice;

    private String creator;

    private String remark;

    private String attachmentName;

    private String attachmentUrl;

    private Long attachmentSize;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<ItemVO> items;

    private List<SalesOrderStatusLogVO> logs;

    @Data
    public static class ItemVO {

        private Long id;

        private String modelCode;

        private Float weight;

        private String spec;

        private BigDecimal quantity;
    }
}
