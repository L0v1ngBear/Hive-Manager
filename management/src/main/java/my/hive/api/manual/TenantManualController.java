package my.hive.api.manual;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.dto.Result;
import my.hive.shared.tenant.RequireTenantFeature;
import my.hive.domain.manual.model.dto.TenantManualSaveRequest;
import my.hive.domain.manual.model.vo.TenantManualVO;
import my.hive.domain.manual.service.TenantManualService;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.domain.tenant.model.enums.TenantFeatureEnum;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manual")
@RequireTenantFeature(TenantFeatureEnum.CODE_MANUAL)
@Validated
public class TenantManualController {

    @Resource
    private TenantManualService tenantManualService;

    @GetMapping("/custom")
    public Result<TenantManualVO> custom() {
        return Result.success(tenantManualService.current());
    }

    @PostMapping("/custom")
    @RequirePermission(value = PermissionCatalogV3.CODE_DOCUMENT_RENAME, message = "您没有权限编辑企业使用手册")
    @CollectLog(module = "manual", action = "save_custom_manual", bizType = "tenant_manual", description = "保存企业自定义使用手册")
    public Result<TenantManualVO> save(@Valid @RequestBody TenantManualSaveRequest request) {
        return Result.success(tenantManualService.save(request));
    }
}
