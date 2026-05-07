package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import my.hive.common.annotation.RequirePermission;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.common.vo.ImportResultVO;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.inventory.model.dto.InventoryInRequest;
import my.management.module.inventory.model.dto.InventoryOutRequest;
import my.management.module.inventory.model.dto.InventoryPageRequest;
import my.management.module.inventory.model.vo.ClothInventoryVO;
import my.management.module.inventory.model.vo.InventoryModelOptionVO;
import my.management.module.inventory.model.vo.InventoryRecordVO;
import my.management.module.inventory.model.vo.InventorySummaryVO;
import my.management.module.inventory.model.vo.InventoryTrendVO;
import my.management.module.inventory.model.vo.InventoryWarningVO;
import my.management.module.inventory.service.InventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 管理端库存控制器，给网页端库存页面提供真实库存接口。
 */
@RestController
@RequestMapping("/inventory")
@RequireTenantFeature("module.inventory")
public class InventoryController {

    @Resource
    private InventoryService inventoryService;

    @GetMapping("/summary")
    @RequirePermission(value = "inventory:warning:list", message = "您没有权限查看库存概览")
    public Result<InventorySummaryVO> summary() {
        return Result.success(inventoryService.summary());
    }

    @GetMapping("/page")
    @RequirePermission(value = "inventory:warning:list", message = "您没有权限查看库存列表")
    public Result<PageResult<ClothInventoryVO>> page(InventoryPageRequest request) {
        return Result.success(inventoryService.page(request));
    }

    @GetMapping("/warning/list")
    @RequirePermission(value = "inventory:warning:list", message = "您没有权限查看库存预警")
    public Result<List<InventoryWarningVO>> warnings() {
        return Result.success(inventoryService.warnings());
    }

    @GetMapping("/record/recent")
    @RequirePermission(value = "inventory:record:recent", message = "您没有权限查看库存流水")
    public Result<List<InventoryRecordVO>> recentRecords() {
        return Result.success(inventoryService.recentRecords());
    }

    @GetMapping("/trend")
    @RequirePermission(value = "inventory:trend", message = "您没有权限查看库存趋势")
    public Result<List<InventoryTrendVO>> trend() {
        return Result.success(inventoryService.trend());
    }

    @GetMapping("/model/search")
    @RequirePermission(value = "inventory:warning:list", message = "您没有权限搜索库存型号")
    public Result<List<InventoryModelOptionVO>> searchModels(@RequestParam(required = false) String keyword) {
        return Result.success(inventoryService.searchModels(keyword));
    }

    @GetMapping("/barCode/search")
    @RequirePermission(value = "inventory:warning:list", message = "您没有权限搜索库存条码")
    public Result<ClothInventoryVO> searchByBarcode(@RequestParam String barCode) {
        return Result.success(inventoryService.searchByBarcode(barCode));
    }

    @PostMapping("/cloth/in")
    @RequirePermission(value = "inventory:cloth:in", message = "您没有权限执行入库")
    public Result<Void> in(@Valid @RequestBody InventoryInRequest request) {
        inventoryService.in(request);
        return Result.success(null);
    }

    @PostMapping("/cloth/out")
    @RequirePermission(value = "inventory:cloth:out", message = "您没有权限执行出库")
    public Result<Void> out(@Valid @RequestBody InventoryOutRequest request) {
        inventoryService.out(request);
        return Result.success(null);
    }

    @GetMapping("/import-template")
    @RequirePermission(value = "inventory:cloth:in", message = "您没有权限下载库存导入模板")
    public void downloadImportTemplate(HttpServletResponse response) {
        inventoryService.downloadImportTemplate(response);
    }

    @PostMapping("/import")
    @RequirePermission(value = "inventory:cloth:in", message = "您没有权限导入库存数据")
    public Result<ImportResultVO> importInventory(@RequestParam("file") MultipartFile file) {
        return Result.success(inventoryService.importInventory(file));
    }
}
