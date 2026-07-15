package my.hive.domain.order.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售订单列表行对象。
 */
@Data
public class SalesOrderPageVO {

    private String orderId;

    private String status;

    private String customerName;

    private String customerPhone;

    private String projectName;

    private String brandName;

    private String orderCategory;

    private String goodsDesc;

    private Integer totalQuantity;

    private String informationChannel;

    private String expressCompany;

    private String expressNo;

    private Integer isInvoice;

    private String creator;

    private String attachmentName;

    private String attachmentUrl;

    private Long attachmentSize;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer detailCount;

    private Boolean fulfillmentTracked = false;

    private Integer fulfillmentRecordCount = 0;

    private Integer process;

    private String processText;

    private String currentProcessText;

    private String completedProcessText;

    private Integer processProgressPercent;

    private List<ProductionProcessStepVO> processSteps;

    /**
     * 列表页展示的核心信息直接复用订单明细，避免汇总字段和明细字段展示不一致。
     */
    private List<ItemVO> items;

    private Boolean staleWarning = false;

    private Long staleDays = 0L;

    private Integer staleWarningDays;

    @Data
    public static class ItemVO {
        private Long id;

        private String modelCode;

        private String weight;

        private String spec;

        private BigDecimal quantity;
    }
}
