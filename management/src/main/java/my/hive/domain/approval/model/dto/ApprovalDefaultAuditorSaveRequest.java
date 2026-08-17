package my.hive.domain.approval.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalDefaultAuditorSaveRequest {

    @NotBlank(message = "审批类型不能为空")
    private String approvalType;

    private Long auditorId;

    private List<Long> auditorIds;

    private String approvalMode;
}
