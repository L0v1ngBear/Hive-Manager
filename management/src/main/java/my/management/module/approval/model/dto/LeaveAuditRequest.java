package my.management.module.approval.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
/**
 * LeaveAuditRequest 属于管理端后端审批模块，定义入参结构。
 */
@Data
public class LeaveAuditRequest {

    @NotBlank(message = "请假单号不能为空")
    private String leaveCode;

    @NotNull(message = "审批动作不能为空")
    private Integer action;

    private String comment;
}
