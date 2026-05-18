package my.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.hive.common.annotation.CollectLog;
import my.hive.common.annotation.RequirePermission;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import my.management.module.order.model.dto.ProductionOrderPageRequest;
import my.management.module.order.model.dto.ProductionOrderSaveRequest;
import my.management.module.order.model.dto.ProductionOrderUpdateRequest;
import my.management.module.order.model.dto.OrderWarningSettingUpdateRequest;
import my.management.module.order.model.dto.SalesOrderPageRequest;
import my.management.module.order.model.dto.SalesOrderSaveRequest;
import my.management.module.order.model.dto.SalesOrderUpdateRequest;
import my.management.module.order.model.vo.ProductionOrderDetailVO;
import my.management.module.order.model.vo.ProductionOrderPageVO;
import my.management.module.order.model.vo.ProductionOrderStatusLogVO;
import my.management.module.order.model.vo.OrderWarningSettingVO;
import my.management.module.order.model.vo.OrderWarningSummaryVO;
import my.management.module.order.model.vo.SalesOrderAttachmentVO;
import my.management.module.order.model.vo.SalesOrderDetailVO;
import my.management.module.order.model.vo.SalesOrderPageVO;
import my.management.module.order.model.vo.SalesOrderStatusLogVO;
import my.management.module.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.util.Map;

/**
 * 订单管理控制器，面向管理端提供销售订单和生产订单维护接口。
 */
@RestController
@RequestMapping("/order")
@RequireTenantFeature(TenantFeatureEnum.CODE_ORDER)
@Validated
public class OrderController {

    @Resource
    private OrderService orderService;

    @GetMapping("/sales/page")
    @RequirePermission(value = PermissionCodeEnum.CODE_SALES_ORDER_LIST, message = "您没有权限查看销售订单列表")
    public Result<PageResult<SalesOrderPageVO>> salesPage(SalesOrderPageRequest request) {
        return Result.success(toPageResult(orderService.pageSalesOrders(request)));
    }

    @GetMapping("/sales/status-summary")
    @RequirePermission(value = PermissionCodeEnum.CODE_SALES_ORDER_LIST, message = "您没有权限查看销售订单统计")
    public Result<Map<String, Long>> salesStatusSummary() {
        return Result.success(orderService.countSalesOrderStatuses());
    }

    @GetMapping("/sales/detail/{orderId}")
    @RequirePermission(value = PermissionCodeEnum.CODE_SALES_ORDER_DETAIL, message = "您没有权限查看销售订单详情")
    public Result<SalesOrderDetailVO> salesDetail(@PathVariable String orderId) {
        return Result.success(orderService.getSalesOrderDetail(orderId));
    }

    @PostMapping("/sales/create")
    @RequirePermission(value = PermissionCodeEnum.CODE_SALES_ORDER_STATUS, message = "您没有权限创建销售订单")
    @CollectLog(module = "order", action = "create_sales", bizType = "sales_order", description = "管理端创建销售订单")
    public Result<String> createSales(@RequestBody @Valid SalesOrderSaveRequest request) {
        return Result.success(orderService.createSalesOrder(request));
    }

    @PostMapping("/sales/attachment/upload")
    @RequirePermission(value = PermissionCodeEnum.CODE_SALES_ORDER_STATUS, message = "您没有权限上传销售订单附件")
    @CollectLog(module = "order", action = "upload_sales_attachment", bizType = "sales_order_attachment", description = "管理端上传销售订单附件")
    public Result<SalesOrderAttachmentVO> uploadSalesAttachment(@RequestParam("file") MultipartFile file) {
        return Result.success(orderService.uploadSalesAttachment(file));
    }

