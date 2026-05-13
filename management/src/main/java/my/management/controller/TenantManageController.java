package my.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.common.annotation.CollectLog;
import my.hive.common.annotation.RequirePermission;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.module.tenant.model.dto.TenantCreateRequest;
import my.management.module.tenant.model.dto.TenantFieldConfigSaveRequest;
import my.management.module.tenant.model.dto.TenantLicenseUpdateRequest;
import my.management.module.tenant.model.dto.TenantPageRequest;
import my.management.module.tenant.model.vo.TenantDetailVO;
import my.management.module.tenant.model.vo.TenantFieldConfigVO;
import my.management.module.tenant.model.vo.TenantFeatureOptionVO;
import my.management.module.tenant.model.vo.TenantPageVO;
import my.management.module.tenant.service.TenantFieldConfigService;
import my.management.module.tenant.service.TenantLicenseService;
import my.management.module.tenant.service.TenantManageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * TenantManageController 是管理端后端请求入口控制类，负责接收请求并调用对应服务。
 */
@RestController
@RequestMapping("/platform/tenant")
public class TenantManageController {

    @Resource
    private TenantManageService tenantManageService;

    @Resource
    private TenantLicenseService tenantLicenseService;

    @Resource
    private TenantFieldConfigService tenantFieldConfigService;

    @GetMapping("/page")
    @RequirePermission(value = PermissionCodeEnum.CODE_PLATFORM_TENANT_VIEW, message = "您没有权限查看租户列表")
    public Result<PageResult<TenantPageVO>> page(@Valid TenantPageRequest request) {
        Page<TenantPageVO> page = tenantManageService.page(request);
        PageResult<TenantPageVO> result = new PageResult<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setData(page.getRecords());
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @RequirePermission(value = PermissionCodeEnum.CODE_PLATFORM_TENANT_VIEW, message = "您没有权限查看租户详情")
    public Result<TenantDetailVO> detail(@PathVariable Long id) {
        return Result.success(tenantManageService.detail(id));
    }

    @GetMapping("/features/catalog")
    @RequirePermission(value = PermissionCodeEnum.CODE_PLATFORM_TENANT_LICENSE, message = "您没有权限查看租户功能清单")
    public Result<List<TenantFeatureOptionVO>> featureCatalog() {
        return Result.success(tenantLicenseService.featureCatalog());
    }

    @GetMapping("/{tenantCode}/field-config")
    @RequirePermission(value = PermissionCodeEnum.CODE_PLATFORM_TENANT_LICENSE, message = "您没有权限查看租户字段配置")
    public Result<List<TenantFieldConfigVO>> fieldConfig(@PathVariable String tenantCode,
                                                         @RequestParam String moduleCode) {
        return Result.success(tenantFieldConfigService.list(tenantCode, moduleCode));
    }

    @PostMapping("/create")
    @RequirePermission(value = PermissionCodeEnum.CODE_PLATFORM_TENANT_CREATE, message = "您没有权限创建租户")
    @CollectLog(module = "platform_tenant", action = "create", bizType = "tenant", bizNo = "#request.tenantCode", description = "create tenant and initialize defaults")
    public Result<Long> create(@Valid @RequestBody TenantCreateRequest request) {
        return Result.success(tenantManageService.createTenant(request));
    }

    @PostMapping("/license")
    @RequirePermission(value = PermissionCodeEnum.CODE_PLATFORM_TENANT_LICENSE, message = "您没有权限调整租户授权")
    @CollectLog(module = "platform_tenant", action = "license", bizType = "tenant", bizNo = "#request.id", description = "update tenant license")
    public Result<Void> updateLicense(@Valid @RequestBody TenantLicenseUpdateRequest request) {
        tenantManageService.updateLicense(request);
        return Result.success(null);
    }

    @PostMapping("/{tenantCode}/field-config")
    @RequirePermission(value = PermissionCodeEnum.CODE_PLATFORM_TENANT_LICENSE, message = "您没有权限维护租户字段配置")
    @CollectLog(module = "platform_tenant", action = "field_config", bizType = "tenant", bizNo = "#tenantCode", description = "update tenant field customization")
    public Result<List<TenantFieldConfigVO>> saveFieldConfig(@PathVariable String tenantCode,
                                                             @Valid @RequestBody TenantFieldConfigSaveRequest request) {
        return Result.success(tenantFieldConfigService.save(tenantCode, request));
    }
}
