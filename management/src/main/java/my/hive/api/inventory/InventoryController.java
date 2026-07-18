package my.hive.api.inventory;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import my.hive.domain.inventory.model.dto.InventoryInRequest;
import my.hive.domain.inventory.model.dto.InventoryOutRequest;
import my.hive.domain.inventory.model.dto.InventoryPageRequest;
import my.hive.domain.inventory.model.dto.InventoryWarningSettingUpdateRequest;
import my.hive.domain.inventory.model.vo.ClothInventoryDetailVO;
import my.hive.domain.inventory.model.vo.ClothInventoryVO;
import my.hive.domain.inventory.model.vo.InventoryImageRecognitionVO;
import my.hive.domain.inventory.model.vo.InventoryImportResultVO;
import my.hive.domain.inventory.model.vo.InventoryInResultVO;
import my.hive.domain.inventory.model.vo.InventoryModelOptionVO;
import my.hive.domain.inventory.model.vo.InventoryModelSummaryVO;
import my.hive.domain.inventory.model.vo.InventoryRecordVO;
import my.hive.domain.inventory.model.vo.InventorySummaryVO;
import my.hive.domain.inventory.model.vo.InventoryTrendVO;
import my.hive.domain.inventory.model.vo.InventoryWarningSettingVO;
import my.hive.domain.inventory.model.vo.InventoryWarningVO;
import my.hive.domain.inventory.service.InventoryService;
import my.hive.domain.inventory.service.InventorySettingService;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.dto.PageResult;
import my.hive.shared.dto.Result;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.shared.tenant.RequireTenantFeature;
import my.hive.domain.tenant.model.enums.TenantFeatureEnum;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 统一库存控制器，为管理端和小程序提供库存领域接口。
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
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_LIST, message = "当前账号没有库存查看权限")
    public Result<InventorySummaryVO> summary() {
        return Result.success(inventoryService.summary());
    }

    @GetMapping("/page")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_LIST, message = "当前账号没有库存查看权限")
    public Result<PageResult<ClothInventoryVO>> page(InventoryPageRequest request) {
        return Result.success(inventoryService.page(request));
    }

    @GetMapping("/model/page")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_LIST, message = "当前账号没有库存查看权限")
    public Result<PageResult<InventoryModelSummaryVO>> modelPage(InventoryPageRequest request) {
        return Result.success(inventoryService.modelPage(request));
    }

    @GetMapping("/model/detail")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_DETAIL, message = "当前账号没有库存详情查看权限")
    public Result<List<ClothInventoryVO>> modelDetail(@RequestParam String modelCode,
                                                      @RequestParam(required = false) java.math.BigDecimal spec,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) String timeOrder) {
        return Result.success(inventoryService.modelDetail(modelCode, spec, status, timeOrder));
    }

    @GetMapping("/cloth/detail")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_DETAIL, message = "当前账号没有布料库存详情查看权限")
    public Result<ClothInventoryDetailVO> clothDetail(@RequestParam(required = false) Long id,
                                                      @RequestParam(required = false) String barcode) {
        return Result.success(inventoryService.clothDetail(id, barcode));
    }

    @GetMapping("/warning/list")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_WARNING_LIST, message = "当前账号没有库存预警查看权限")
    public Result<List<InventoryWarningVO>> warnings() {
        return Result.success(inventoryService.warnings());
    }

    @GetMapping("/warning/setting")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_WARNING_LIST, message = "当前账号没有库存预警设置查看权限")
    public Result<InventoryWarningSettingVO> warningSetting() {
        return Result.success(inventorySettingService.currentSetting());
    }

    @PostMapping("/warning/setting")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_WARNING_SETTING, message = "当前账号没有库存预警设置修改权限")
    public Result<InventoryWarningSettingVO> updateWarningSetting(@Valid @RequestBody InventoryWarningSettingUpdateRequest request) {
        return Result.success(inventorySettingService.updateCurrentSetting(request));
    }

    @GetMapping("/record/recent")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_RECORD_LIST, message = "当前账号没有库存记录查看权限")
    public Result<List<InventoryRecordVO>> recentRecords() {
        return Result.success(inventoryService.recentRecords());
    }

    @GetMapping("/trend")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_TREND, message = "当前账号没有库存趋势查看权限")
    public Result<List<InventoryTrendVO>> trend() {
        return Result.success(inventoryService.trend());
    }

    @GetMapping("/model/search")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_MODEL_SEARCH, message = "当前账号没有库存型号搜索权限")
    public Result<List<InventoryModelOptionVO>> searchModels(@RequestParam(required = false) String keyword) {
        return Result.success(inventoryService.searchModels(keyword));
    }

    @GetMapping("/barCode/search")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_BARCODE_SEARCH, message = "当前账号没有库存条码搜索权限")
    public Result<ClothInventoryVO> searchByBarcode(@RequestParam String barCode) {
        return Result.success(inventoryService.searchByBarcode(barCode));
    }

    @PostMapping("/cloth/in")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_CLOTH_IN, message = "当前账号没有入库权限")
    public Result<InventoryInResultVO> in(@Valid @RequestBody InventoryInRequest request) {
        return Result.success(inventoryService.in(request));
    }

    @PostMapping("/cloth/image-recognition")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_CLOTH_IN, message = "当前账号没有图片识别入库权限")
    public Result<InventoryImageRecognitionVO> recognizeInboundImage(@RequestParam("file") MultipartFile file) {
        return Result.success(inventoryService.recognizeInboundImage(file));
    }

    @PostMapping("/cloth/out")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_CLOTH_OUT, message = "当前账号没有出库权限")
    public Result<Void> out(@Valid @RequestBody InventoryOutRequest request) {
        inventoryService.out(request);
        return Result.success(null);
    }

    @GetMapping("/import-template")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_IMPORT, message = "当前账号没有库存导入模板下载权限")
    public void downloadImportTemplate(HttpServletResponse response) {
        inventoryService.downloadImportTemplate(response);
    }

    @PostMapping("/import")
    @RequirePermission(value = PermissionCatalogV3.CODE_INVENTORY_IMPORT, message = "当前账号没有库存导入权限")
    public Result<InventoryImportResultVO> importInventory(@RequestParam("file") MultipartFile file) {
        return Result.success(inventoryService.importInventory(file));
    }
}
