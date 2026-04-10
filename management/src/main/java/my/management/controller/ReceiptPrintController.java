package my.management.controller;

import jakarta.annotation.Resource;
import my.management.common.dto.Result;
import my.management.module.receipt.model.vo.OutboundPrintDetailVO;
import my.management.module.receipt.model.vo.OutboundPrintOrderVO;
import my.management.module.receipt.service.ReceiptPrintService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/receipt")
public class ReceiptPrintController {

    @Resource
    private ReceiptPrintService receiptPrintService;

    @GetMapping("/print/pending")
    public Result<List<OutboundPrintOrderVO>> pendingList() {
        return Result.success(receiptPrintService.pendingList());
    }

    @GetMapping("/print/detail")
    public Result<OutboundPrintDetailVO> detail(@RequestParam String orderNo) {
        return Result.success(receiptPrintService.detail(orderNo));
    }

    @PostMapping("/print/mark-printed")
    public Result<Void> markPrinted(@RequestParam String orderNo) {
        receiptPrintService.markPrinted(orderNo);
        return Result.success(null);
    }

    @PostMapping("/print/cancel")
    public Result<Void> cancel(@RequestParam String orderNo) {
        receiptPrintService.cancel(orderNo);
        return Result.success(null);
    }
}