package my.management.module.approval.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinanceApprovalVO {

    private String approvalCode;

    private Long applyUserId;

    private String applyUserName;

    private String applyDepartmentName;

    private String category;

    private BigDecimal amount;

    private String reason;

    private String attachmentUrl;

    private Integer status;

    private String statusText;

    private Long auditorId;

    private String auditorName;

    private String auditComment;

    private LocalDateTime createTime;
}
