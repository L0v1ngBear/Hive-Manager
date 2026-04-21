package my.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.hive.common.annotation.RequirePermission;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.module.order.model.dto.ProductionOrderPageRequest;
import my.management.module.order.model.dto.ProductionOrderSaveRequest;
import my.management.module.order.model.dto.ProductionOrderUpdateRequest;
import my.management.module.order.model.dto.SalesOrderPageRequest;
import my.management.module.order.model.dto.SalesOrderSaveRequest;
import my.management.module.order.model.dto.SalesOrderUpdateRequest;
import my.management.module.order.model.vo.ProductionOrderDetailVO;
import my.management.module.order.model.vo.ProductionOrderPageVO;
import my.management.module.order.model.vo.ProductionOrderStatusLogVO;
import my.management.module.order.model.vo.SalesOrderDetailVO;
import my.management.module.order.model.vo.SalesOrderPageVO;
import my.management.module.order.model.vo.SalesOrderStatusLogVO;
import my.management.module.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 订单管理控制器，面向管理端提供销售订单和生产订单维护接口。
 */
@RestController
@RequestMapping("/order")
@Validated
public class OrderController {

    @Resource
    private OrderService orderService;

    @GetMapping("/sales/page")
    @RequirePermission(value = "sales:order:list", message = "您没有权限查看销售订单列表")
    public Result<PageResult<SalesOrderPageVO>> salesPage(SalesOrderPageRequest request) {
        return Result.success(toPageResult(orderService.pageSalesOrders(request)));
    }

    @GetMapping("/sales/detail/{orderId}")
    @RequirePermission(value = "sales:order:detail", message = "您没有权限查看销售订单详情")
    public Result<SalesOrderDetailVO> salesDetail(@PathVariable String orderId) {
        return Result.success(orderService.getSalesOrderDetail(orderId));
    }

    @PostMapping("/sales/create")
    @RequirePermission(value = "sales:order:status", message = "您没有权限创建销售订单")
    public Result<String> createSales(@RequestBody @Valid SalesOrderSaveRequest request) {
        return Result.success(orderService.createSalesOrder(request));
    }

    @PostMapping("/sales/save/{orderId}")
    @RequirePermission(value = "sales:order:status", message = "您没有权限编辑销售订单")
    public Result<Void> saveSales(@PathVariable String orderId, @RequestBody @Valid SalesOrderSaveRequest request) {
        orderService.saveSalesOrder(orderId, request);
        return Result.success(null);
    }

    @PostMapping("/sales/update/{orderId}")
    @RequirePermission(value = "sales:order:status", message = "您没有权限更新销售订单")
    public Result<Void> updateSales(@PathVariable String orderId, @RequestBody SalesOrderUpdateRequest request) {
        orderService.updateSalesOrder(orderId, request);
        return Result.success(null);
    }

    @GetMapping("/sales/log/{orderId}")
    @RequirePermission(value = "sales:order:log", message = "您没有权限查看销售订单日志")
    public Result<List<SalesOrderStatusLogVO>> salesLog(@PathVariable String orderId) {
        return Result.success(orderService.listSalesLogs(orderId));
    }

    @GetMapping("/production/page")
    @RequirePermission(value = "production:order:list", message = "您没有权限查看生产订单列表")
    public Result<PageResult<ProductionOrderPageVO>> productionPage(ProductionOrderPageRequest request) {
        return Result.success(toPageResult(orderService.pageProductionOrders(request)));
    }

    @GetMapping("/production/detail/{orderId}")
    @RequirePermission(value = "production:order:detail", message = "您没有权限查看生产订单详情")
    public Result<ProductionOrderDetailVO> productionDetail(@PathVariable String orderId) {
        return Result.success(orderService.getProductionOrderDetail(orderId));
    }

    @PostMapping("/production/create")
    @RequirePermission(value = "production:order:status", message = "您没有权限创建生产订单")
    public Result<String> createProduction(@RequestBody @Valid ProductionOrderSaveRequest request) {
        return Result.success(orderService.createProductionOrder(request));
    }

    @PostMapping("/production/save/{orderId}")
    @RequirePermission(value = "production:order:status", message = "您没有权限编辑生产订单")
    public Result<Void> saveProduction(@PathVariable String orderId, @RequestBody @Valid ProductionOrderSaveRequest request) {
        orderService.saveProductionOrder(orderId, request);
        return Result.success(null);
    }

    @GetMapping("/production/log/{orderId}")
    @RequirePermission(value = "production:order:log", message = "您没有权限查看生产订单日志")
    public Result<List<ProductionOrderStatusLogVO>> productionLog(@PathVariable String orderId) {
        return Result.success(orderService.listProductionLogs(orderId));
    }

    @PostMapping("/production/update/{orderId}")
    @RequirePermission(value = "production:order:status", message = "您没有权限更新生产订单")
    public Result<Void> updateProduction(@PathVariable String orderId, @RequestBody ProductionOrderUpdateRequest request) {
        orderService.updateProductionOrder(orderId, request);
        return Result.success(null);
    }

    @GetMapping("/health")
    @RequirePermission(value = "sales:order:list", message = "您没有权限查看订单模块检查结果")
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
