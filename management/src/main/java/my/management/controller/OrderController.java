package my.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.common.annotation.CollectLog;
import my.hive.common.annotation.RequirePermission;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.order.model.dto.OrderFlowPrintTaskRequest;
import my.management.module.order.model.dto.OrderStatusLogTimeCorrectionRequest;
import my.management.module.order.model.dto.OrderWarningSettingUpdateRequest;
import my.management.module.order.model.dto.SalesOrderPageRequest;
import my.management.module.order.model.dto.SalesOrderSaveRequest;
import my.management.module.order.model.dto.SalesOrderUpdateRequest;
import my.management.module.order.model.vo.OrderFlowPrintTaskVO;
import my.management.module.order.model.vo.OrderWarningSettingVO;
import my.management.module.order.model.vo.OrderWarningSummaryVO;
import my.management.module.order.model.vo.SalesOrderAttachmentVO;
import my.management.module.order.model.vo.SalesOrderDetailVO;
import my.management.module.order.model.vo.SalesOrderPageVO;
import my.management.module.order.service.OrderService;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
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
import java.util.Map;

/**
 * Single public order API. Production records remain internal fulfillment details.
 */
@RestController
@RequestMapping("/order")
@RequireTenantFeature(TenantFeatureEnum.CODE_ORDER)
@Validated
public class OrderController {

    @Resource
    private OrderService orderService;

