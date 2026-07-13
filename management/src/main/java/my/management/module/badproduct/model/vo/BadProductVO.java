package my.management.module.badproduct.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 质量管理页面展示对象。
 */
@Data
public class BadProductVO {

    private String defectiveId;

    private String orderId;

    private String type;

    private LocalDateTime createTime;

    private String creator;

    private BigDecimal quantity;

    private BigDecimal lossAmount;

    private String description;

    private String responsiblePerson;

    private String processMeasure;

    private String improvementPlan;

    private String attachmentName;

    private String attachmentUrl;

    private Long attachmentSize;

    private String status;

    private String processMethod;

    private String processRemark;
}
