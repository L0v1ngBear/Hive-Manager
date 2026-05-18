package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import my.hive.common.annotation.CollectLog;
import my.hive.common.annotation.RequirePermission;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import my.hive.common.dto.Result;
import my.management.common.storage.BusinessAttachmentService;
import my.management.common.storage.BusinessAttachmentVO;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import my.management.module.approval.model.dto.FinanceAuditRequest;
import my.management.module.approval.model.dto.FinanceSubmitRequest;
import my.management.module.approval.model.dto.LeaveAuditRequest;
import my.management.module.approval.model.dto.OrderApprovalAuditRequest;
import my.management.module.approval.model.dto.ResignationAuditRequest;
import my.management.module.approval.model.dto.ResignationSubmitRequest;
import my.management.module.approval.model.vo.ApprovalSummaryVO;
import my.management.module.approval.model.vo.FinanceApprovalVO;
import my.management.module.approval.model.vo.LeaveApprovalListVO;
import my.management.module.approval.model.vo.LeaveDetailVO;
import my.management.module.approval.model.vo.OrderApprovalVO;
import my.management.module.approval.model.vo.ResignationApprovalVO;
import my.management.module.approval.service.ApprovalService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    @Resource
    private BusinessAttachmentService businessAttachmentService;

    @GetMapping("/summary")
    public Result<ApprovalSummaryVO> summary() {
        return Result.success(approvalService.getSummary());
    }

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
    @CollectLog(module = "approval", action = "audit_leave", bizType = "leave_approval", bizNo = "#request.leaveCode", description = "管理端审批请假单")
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
    @CollectLog(module = "approval", action = "audit_finance", bizType = "finance_approval", bizNo = "#request.approvalCode", description = "管理端审批财务单")
    public Result<Void> auditFinance(@Valid @RequestBody FinanceAuditRequest request) {
        approvalService.auditFinance(request);
        return Result.success(null);
    }

    @PostMapping("/finance/submit")
    @RequirePermission(value = PermissionCodeEnum.CODE_APPROVAL_FINANCE_SUBMIT, message = "您没有权限提交财务审批")
    @CollectLog(module = "approval", action = "submit_finance", bizType = "finance_approval", description = "管理端提交财务审批")
    public Result<String> submitFinance(@Valid @RequestBody FinanceSubmitRequest request) {
        return Result.success(approvalService.submitFinance(request));
    }

    @PostMapping("/finance/attachment/upload")
    @RequirePermission(value = PermissionCodeEnum.CODE_APPROVAL_FINANCE_SUBMIT, message = "您没有权限上传财务审批附件")
    @CollectLog(module = "approval", action = "upload_finance_attachment", bizType = "finance_approval_attachment", description = "管理端上传财务审批附件")
    public Result<BusinessAttachmentVO> uploadFinanceAttachment(@RequestParam("file") MultipartFile file) {
        return Result.success(businessAttachmentService.upload(file, "finance"));
    }

    @GetMapping("/finance/attachment/download")
    @RequirePermission(value = PermissionCodeEnum.CODE_APPROVAL_FINANCE_DETAIL, message = "您没有权限下载财务审批附件")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFinanceAttachment(@RequestParam String url,
                                                                                         @RequestParam(required = false) String name) {
        org.springframework.core.io.Resource resource = businessAttachmentService.load(url, "finance");
        String filename = name != null && !name.isBlank() ? name.trim() : resource.getFilename();
        String encodedFilename = URLEncoder.encode(filename == null ? "finance-attachment" : filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @GetMapping("/resignation/list")
    @RequirePermission(value = PermissionCodeEnum.CODE_APPROVAL_RESIGNATION, message = "您没有权限查看离职审批列表")
    public Result<List<ResignationApprovalVO>> listResignationApprovals() {
        return Result.success(approvalService.listResignationApprovals());
    }

    @GetMapping("/resignation/{resignationCode}")
    @RequirePermission(value = PermissionCodeEnum.CODE_APPROVAL_RESIGNATION_DETAIL, message = "您没有权限查看离职审批详情")
    public Result<ResignationApprovalVO> getResignationDetail(@NotBlank @PathVariable String resignationCode) {
        return Result.success(approvalService.getResignationDetail(resignationCode));
    }

    @PostMapping("/resignation/submit")
    @RequirePermission(value = PermissionCodeEnum.CODE_APPROVAL_RESIGNATION_SUBMIT, message = "您没有权限提交离职审批")
    @CollectLog(module = "approval", action = "submit_resignation", bizType = "resignation_approval", description = "管理端提交离职审批")
    public Result<String> submitResignation(@Valid @RequestBody ResignationSubmitRequest request) {
        return Result.success(approvalService.submitResignation(request));
    }

    @PostMapping("/resignation/audit")
    @RequirePermission(value = PermissionCodeEnum.CODE_APPROVAL_RESIGNATION_AUDIT, message = "您没有权限审批离职单")
    @CollectLog(module = "approval", action = "audit_resignation", bizType = "resignation_approval", bizNo = "#request.resignationCode", description = "管理端审批离职单")
    public Result<Void> auditResignation(@Valid @RequestBody ResignationAuditRequest request) {
        approvalService.auditResignation(request);
        return Result.success(null);
    }

    @GetMapping("/order/list")
    @RequirePermission(value = PermissionCodeEnum.CODE_SALES_ORDER_LIST, message = "您没有权限查看订单审批列表")
    public Result<List<OrderApprovalVO>> listOrderApprovals() {
        return Result.success(approvalService.listOrderApprovals());
    }

    @GetMapping("/order/{orderType}/{orderId}")
    @RequirePermission(value = PermissionCodeEnum.CODE_SALES_ORDER_DETAIL, message = "您没有权限查看订单审批详情")
    public Result<OrderApprovalVO> getOrderApprovalDetail(@NotBlank @PathVariable String orderType,
                                                          @NotBlank @PathVariable String orderId) {
        return Result.success(approvalService.getOrderApprovalDetail(orderType, orderId));
    }

    @PostMapping("/order/audit")
    @RequirePermission(value = PermissionCodeEnum.CODE_SALES_ORDER_STATUS, message = "您没有权限处理订单审批")
    @CollectLog(module = "approval", action = "audit_order", bizType = "order_approval", bizNo = "#request.orderId", description = "管理端确认待审批订单")
    public Result<Void> auditOrder(@Valid @RequestBody OrderApprovalAuditRequest request) {
        approvalService.auditOrder(request);
        return Result.success(null);
    }
}
