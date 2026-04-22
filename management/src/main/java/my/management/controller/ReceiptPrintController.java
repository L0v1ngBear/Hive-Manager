package my.management.controller;

import jakarta.annotation.Resource;
import my.hive.common.annotation.RequirePermission;
import my.hive.common.dto.Result;
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
public class ReceiptPrintController {

    @Resource
    private ReceiptPrintService receiptPrintService;

    @Resource
    private LabelTemplateService labelTemplateService;

    @GetMapping("/print/pending")
    @RequirePermission(value = "receipt:print:list", message = "您没有权限查看待打印出库单")
    public Result<List<OutboundPrintOrderVO>> pendingList() {
        return Result.success(receiptPrintService.pendingList());
    }

    @GetMapping("/print/detail")
    @RequirePermission(value = "receipt:print:detail", message = "您没有权限查看出库单详情")
    public Result<OutboundPrintDetailVO> detail(@RequestParam String orderNo) {
        return Result.success(receiptPrintService.detail(orderNo));
    }

    @GetMapping("/print/raw-command")
    @RequirePermission(value = "receipt:print:detail", message = "您没有权限生成打印指令")
    public Result<OutboundPrintCommandVO> rawCommand(@RequestParam String orderNo) {
        return Result.success(receiptPrintService.rawCommand(orderNo));
    }

    @PostMapping("/print/mark-printed")
    @RequirePermission(value = "receipt:print:mark", message = "您没有权限标记打印完成")
    public Result<Void> markPrinted(@RequestParam String orderNo) {
        receiptPrintService.markPrinted(orderNo);
        return Result.success(null);
    }

    @PostMapping("/print/cancel")
    @RequirePermission(value = "receipt:print:cancel", message = "您没有权限作废出库单")
    public Result<Void> cancel(@RequestParam String orderNo) {
        receiptPrintService.cancel(orderNo);
        return Result.success(null);
    }

    @GetMapping("/template/variables")
    @RequirePermission(value = "receipt:print:list", message = "您没有权限查看出库单模板变量")
    public Result<List<LabelTemplateVariableVO>> templateVariables() {
        return Result.success(labelTemplateService.variables("receipt"));
    }

    @GetMapping("/template/list")
    @RequirePermission(value = "receipt:print:list", message = "您没有权限查看出库单模板")
    public Result<List<LabelTemplateVO>> templateList() {
        return Result.success(labelTemplateService.list("receipt"));
    }

    @PostMapping("/template/save")
    @RequirePermission(value = "receipt:print:mark", message = "您没有权限保存出库单模板")
    public Result<LabelTemplateVO> saveTemplate(@RequestBody LabelTemplateSaveRequest request) {
        request.setPrintType("receipt");
        return Result.success(labelTemplateService.save(request));
    }

    @PostMapping("/template/{id}/default")
    @RequirePermission(value = "receipt:print:mark", message = "您没有权限设置默认出库单模板")
    public Result<Void> setDefaultTemplate(@PathVariable Long id) {
        labelTemplateService.setDefault(id);
        return Result.success(null);
    }
}
