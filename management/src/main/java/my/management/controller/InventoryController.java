package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import my.hive.common.annotation.RequirePermission;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import my.management.module.inventory.model.dto.InventoryInRequest;
import my.management.module.inventory.model.dto.InventoryOutRequest;
import my.management.module.inventory.model.dto.InventoryPageRequest;
import my.management.module.inventory.model.dto.InventoryWarningSettingUpdateRequest;
import my.management.module.inventory.model.vo.ClothInventoryVO;
import my.management.module.inventory.model.vo.InventoryImageRecognitionVO;
import my.management.module.inventory.model.vo.InventoryImportResultVO;
import my.management.module.inventory.model.vo.InventoryInResultVO;
import my.management.module.inventory.model.vo.InventoryModelSummaryVO;
import my.management.module.inventory.model.vo.InventoryModelOptionVO;
import my.management.module.inventory.model.vo.InventoryRecordVO;
import my.management.module.inventory.model.vo.InventorySummaryVO;
import my.management.module.inventory.model.vo.InventoryTrendVO;
import my.management.module.inventory.model.vo.InventoryWarningSettingVO;
import my.management.module.inventory.model.vo.InventoryWarningVO;
import my.management.module.inventory.service.InventorySettingService;
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
@RequireTenantFeature(TenantFeatureEnum.CODE_INVENTORY)
public class InventoryController {

    @Resource
    private InventoryService inventoryService;

    @Resource
    private InventorySettingService inventorySettingService;

    @GetMapping("/summary")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_WARNING_LIST, message = "您没有权限查看库存概览")
    public Result<InventorySummaryVO> summary() {
        return Result.success(inventoryService.summary());
    }

    @GetMapping("/page")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_WARNING_LIST, message = "您没有权限查看库存列表")
    public Result<PageResult<ClothInventoryVO>> page(InventoryPageRequest request) {
        return Result.success(inventoryService.page(request));
    }

    @GetMapping("/model/page")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_WARNING_LIST, message = "您没有权限查看库存聚合列表")
    public Result<PageResult<InventoryModelSummaryVO>> modelPage(InventoryPageRequest request) {
        return Result.success(inventoryService.modelPage(request));
    }

    @GetMapping("/model/detail")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_WARNING_LIST, message = "您没有权限查看库存明细")
    public Result<List<ClothInventoryVO>> modelDetail(@RequestParam String modelCode,
                                                      @RequestParam(required = false) java.math.BigDecimal spec,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) String timeOrder) {
        return Result.success(inventoryService.modelDetail(modelCode, spec, status, timeOrder));
    }

    @GetMapping("/warning/list")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_WARNING_LIST, message = "您没有权限查看库存预警")
    public Result<List<InventoryWarningVO>> warnings() {
        return Result.success(inventoryService.warnings());
    }

    @GetMapping("/warning/setting")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_WARNING_LIST, message = "您没有权限查看库存预警设置")
    public Result<InventoryWarningSettingVO> warningSetting() {
        return Result.success(inventorySettingService.currentSetting());
    }

    @PostMapping("/warning/setting")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_WARNING_SETTING, message = "您没有权限维护库存预警设置")
    public Result<InventoryWarningSettingVO> updateWarningSetting(@Valid @RequestBody InventoryWarningSettingUpdateRequest request) {
        return Result.success(inventorySettingService.updateCurrentSetting(request));
    }

    @GetMapping("/record/recent")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_RECORD_RECENT, message = "您没有权限查看库存流水")
    public Result<List<InventoryRecordVO>> recentRecords() {
        return Result.success(inventoryService.recentRecords());
    }

    @GetMapping("/trend")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_TREND, message = "您没有权限查看库存趋势")
    public Result<List<InventoryTrendVO>> trend() {
        return Result.success(inventoryService.trend());
    }

    @GetMapping("/model/search")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_WARNING_LIST, message = "您没有权限搜索库存型号")
    public Result<List<InventoryModelOptionVO>> searchModels(@RequestParam(required = false) String keyword) {
        return Result.success(inventoryService.searchModels(keyword));
    }

    @GetMapping("/barCode/search")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_WARNING_LIST, message = "您没有权限搜索库存条码")
    public Result<ClothInventoryVO> searchByBarcode(@RequestParam String barCode) {
        return Result.success(inventoryService.searchByBarcode(barCode));
    }

    @PostMapping("/cloth/in")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_CLOTH_IN, message = "您没有权限执行入库")
    public Result<InventoryInResultVO> in(@Valid @RequestBody InventoryInRequest request) {
        return Result.success(inventoryService.in(request));
    }

    @PostMapping("/cloth/image-recognition")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_CLOTH_IN, message = "您没有权限执行图片识别入库")
    public Result<InventoryImageRecognitionVO> recognizeInboundImage(@RequestParam("file") MultipartFile file) {
        return Result.success(inventoryService.recognizeInboundImage(file));
    }

    @PostMapping("/cloth/out")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_CLOTH_OUT, message = "您没有权限执行出库")
    public Result<Void> out(@Valid @RequestBody InventoryOutRequest request) {
        inventoryService.out(request);
        return Result.success(null);
    }

    @GetMapping("/import-template")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_CLOTH_IN, message = "您没有权限下载库存导入模板")
    public void downloadImportTemplate(HttpServletResponse response) {
        inventoryService.downloadImportTemplate(response);
    }

    @PostMapping("/import")
    @RequirePermission(value = PermissionCodeEnum.CODE_INVENTORY_CLOTH_IN, message = "您没有权限导入库存数据")
    public Result<InventoryImportResultVO> importInventory(@RequestParam("file") MultipartFile file) {
        return Result.success(inventoryService.importInventory(file));
    }
}
