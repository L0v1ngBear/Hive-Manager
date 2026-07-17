package my.hive.domain.order.model.vo;

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

    private String brandName;

    private String orderCategory;

    private String goodsDesc;

    private Integer totalQuantity;

    private String informationChannel;

    private List<SalesOrderShipmentVO> shipments;

    private Integer isInvoice;

    private String creator;

    private String attachmentName;

    private String attachmentUrl;

    private Long attachmentSize;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean fulfillmentTracked = false;

    private Integer fulfillmentRecordCount = 0;

    private Integer process;

    private String processText;

    private String currentProcessText;

    private String completedProcessText;

    private Integer processProgressPercent;

    private List<ProductionProcessStepVO> processSteps;

    private List<ItemVO> items;

    private List<SalesOrderNoteVO> notes;

    private List<SalesOrderStatusLogVO> logs;

    @Data
    public static class ItemVO {

        private Long id;

        private String modelCode;

        private String weight;

        private String spec;

        private BigDecimal quantity;
    }
}
