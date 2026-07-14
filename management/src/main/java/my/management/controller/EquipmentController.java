package my.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.dto.PageResult;
import my.hive.shared.dto.Result;
import my.hive.shared.exception.BusinessException;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.equipment.model.dto.EquipmentInspectionSubmitRequest;
import my.management.module.equipment.model.dto.EquipmentPageRequest;
import my.management.module.equipment.model.dto.EquipmentRecordPageRequest;
import my.management.module.equipment.model.dto.EquipmentSaveRequest;
import my.management.module.equipment.model.vo.EquipmentDeviceVO;
import my.management.module.equipment.model.vo.EquipmentInspectionRecordVO;
import my.management.module.equipment.service.EquipmentService;
import my.hive.shared.permission.PermissionCatalogV3;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/equipment")
@RequireTenantFeature(TenantFeatureEnum.CODE_EQUIPMENT)
@Validated
public class EquipmentController {

    @Resource
    private EquipmentService equipmentService;

    @GetMapping("/page")
    @RequirePermission(value = PermissionCatalogV3.CODE_EQUIPMENT_LIST, message = "您没有权限查看设备巡检")
    public Result<PageResult<EquipmentDeviceVO>> page(@Valid EquipmentPageRequest request) {
        return Result.success(toPageResult(equipmentService.page(request)));
    }

    @GetMapping("/detail/{id}")
    @RequirePermission(value = PermissionCatalogV3.CODE_EQUIPMENT_DETAIL, message = "您没有权限查看设备详情")
    public Result<EquipmentDeviceVO> detail(@PathVariable Long id) {
        return Result.success(equipmentService.detail(id));
    }

    @GetMapping("/scan-target")
    @RequirePermission(value = PermissionCatalogV3.CODE_EQUIPMENT_INSPECTION_SUBMIT, message = "您没有权限执行设备巡检")
    public Result<EquipmentDeviceVO> scanTarget(String equipmentCode) {
        return Result.success(equipmentService.scanTarget(equipmentCode));
    }

    @PostMapping("/save")
    @CollectLog(module = "equipment", action = "save", bizType = "equipment", bizNo = "#request.equipmentCode", description = "保存设备档案")
    public Result<EquipmentDeviceVO> save(@Valid @RequestBody EquipmentSaveRequest request) {
        String permissionCode = request.getId() == null
                ? PermissionCatalogV3.CODE_EQUIPMENT_CREATE
                : PermissionCatalogV3.CODE_EQUIPMENT_UPDATE;
        if (!TenantPermissionContext.hasPermission(permissionCode)) {
            throw new BusinessException(403, "您没有权限维护设备");
        }
        return Result.success(equipmentService.save(request));
    }

    @PostMapping("/disable/{id}")
    @RequirePermission(value = PermissionCatalogV3.CODE_EQUIPMENT_DISABLE, message = "您没有权限停用设备")
    @CollectLog(module = "equipment", action = "disable", bizType = "equipment", bizNo = "#id", description = "停用设备档案")
    public Result<Void> disable(@PathVariable Long id) {
        equipmentService.disable(id);
        return Result.success(null);
    }

    @GetMapping("/inspection/records")
    @RequirePermission(value = PermissionCatalogV3.CODE_EQUIPMENT_INSPECTION_LIST, message = "您没有权限查看巡检记录")
    public Result<PageResult<EquipmentInspectionRecordVO>> recordPage(@Valid EquipmentRecordPageRequest request) {
        return Result.success(toPageResult(equipmentService.recordPage(request)));
    }

    @PostMapping("/inspection/submit")
    @RequirePermission(value = PermissionCatalogV3.CODE_EQUIPMENT_INSPECTION_SUBMIT, message = "您没有权限提交巡检记录")
    @CollectLog(module = "equipment", action = "inspection_submit", bizType = "equipment", bizNo = "#request.equipmentCode", description = "提交设备巡检记录")
    public Result<EquipmentInspectionRecordVO> submit(@Valid @RequestBody EquipmentInspectionSubmitRequest request) {
        return Result.success(equipmentService.submitInspection(request));
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