    @GetMapping("/page")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_LIST, message = "您没有权限查看订单列表")
    public Result<PageResult<SalesOrderPageVO>> page(SalesOrderPageRequest request) {
        return Result.success(toPageResult(orderService.pageSalesOrders(request)));
    }

    @GetMapping("/status-summary")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_LIST, message = "您没有权限查看订单统计")
    public Result<Map<String, Long>> statusSummary() {
        return Result.success(orderService.countSalesOrderStatuses());
    }

    @GetMapping("/detail/{orderId}")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_DETAIL, message = "您没有权限查看订单详情")
    public Result<SalesOrderDetailVO> detail(@PathVariable String orderId) {
        return Result.success(orderService.getSalesOrderDetail(orderId));
    }

    @PostMapping("/create")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_CREATE, message = "您没有权限创建订单")
    @CollectLog(module = "order", action = "create_order", bizType = "order", description = "管理端创建订单")
    public Result<String> create(@RequestBody @Valid SalesOrderSaveRequest request) {
        request.setCreateProductionOrder(1);
        return Result.success(orderService.createSalesOrder(request));
    }

    @PostMapping("/attachment/upload")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_CREATE, message = "您没有权限上传订单附件")
    @CollectLog(module = "order", action = "upload_order_attachment", bizType = "order_attachment", description = "管理端上传订单附件")
    public Result<SalesOrderAttachmentVO> uploadAttachment(@RequestParam("file") MultipartFile file) {
        return Result.success(orderService.uploadSalesAttachment(file));
    }

    @GetMapping("/attachment/download")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_DETAIL, message = "您没有权限下载订单附件")
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

    @PostMapping("/save/{orderId}")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_UPDATE, message = "您没有权限编辑订单")
    @CollectLog(module = "order", action = "save_order", bizType = "order", bizNo = "#orderId", description = "管理端保存订单")
    public Result<Void> save(@PathVariable String orderId, @RequestBody @Valid SalesOrderSaveRequest request) {
        orderService.saveSalesOrder(orderId, request);
        return Result.success(null);
    }

    @PostMapping("/update/{orderId}")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_UPDATE, message = "您没有权限更新订单")
    @CollectLog(module = "order", action = "update_order_status", bizType = "order", bizNo = "#orderId", description = "管理端更新订单状态")
    public Result<Void> update(@PathVariable String orderId, @RequestBody SalesOrderUpdateRequest request) {
        orderService.updateSalesOrder(orderId, request);
        return Result.success(null);
    }

    @PostMapping("/next/{orderId}")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_UPDATE, message = "您没有权限推进订单")
    @CollectLog(module = "order", action = "advance_order_status", bizType = "order", bizNo = "#orderId", description = "管理端推进订单到下一阶段")
    public Result<Void> advance(@PathVariable String orderId,
                                @RequestBody(required = false) SalesOrderUpdateRequest request) {
        orderService.advanceSalesOrderToNextStage(orderId, request == null ? new SalesOrderUpdateRequest() : request);
        return Result.success(null);
    }

    @PostMapping("/rollback/{orderId}")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_UPDATE, message = "您没有权限提交订单回退审批")
    @CollectLog(module = "order", action = "submit_order_rollback", bizType = "order", bizNo = "#orderId", description = "管理端提交订单回退审批")
    public Result<Void> rollback(@PathVariable String orderId,
                                 @RequestBody(required = false) SalesOrderUpdateRequest request) {
        orderService.submitSalesOrderRollbackApproval(orderId, request == null ? new SalesOrderUpdateRequest() : request);
        return Result.success(null);
    }

    @PostMapping("/log/{logId}/time")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_UPDATE, message = "您没有权限修正订单流转时间")
    public Result<Void> correctLogTime(@PathVariable Long logId,
                                       @RequestBody @Valid OrderStatusLogTimeCorrectionRequest request) {
        orderService.correctSalesLogTime(logId, request);
        return Result.success(null);
    }

    @PostMapping("/flow-print-task")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_PRINT, message = "您没有权限生成订单流转码")
    @CollectLog(module = "order", action = "create_order_flow_print_task", bizType = "order", bizNo = "#request.orderId", description = "管理端创建订单流转码打印任务")
    public Result<OrderFlowPrintTaskVO> createFlowPrintTask(@RequestBody @Valid OrderFlowPrintTaskRequest request) {
        return Result.success(orderService.createSalesOrderFlowPrintTask(request));
    }

    @GetMapping("/warning/setting")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_WARNING_LIST, message = "您没有权限查看订单预警设置")
    public Result<OrderWarningSettingVO> warningSetting() {
        return Result.success(orderService.getOrderWarningSetting());
    }

    @PostMapping("/warning/setting")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_WARNING_SETTING, message = "您没有权限维护订单预警设置")
    @CollectLog(module = "order", action = "update_warning_setting", bizType = "order_warning_setting", description = "管理端维护订单未更新预警设置")
    public Result<OrderWarningSettingVO> updateWarningSetting(@RequestBody @Valid OrderWarningSettingUpdateRequest request) {
        return Result.success(orderService.updateOrderWarningSetting(request));
    }

    @GetMapping("/warning/summary")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_WARNING_LIST, message = "您没有权限查看订单预警统计")
    public Result<OrderWarningSummaryVO> warningSummary() {
        return Result.success(orderService.getOrderWarningSummary());
    }

    @PostMapping("/warning/refresh")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_WARNING_LIST, message = "您没有权限刷新订单预警")
    @CollectLog(module = "order", action = "refresh_warning", bizType = "order_warning", description = "管理端重新更新订单预警")
    public Result<OrderWarningSummaryVO> refreshWarningSummary() {
        return Result.success(orderService.refreshOrderWarningSummary());
    }

    @PostMapping("/warning/refresh-all")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_WARNING_LIST, message = "您没有权限刷新订单预警")
    @CollectLog(module = "order", action = "refresh_order_warning", bizType = "order_warning", description = "管理端重新计时订单预警")
    public Result<OrderWarningSummaryVO> refreshWarnings() {
        return Result.success(orderService.refreshOrderWarnings());
    }

    @PostMapping("/warning/{orderId}/refresh")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_WARNING_LIST, message = "您没有权限刷新订单预警")
    @CollectLog(module = "order", action = "refresh_single_order_warning", bizType = "order", bizNo = "#orderId", description = "管理端重新计时单个订单预警")
    public Result<OrderWarningSummaryVO> refreshOrderWarning(@PathVariable String orderId) {
        return Result.success(orderService.refreshOrderWarning(orderId));
    }

    @GetMapping("/health")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_LIST, message = "您没有权限查看订单模块检查结果")
    public Result<Map<String, Object>> health() {
        return Result.success(orderService.checkOrderTables());
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
