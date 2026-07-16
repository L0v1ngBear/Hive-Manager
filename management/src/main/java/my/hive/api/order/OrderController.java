package my.hive.api.order;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.dto.PageResult;
import my.hive.shared.dto.Result;
import my.hive.shared.tenant.RequireTenantFeature;
import my.hive.domain.order.model.dto.OrderFlowPrintTaskRequest;
import my.hive.domain.order.model.dto.OrderStatusLogTimeCorrectionRequest;
import my.hive.domain.order.model.dto.OrderWarningSettingUpdateRequest;
import my.hive.domain.order.model.dto.ProductionOrderUpdateRequest;
import my.hive.domain.order.model.dto.SalesOrderPageRequest;
import my.hive.domain.order.model.dto.SalesOrderSaveRequest;
import my.hive.domain.order.model.dto.SalesOrderUpdateRequest;
import my.hive.domain.order.model.vo.OrderFlowPrintTaskVO;
import my.hive.domain.order.model.vo.OrderWarningSettingVO;
import my.hive.domain.order.model.vo.OrderWarningSummaryVO;
import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.domain.order.model.vo.SalesOrderAttachmentVO;
import my.hive.domain.order.model.vo.SalesOrderDetailVO;
import my.hive.domain.order.model.vo.SalesOrderPageVO;
import my.hive.domain.order.service.OrderService;
import my.hive.domain.order.service.OrderLogisticsTrackingService;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.domain.tenant.model.enums.TenantFeatureEnum;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Single public order API. Production records remain internal fulfillment details.
 */
