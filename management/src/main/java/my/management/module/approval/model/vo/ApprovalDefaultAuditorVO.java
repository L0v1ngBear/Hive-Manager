package my.management.module.approval.model.vo;

import lombok.Data;

@Data
public class ApprovalDefaultAuditorVO {

    private String approvalType;

    private String approvalTypeText;

    private String permissionCode;

    private Long auditorId;

    private String auditorName;

    private Boolean configured;
}
