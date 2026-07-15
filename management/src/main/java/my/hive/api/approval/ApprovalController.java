package my.hive.api.approval;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.shared.dto.Result;
import my.hive.infrastructure.storage.BusinessAttachmentService;
import my.hive.infrastructure.storage.BusinessAttachmentVO;
import my.hive.shared.tenant.RequireTenantFeature;
import my.hive.domain.tenant.model.enums.TenantFeatureEnum;
import my.hive.domain.approval.model.dto.FinanceAuditRequest;
import my.hive.domain.approval.model.dto.FinanceSubmitRequest;
import my.hive.domain.approval.model.dto.LeaveAuditRequest;
import my.hive.domain.approval.model.dto.LeaveSubmitRequest;
import my.hive.domain.approval.model.dto.OrderApprovalAuditRequest;
import my.hive.domain.approval.model.dto.ApprovalDefaultAuditorSaveRequest;
import my.hive.domain.approval.model.dto.QualityAuditRequest;
import my.hive.domain.approval.model.dto.ResignationAuditRequest;
import my.hive.domain.approval.model.dto.ResignationSubmitRequest;
import my.hive.domain.approval.model.vo.ApprovalSummaryVO;
import my.hive.domain.approval.model.vo.ApprovalAuditorOptionVO;
import my.hive.domain.approval.model.vo.ApprovalDefaultAuditorVO;
import my.hive.domain.approval.model.vo.FinanceApprovalVO;
import my.hive.domain.approval.model.vo.LeaveApprovalListVO;
import my.hive.domain.approval.model.vo.LeaveDetailVO;
import my.hive.domain.approval.model.vo.OrderApprovalVO;
import my.hive.domain.approval.model.vo.QualityApprovalVO;
import my.hive.domain.approval.model.vo.ResignationApprovalVO;
import my.hive.domain.approval.service.ApprovalDefaultAuditorService;
import my.hive.domain.approval.service.ApprovalService;
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
    private ApprovalDefaultAuditorService approvalDefaultAuditorService;

    @Resource
    private BusinessAttachmentService businessAttachmentService;

    @GetMapping("/summary")
    public Result<ApprovalSummaryVO> summary() {
        return Result.success(approvalService.getSummary());
    }

    @GetMapping("/auditors")
    public Result<List<ApprovalAuditorOptionVO>> listAuditors(@RequestParam String type,
                                                              @RequestParam(required = false) String keyword,
                                                              @RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(approvalService.listAuditorOptions(type, keyword, limit));
    }

    @GetMapping("/default-auditors")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_AUDITOR_LIST, message = "您没有权限查看审批负责人配置")
    public Result<List<ApprovalDefaultAuditorVO>> listDefaultAuditors() {
        return Result.success(approvalDefaultAuditorService.listDefaults());
    }

    @PostMapping("/default-auditors")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_AUDITOR_SETTING, message = "您没有权限配置审批负责人")
    @CollectLog(module = "approval", action = "save_default_auditor", bizType = "approval_default_auditor", bizNo = "#request.approvalType", description = "配置审批默认负责人")
    public Result<Void> saveDefaultAuditor(@Valid @RequestBody ApprovalDefaultAuditorSaveRequest request) {
        approvalDefaultAuditorService.saveDefault(request);
        return Result.success(null);
    }

    @GetMapping("/leave")
    @RequirePermission(value = {
            PermissionCatalogV3.CODE_APPROVAL_LEAVE_SUBMIT,
            PermissionCatalogV3.CODE_APPROVAL_LEAVE_LIST,
            PermissionCatalogV3.CODE_APPROVAL_LEAVE_AUDIT
    }, message = "您没有权限查看请假审批列表")
    public Result<List<LeaveApprovalListVO>> listLeaveApprovals(@RequestParam(defaultValue = "pending") String scope,
                                                                @RequestParam(required = false) Integer status,
                                                                @RequestParam(required = false) Integer limit) {
        return Result.success(approvalService.listLeaveApprovals(scope, status, limit));
    }

    @PostMapping("/leave")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_LEAVE_SUBMIT, message = "没有权限提交请假申请")
    @CollectLog(module = "approval", action = "submit_leave", bizType = "leave_approval", description = "提交请假审批")
    public Result<String> submitLeave(@Valid @RequestBody LeaveSubmitRequest request) {
        return Result.success(approvalService.submitLeave(request));
    }

    @GetMapping("/leave/{leaveCode}")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_LEAVE_DETAIL, message = "您没有权限查看请假详情")
    public Result<LeaveDetailVO> getLeaveDetail(@NotBlank @PathVariable String leaveCode) {
        return Result.success(approvalService.getLeaveDetail(leaveCode));
    }

    @PostMapping("/leave/audit")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_LEAVE_AUDIT, message = "您没有权限审批请假单")
    @CollectLog(module = "approval", action = "audit_leave", bizType = "leave_approval", bizNo = "#request.leaveCode", description = "管理端审批请假单")
    public Result<Void> auditLeave(@Valid @RequestBody LeaveAuditRequest request) {
        approvalService.auditLeave(request);
        return Result.success(null);
    }

    @GetMapping("/finance")
    @RequirePermission(value = {
            PermissionCatalogV3.CODE_APPROVAL_FINANCE_SUBMIT,
            PermissionCatalogV3.CODE_APPROVAL_FINANCE_LIST,
            PermissionCatalogV3.CODE_APPROVAL_FINANCE_AUDIT
    }, message = "您没有权限查看财务审批列表")
    public Result<List<FinanceApprovalVO>> listFinanceApprovals(@RequestParam(defaultValue = "pending") String scope,
                                                                @RequestParam(required = false) Integer status,
                                                                @RequestParam(required = false) Integer limit) {
        return Result.success(approvalService.listFinanceApprovals(scope, status, limit));
    }

    @GetMapping("/finance/{approvalCode}")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_FINANCE_DETAIL, message = "您没有权限查看财务审批详情")
    public Result<FinanceApprovalVO> getFinanceDetail(@PathVariable String approvalCode) {
        return Result.success(approvalService.getFinanceDetail(approvalCode));
    }

    @PostMapping("/finance/audit")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_FINANCE_AUDIT, message = "您没有权限审批财务单")
    @CollectLog(module = "approval", action = "audit_finance", bizType = "finance_approval", bizNo = "#request.approvalCode", description = "管理端审批财务单")
    public Result<Void> auditFinance(@Valid @RequestBody FinanceAuditRequest request) {
        approvalService.auditFinance(request);
        return Result.success(null);
    }

    @PostMapping("/finance")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_FINANCE_SUBMIT, message = "您没有权限提交财务审批")
    @CollectLog(module = "approval", action = "submit_finance", bizType = "finance_approval", description = "管理端提交财务审批")
    public Result<String> submitFinance(@Valid @RequestBody FinanceSubmitRequest request) {
        return Result.success(approvalService.submitFinance(request));
    }

    @PostMapping("/finance/attachment")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_FINANCE_SUBMIT, message = "您没有权限上传财务审批附件")
    @CollectLog(module = "approval", action = "upload_finance_attachment", bizType = "finance_approval_attachment", description = "管理端上传财务审批附件")
    public Result<BusinessAttachmentVO> uploadFinanceAttachment(@RequestParam("file") MultipartFile file) {
        return Result.success(businessAttachmentService.upload(file, "finance"));
    }

    @GetMapping("/finance/attachment")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_FINANCE_DETAIL, message = "您没有权限下载财务审批附件")
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

    @GetMapping("/resignation")
    @RequirePermission(value = {
            PermissionCatalogV3.CODE_APPROVAL_RESIGNATION_SUBMIT,
            PermissionCatalogV3.CODE_APPROVAL_RESIGNATION_LIST,
            PermissionCatalogV3.CODE_APPROVAL_RESIGNATION_AUDIT
    }, message = "您没有权限查看离职审批列表")
    public Result<List<ResignationApprovalVO>> listResignationApprovals(@RequestParam(defaultValue = "pending") String scope,
                                                                        @RequestParam(required = false) Integer status,
                                                                        @RequestParam(required = false) Integer limit) {
        return Result.success(approvalService.listResignationApprovals(scope, status, limit));
    }

    @GetMapping("/resignation/{resignationCode}")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_RESIGNATION_DETAIL, message = "您没有权限查看离职审批详情")
    public Result<ResignationApprovalVO> getResignationDetail(@NotBlank @PathVariable String resignationCode) {
        return Result.success(approvalService.getResignationDetail(resignationCode));
    }

    @PostMapping("/resignation")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_RESIGNATION_SUBMIT, message = "您没有权限提交离职审批")
    @CollectLog(module = "approval", action = "submit_resignation", bizType = "resignation_approval", description = "管理端提交离职审批")
    public Result<String> submitResignation(@Valid @RequestBody ResignationSubmitRequest request) {
        return Result.success(approvalService.submitResignation(request));
    }

    @PostMapping("/resignation/audit")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_RESIGNATION_AUDIT, message = "您没有权限审批离职单")
    @CollectLog(module = "approval", action = "audit_resignation", bizType = "resignation_approval", bizNo = "#request.resignationCode", description = "管理端审批离职单")
    public Result<Void> auditResignation(@Valid @RequestBody ResignationAuditRequest request) {
        approvalService.auditResignation(request);
        return Result.success(null);
    }

    @GetMapping("/quality")
    @RequirePermission(value = PermissionCatalogV3.CODE_QUALITY_AUDIT, message = "您没有权限查看质量审核列表")
    public Result<List<QualityApprovalVO>> listQualityApprovals(@RequestParam(required = false) Integer limit) {
        return Result.success(approvalService.listQualityApprovals(limit));
    }

    @GetMapping("/quality/{defectiveId}")
    @RequirePermission(value = PermissionCatalogV3.CODE_QUALITY_AUDIT, message = "您没有权限查看质量审核详情")
    public Result<QualityApprovalVO> getQualityApprovalDetail(@NotBlank @PathVariable String defectiveId) {
        return Result.success(approvalService.getQualityApprovalDetail(defectiveId));
    }

    @PostMapping("/quality/audit")
    @RequirePermission(value = PermissionCatalogV3.CODE_QUALITY_AUDIT, message = "您没有权限处理质量审核")
    @CollectLog(module = "approval", action = "audit_quality", bizType = "quality_approval", bizNo = "#request.defectiveId", description = "管理端审核质量处理")
    public Result<Void> auditQuality(@Valid @RequestBody QualityAuditRequest request) {
        approvalService.auditQuality(request);
        return Result.success(null);
    }

    @GetMapping("/order")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_LIST, message = "您没有权限查看订单审批列表")
    public Result<List<OrderApprovalVO>> listOrderApprovals(@RequestParam(required = false) Integer limit) {
        return Result.success(approvalService.listOrderApprovals(limit));
    }

    @GetMapping("/order/{orderType}/{orderId}")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_LIST, message = "您没有权限查看订单审批详情")
    public Result<OrderApprovalVO> getOrderApprovalDetail(@NotBlank @PathVariable String orderType,
                                                          @NotBlank @PathVariable String orderId) {
        return Result.success(approvalService.getOrderApprovalDetail(orderType, orderId));
    }

    @PostMapping("/order/audit")
    @RequirePermission(value = PermissionCatalogV3.CODE_APPROVAL_LIST, message = "您没有权限处理订单审批")
    @CollectLog(module = "approval", action = "audit_order", bizType = "order_approval", bizNo = "#request.orderId", description = "管理端确认待审批订单")
    public Result<Void> auditOrder(@Valid @RequestBody OrderApprovalAuditRequest request) {
        approvalService.auditOrder(request);
        return Result.success(null);
    }
}