@RestController
@RequestMapping("/orders")
@RequireTenantFeature(TenantFeatureEnum.CODE_ORDER)
@Validated
public class OrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private OrderLogisticsTrackingService orderLogisticsTrackingService;

    @GetMapping
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_LIST, message = "您没有权限查看订单列表")
    public Result<PageResult<SalesOrderPageVO>> page(SalesOrderPageRequest request) {
        return Result.success(toPageResult(orderService.pageSalesOrders(request)));
    }

    @GetMapping("/status-summary")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_LIST, message = "您没有权限查看订单统计")
    public Result<Map<String, Long>> statusSummary() {
        return Result.success(orderService.countSalesOrderStatuses());
    }

    @GetMapping("/{orderId}")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_DETAIL, message = "您没有权限查看订单详情")
    public Result<SalesOrderDetailVO> detail(@PathVariable String orderId) {
        return Result.success(orderService.getSalesOrderDetail(orderId));
    }

    @GetMapping("/{orderId}/logistics-tracking")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_DETAIL, message = "您没有权限查看订单物流")
    public Result<OrderLogisticsTrackingVO> logisticsTracking(@PathVariable String orderId) {
        return Result.success(orderLogisticsTrackingService.getTracking(orderId));
    }

    @PostMapping
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_CREATE, message = "您没有权限创建订单")
    @CollectLog(module = "order", action = "create_order", bizType = "order", description = "管理端创建订单")
    public Result<String> create(@RequestBody @Valid SalesOrderSaveRequest request) {
        request.setCreateProductionOrder(1);
        return Result.success(orderService.createSalesOrder(request));
    }

    @PostMapping("/attachment")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_CREATE, message = "您没有权限上传订单附件")
    @CollectLog(module = "order", action = "upload_order_attachment", bizType = "order_attachment", description = "管理端上传订单附件")
    public Result<SalesOrderAttachmentVO> uploadAttachment(@RequestParam("file") MultipartFile file) {
        return Result.success(orderService.uploadSalesAttachment(file));
    }

    @GetMapping("/attachment")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_DETAIL, message = "您没有权限下载订单附件")
    public ResponseEntity<org.springframework.core.io.Resource> downloadAttachment(@RequestParam String url,
                                                                                   @RequestParam(required = false) String name) {
        org.springframework.core.io.Resource resource = orderService.loadSalesAttachment(url);
        String filename = name != null && !name.isBlank() ? name.trim() : resource.getFilename();
        String encodedFilename = URLEncoder.encode(filename == null ? "order-attachment" : filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @PutMapping("/{orderId}")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_UPDATE, message = "您没有权限编辑订单")
    @CollectLog(module = "order", action = "save_order", bizType = "order", bizNo = "#orderId", description = "保存订单")
    public Result<Void> replace(@PathVariable String orderId, @RequestBody @Valid SalesOrderSaveRequest request) {
        orderService.saveSalesOrder(orderId, request);
        return Result.success(null);
    }

    @PostMapping("/{orderId}/status")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_UPDATE, message = "您没有权限更新订单")
    @CollectLog(module = "order", action = "update_order_status", bizType = "order", bizNo = "#orderId", description = "管理端更新订单状态")
    public Result<Void> update(@PathVariable String orderId, @RequestBody SalesOrderUpdateRequest request) {
        orderService.updateSalesOrder(orderId, request);
        return Result.success(null);
    }

    @PostMapping("/{orderId}/process")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_UPDATE, message = "您没有权限更新订单生产进度")
    @CollectLog(module = "order", action = "update_order_process", bizType = "order", bizNo = "#orderId", description = "更新统一订单生产进度")
    public Result<Void> updateProcess(@PathVariable String orderId, @RequestBody ProductionOrderUpdateRequest request) {
        orderService.updateSalesOrderProcess(orderId, request);
        return Result.success(null);
    }

    @PostMapping("/{orderId}/advance")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_UPDATE, message = "您没有权限推进订单")
    @CollectLog(module = "order", action = "advance_order_status", bizType = "order", bizNo = "#orderId", description = "管理端推进订单到下一阶段")
    public Result<Void> advance(@PathVariable String orderId,
                                @RequestBody(required = false) SalesOrderUpdateRequest request) {
        orderService.advanceSalesOrderToNextStage(orderId, request == null ? new SalesOrderUpdateRequest() : request);
        return Result.success(null);
    }

    @PostMapping("/{orderId}/rollback")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_UPDATE, message = "您没有权限提交订单回退审批")
    @CollectLog(module = "order", action = "submit_order_rollback", bizType = "order", bizNo = "#orderId", description = "管理端提交订单回退审批")
    public Result<Void> rollback(@PathVariable String orderId,
                                 @RequestBody(required = false) SalesOrderUpdateRequest request) {
        orderService.submitSalesOrderRollbackApproval(orderId, request == null ? new SalesOrderUpdateRequest() : request);
        return Result.success(null);
    }

    @PostMapping("/status-log/{logId}/time")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_UPDATE, message = "您没有权限修正订单流转时间")
    public Result<Void> correctLogTime(@PathVariable Long logId,
                                       @RequestBody @Valid OrderStatusLogTimeCorrectionRequest request) {
        orderService.correctSalesLogTime(logId, request);
        return Result.success(null);
    }

    @GetMapping("/{orderId}/status-log")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_DETAIL, message = "您没有权限查看订单流转记录")
    public Result<java.util.List<my.hive.domain.order.model.vo.SalesOrderStatusLogVO>> statusLog(
            @PathVariable String orderId) {
        return Result.success(orderService.listSalesLogs(orderId));
    }

    @PostMapping("/flow/{flowCode}/advance")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_UPDATE, message = "您没有权限推进订单")
    @CollectLog(module = "order", action = "scan_advance_order", bizType = "order", description = "扫码推进订单", recordArgs = false)
    public Result<Void> advanceByFlowCode(@PathVariable String flowCode) {
        orderService.advanceSalesOrderByFlowCode(flowCode);
        return Result.success(null);
    }

    @PostMapping("/flow-print-task")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_PRINT, message = "您没有权限生成订单流转码")
    @CollectLog(module = "order", action = "create_order_flow_print_task", bizType = "order", bizNo = "#request.orderId", description = "管理端创建订单流转码打印任务")
    public Result<OrderFlowPrintTaskVO> createFlowPrintTask(@RequestBody @Valid OrderFlowPrintTaskRequest request) {
        return Result.success(orderService.createSalesOrderFlowPrintTask(request));
    }

    @GetMapping("/warning/setting")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_WARNING_LIST, message = "您没有权限查看订单预警设置")
    public Result<OrderWarningSettingVO> warningSetting() {
        return Result.success(orderService.getOrderWarningSetting());
    }

    @PostMapping("/warning/setting")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_WARNING_SETTING, message = "您没有权限维护订单预警设置")
    @CollectLog(module = "order", action = "update_warning_setting", bizType = "order_warning_setting", description = "管理端维护订单未更新预警设置")
    public Result<OrderWarningSettingVO> updateWarningSetting(@RequestBody @Valid OrderWarningSettingUpdateRequest request) {
        return Result.success(orderService.updateOrderWarningSetting(request));
    }

    @GetMapping("/warning/summary")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_WARNING_LIST, message = "您没有权限查看订单预警统计")
    public Result<OrderWarningSummaryVO> warningSummary() {
        return Result.success(orderService.getOrderWarningSummary());
    }

    @PostMapping("/warning/refresh")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_WARNING_LIST, message = "您没有权限刷新订单预警")
    @CollectLog(module = "order", action = "refresh_warning", bizType = "order_warning", description = "管理端重新更新订单预警")
    public Result<OrderWarningSummaryVO> refreshWarningSummary() {
        return Result.success(orderService.refreshOrderWarningSummary());
    }

    @PostMapping("/warning/refresh-all")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_WARNING_LIST, message = "您没有权限刷新订单预警")
    @CollectLog(module = "order", action = "refresh_order_warning", bizType = "order_warning", description = "管理端重新计时订单预警")
    public Result<OrderWarningSummaryVO> refreshWarnings() {
        return Result.success(orderService.refreshOrderWarnings());
    }

    @PostMapping("/warning/{orderId}/refresh")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORDER_WARNING_LIST, message = "您没有权限刷新订单预警")
    @CollectLog(module = "order", action = "refresh_single_order_warning", bizType = "order", bizNo = "#orderId", description = "管理端重新计时单个订单预警")
    public Result<OrderWarningSummaryVO> refreshOrderWarning(@PathVariable String orderId) {
        return Result.success(orderService.refreshOrderWarning(orderId));
    }

    private <T> PageResult<T> toPageResult(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setData(page.getRecords());
        return result;
    }
}