    @GetMapping("/sales/attachment/download")
    @RequirePermission(value = PermissionCodeEnum.CODE_SALES_ORDER_DETAIL, message = "您没有权限下载销售订单附件")
    public ResponseEntity<org.springframework.core.io.Resource> downloadSalesAttachment(@RequestParam String url,
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

    @PostMapping("/sales/save/{orderId}")
    @RequirePermission(value = PermissionCodeEnum.CODE_SALES_ORDER_STATUS, message = "您没有权限编辑销售订单")
    @CollectLog(module = "order", action = "save_sales", bizType = "sales_order", bizNo = "#orderId", description = "管理端保存销售订单")
    public Result<Void> saveSales(@PathVariable String orderId, @RequestBody @Valid SalesOrderSaveRequest request) {
        orderService.saveSalesOrder(orderId, request);
        return Result.success(null);
    }

    @PostMapping("/sales/update/{orderId}")
    @RequirePermission(value = PermissionCodeEnum.CODE_SALES_ORDER_STATUS, message = "您没有权限更新销售订单")
    @CollectLog(module = "order", action = "update_sales_status", bizType = "sales_order", bizNo = "#orderId", description = "管理端更新销售订单状态")
    public Result<Void> updateSales(@PathVariable String orderId, @RequestBody SalesOrderUpdateRequest request) {
        orderService.updateSalesOrder(orderId, request);
        return Result.success(null);
    }

    @GetMapping("/sales/log/{orderId}")
    @RequirePermission(value = PermissionCodeEnum.CODE_SALES_ORDER_DETAIL, message = "您没有权限查看销售订单日志")
    public Result<List<SalesOrderStatusLogVO>> salesLog(@PathVariable String orderId) {
        return Result.success(orderService.listSalesLogs(orderId));
    }

    @GetMapping("/production/page")
    @RequirePermission(value = PermissionCodeEnum.CODE_PRODUCTION_ORDER_LIST, message = "您没有权限查看生产订单列表")
    public Result<PageResult<ProductionOrderPageVO>> productionPage(ProductionOrderPageRequest request) {
        return Result.success(toPageResult(orderService.pageProductionOrders(request)));
    }

    @GetMapping("/production/status-summary")
    @RequirePermission(value = PermissionCodeEnum.CODE_PRODUCTION_ORDER_LIST, message = "您没有权限查看生产订单统计")
    public Result<Map<String, Long>> productionStatusSummary() {
        return Result.success(orderService.countProductionOrderStatuses());
    }

    @GetMapping("/warning/setting")
    @RequirePermission(value = PermissionCodeEnum.CODE_ORDER_WARNING_SETTING, message = "您没有权限查看订单预警设置")
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
    @RequirePermission(value = PermissionCodeEnum.CODE_SALES_ORDER_LIST, message = "您没有权限查看订单预警统计")
    public Result<OrderWarningSummaryVO> warningSummary() {
        return Result.success(orderService.getOrderWarningSummary());
    }

    @GetMapping("/production/detail/{orderId}")
    @RequirePermission(value = PermissionCodeEnum.CODE_PRODUCTION_ORDER_DETAIL, message = "您没有权限查看生产订单详情")
    public Result<ProductionOrderDetailVO> productionDetail(@PathVariable String orderId) {
        return Result.success(orderService.getProductionOrderDetail(orderId));
    }

    @PostMapping("/production/create")
    @RequirePermission(value = PermissionCodeEnum.CODE_PRODUCTION_ORDER_STATUS, message = "您没有权限创建生产订单")
    @CollectLog(module = "order", action = "create_production", bizType = "production_order", description = "管理端创建生产订单")
    public Result<String> createProduction(@RequestBody @Valid ProductionOrderSaveRequest request) {
        return Result.success(orderService.createProductionOrder(request));
    }

    @PostMapping("/production/save/{orderId}")
    @RequirePermission(value = PermissionCodeEnum.CODE_PRODUCTION_ORDER_STATUS, message = "您没有权限编辑生产订单")
    @CollectLog(module = "order", action = "save_production", bizType = "production_order", bizNo = "#orderId", description = "管理端保存生产订单")
    public Result<Void> saveProduction(@PathVariable String orderId, @RequestBody @Valid ProductionOrderSaveRequest request) {
        orderService.saveProductionOrder(orderId, request);
        return Result.success(null);
    }

    @GetMapping("/production/log/{orderId}")
    @RequirePermission(value = PermissionCodeEnum.CODE_PRODUCTION_ORDER_LOG, message = "您没有权限查看生产订单日志")
    public Result<List<ProductionOrderStatusLogVO>> productionLog(@PathVariable String orderId) {
        return Result.success(orderService.listProductionLogs(orderId));
    }

    @PostMapping("/production/update/{orderId}")
    @RequirePermission(value = PermissionCodeEnum.CODE_PRODUCTION_ORDER_STATUS, message = "您没有权限更新生产订单")
    @CollectLog(module = "order", action = "update_production_status", bizType = "production_order", bizNo = "#orderId", description = "管理端更新生产订单状态")
    public Result<Void> updateProduction(@PathVariable String orderId, @RequestBody ProductionOrderUpdateRequest request) {
        orderService.updateProductionOrder(orderId, request);
        return Result.success(null);
    }

    @GetMapping("/health")
    @RequirePermission(value = PermissionCodeEnum.CODE_SALES_ORDER_LIST, message = "您没有权限查看订单模块检查结果")
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
