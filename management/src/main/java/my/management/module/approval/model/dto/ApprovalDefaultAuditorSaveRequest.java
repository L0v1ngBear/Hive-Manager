package my.management.module.approval.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalDefaultAuditorSaveRequest {

    @NotBlank(message = "approval type is required")
    private String approvalType;

    @NotNull(message = "default auditor is required")
    private Long auditorId;
}
