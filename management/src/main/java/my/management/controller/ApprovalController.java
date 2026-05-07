package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import my.hive.common.annotation.RequirePermission;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import my.hive.common.dto.Result;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import my.management.module.approval.model.dto.FinanceAuditRequest;
import my.management.module.approval.model.dto.FinanceSubmitRequest;
import my.management.module.approval.model.dto.LeaveAuditRequest;
import my.management.module.approval.model.vo.FinanceApprovalVO;
import my.management.module.approval.model.vo.LeaveApprovalListVO;
import my.management.module.approval.model.vo.LeaveDetailVO;
import my.management.module.approval.service.ApprovalService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * ApprovalController 是管理端后端请求入口控制类，负责接收请求并调用对应服务。
 */
@RestController
@RequestMapping("/approval")
@RequireTenantFeature(TenantFeatureEnum.CODE_APPROVAL)
@Validated
public class ApprovalController {

    @Resource
    private ApprovalService approvalService;

    @GetMapping("/leave/list")
    @RequirePermission(value = PermissionCodeEnum.CODE_APPROVAL_LEAVE, message = "您没有权限查看请假审批列表")
    public Result<List<LeaveApprovalListVO>> listLeaveApprovals() {
        return Result.success(approvalService.listLeaveApprovals());
    }

    @GetMapping("/leave/{leaveCode}")
    @RequirePermission(value = PermissionCodeEnum.CODE_APPROVAL_LEAVE_DETAIL, message = "您没有权限查看请假详情")
    public Result<LeaveDetailVO> getLeaveDetail(@NotBlank @PathVariable String leaveCode) {
        return Result.success(approvalService.getLeaveDetail(leaveCode));
    }

    @PostMapping("/leave/audit")
    @RequirePermission(value = PermissionCodeEnum.CODE_APPROVAL_LEAVE_AUDIT, message = "您没有权限审批请假单")
    public Result<Void> auditLeave(@Valid @RequestBody LeaveAuditRequest request) {
        approvalService.auditLeave(request);
        return Result.success(null);
    }

    @GetMapping("/finance/list")
    @RequirePermission(value = PermissionCodeEnum.CODE_APPROVAL_FINANCE, message = "您没有权限查看财务审批列表")
    public Result<List<FinanceApprovalVO>> listFinanceApprovals() {
        return Result.success(approvalService.listFinanceApprovals());
    }

    @GetMapping("/finance/{approvalCode}")
    @RequirePermission(value = PermissionCodeEnum.CODE_APPROVAL_FINANCE_DETAIL, message = "您没有权限查看财务审批详情")
    public Result<FinanceApprovalVO> getFinanceDetail(@PathVariable String approvalCode) {
        return Result.success(approvalService.getFinanceDetail(approvalCode));
    }

    @PostMapping("/finance/audit")
    @RequirePermission(value = PermissionCodeEnum.CODE_APPROVAL_FINANCE_AUDIT, message = "您没有权限审批财务单")
    public Result<Void> auditFinance(@Valid @RequestBody FinanceAuditRequest request) {
        approvalService.auditFinance(request);
        return Result.success(null);
    }

    @PostMapping("/finance/submit")
    @RequirePermission(value = PermissionCodeEnum.CODE_APPROVAL_FINANCE_SUBMIT, message = "您没有权限提交财务审批")
    public Result<String> submitFinance(@Valid @RequestBody FinanceSubmitRequest request) {
        return Result.success(approvalService.submitFinance(request));
    }
}
