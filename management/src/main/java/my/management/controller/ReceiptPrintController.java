package my.management.controller;

import jakarta.annotation.Resource;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.shared.dto.Result;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import my.management.module.receipt.model.dto.OutboundPrintUpdateRequest;
import my.management.module.receipt.model.vo.OutboundPrintCommandVO;
import my.management.module.receipt.model.vo.OutboundPrintDetailVO;
import my.management.module.receipt.model.vo.OutboundPrintOrderVO;
import my.management.module.receipt.service.ReceiptPrintService;
import my.management.module.label.model.dto.LabelTemplateSaveRequest;
import my.management.module.label.model.vo.LabelTemplateVO;
import my.management.module.label.model.vo.LabelTemplateVariableVO;
import my.management.module.label.service.LabelTemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * ReceiptPrintController 是管理端后端请求入口控制类，负责接收请求并调用对应服务。
 */
@RestController
@RequestMapping("/receipt")
@RequireTenantFeature(TenantFeatureEnum.CODE_RECEIPT)
public class ReceiptPrintController {

    @Resource
    private ReceiptPrintService receiptPrintService;

    @Resource
    private LabelTemplateService labelTemplateService;

    @GetMapping("/print/pending")
    @RequirePermission(value = PermissionCatalogV3.CODE_PRINT_RECEIPT_LIST, message = "您没有权限查看待打印出库单")
    public Result<List<OutboundPrintOrderVO>> pendingList() {
        return Result.success(receiptPrintService.pendingList());
    }

    @GetMapping("/print/detail")
    @RequirePermission(value = PermissionCatalogV3.CODE_PRINT_RECEIPT_DETAIL, message = "您没有权限查看出库单详情")
    public Result<OutboundPrintDetailVO> detail(@RequestParam String orderNo) {
        return Result.success(receiptPrintService.detail(orderNo));
    }

    @GetMapping("/print/raw-command")
    @RequirePermission(value = PermissionCatalogV3.CODE_PRINT_RECEIPT_EXECUTE, message = "您没有权限生成打印指令")
    public Result<OutboundPrintCommandVO> rawCommand(@RequestParam String orderNo) {
        return Result.success(receiptPrintService.rawCommand(orderNo));
    }

    @PostMapping("/print/update")
    @RequirePermission(value = PermissionCatalogV3.CODE_PRINT_RECEIPT_UPDATE, message = "您没有权限修正出库单打印内容")
    @CollectLog(module = "receipt_print", action = "update_print_detail", bizType = "outbound_print", bizNo = "#request.orderNo", description = "管理端修改出库单打印内容")
    public Result<OutboundPrintDetailVO> updatePrintDetail(@RequestBody OutboundPrintUpdateRequest request) {
        return Result.success(receiptPrintService.updatePrintDetail(request));
    }

    @PostMapping("/print/mark-printed")
    @RequirePermission(value = PermissionCatalogV3.CODE_PRINT_RECEIPT_EXECUTE, message = "您没有权限标记打印完成")
    @CollectLog(module = "receipt_print", action = "mark_printed", bizType = "outbound_print", bizNo = "#orderNo", description = "管理端标记出库单已打印")
    public Result<Void> markPrinted(@RequestParam String orderNo) {
        receiptPrintService.markPrinted(orderNo);
        return Result.success(null);
    }

    @PostMapping("/print/cancel")
    @RequirePermission(value = PermissionCatalogV3.CODE_PRINT_RECEIPT_CANCEL, message = "您没有权限作废出库单")
    @CollectLog(module = "receipt_print", action = "cancel", bizType = "outbound_print", bizNo = "#orderNo", description = "管理端作废出库单")
    public Result<Void> cancel(@RequestParam String orderNo) {
        receiptPrintService.cancel(orderNo);
        return Result.success(null);
    }

    @GetMapping("/template/variables")
    @RequirePermission(value = PermissionCatalogV3.CODE_PRINT_RECEIPT_LIST, message = "您没有权限查看出库单模板变量")
    public Result<List<LabelTemplateVariableVO>> templateVariables() {
        return Result.success(labelTemplateService.variables("receipt"));
    }

    @GetMapping("/template/list")
    @RequirePermission(value = PermissionCatalogV3.CODE_PRINT_RECEIPT_LIST, message = "您没有权限查看出库单模板")
    public Result<List<LabelTemplateVO>> templateList() {
        return Result.success(labelTemplateService.list("receipt"));
    }

    @PostMapping("/template/save")
    @RequirePermission(value = PermissionCatalogV3.CODE_PRINT_RECEIPT_UPDATE, message = "您没有权限保存出库单模板")
    @CollectLog(module = "receipt_template", action = "save", bizType = "receipt_template", bizNo = "#request.id", description = "管理端保存出库单模板")
    public Result<LabelTemplateVO> saveTemplate(@RequestBody LabelTemplateSaveRequest request) {
        request.setPrintType("receipt");
        return Result.success(labelTemplateService.save(request));
    }

    @PostMapping("/template/{id}/default")
    @RequirePermission(value = PermissionCatalogV3.CODE_PRINT_RECEIPT_UPDATE, message = "您没有权限设置默认出库单模板")
    @CollectLog(module = "receipt_template", action = "set_default", bizType = "receipt_template", bizNo = "#id", description = "管理端设置默认出库单模板")
    public Result<Void> setDefaultTemplate(@PathVariable Long id) {
        labelTemplateService.setDefault(id);
        return Result.success(null);
    }
}
