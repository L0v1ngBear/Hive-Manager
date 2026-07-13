package my.management.module.approval.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 质量记录处理审核展示对象。
 */
@Data
public class QualityApprovalVO {

    private String defectiveId;

    private String orderId;

    private String type;

    private String typeText;

    private String applicantName;

    private String summary;

    private BigDecimal quantity;

    private BigDecimal lossAmount;

    private String description;

    private String responsiblePerson;

    private String processMeasure;

    private String improvementPlan;

    private String processMethod;

    private String processRemark;

    private Long auditorId;

    private String auditorIds;

    private String auditorName;

    private Integer status;

    private String statusText;

    private Boolean canAudit;

    private LocalDateTime createTime;
}
