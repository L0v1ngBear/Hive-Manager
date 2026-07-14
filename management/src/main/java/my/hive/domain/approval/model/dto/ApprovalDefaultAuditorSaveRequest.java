package my.hive.domain.approval.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalDefaultAuditorSaveRequest {

    @NotBlank(message = "approval type is required")
    private String approvalType;

    private Long auditorId;

    private List<Long> auditorIds;
}
