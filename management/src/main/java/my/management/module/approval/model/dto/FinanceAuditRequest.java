package my.management.module.approval.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
/**
 * FinanceAuditRequest 属于管理端后端审批模块，定义入参结构。
 */
@Data
public class FinanceAuditRequest {

    @NotBlank(message = "财务审批单号不能为空")
    private String approvalCode;

    @NotNull(message = "审批动作不能为空")
    private Integer action;

    private String comment;
}
