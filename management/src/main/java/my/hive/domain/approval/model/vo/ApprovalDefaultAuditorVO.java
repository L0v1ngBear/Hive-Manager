package my.hive.domain.approval.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class ApprovalDefaultAuditorVO {

    private String approvalType;

    private String approvalTypeText;

    private String permissionCode;

    private Long auditorId;

    private List<Long> auditorIds;

    private String auditorName;

    private Boolean configured;
}
