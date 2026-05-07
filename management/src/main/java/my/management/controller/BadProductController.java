package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.common.annotation.RequirePermission;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import my.management.module.badproduct.model.dto.BadProductPageRequest;
import my.management.module.badproduct.model.dto.BadProductProcessRequest;
import my.management.module.badproduct.model.dto.BadProductSaveRequest;
import my.management.module.badproduct.model.vo.BadProductVO;
import my.management.module.badproduct.service.BadProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端次品管理控制器。
 */
@RestController
@RequestMapping("/bad-product")
@RequireTenantFeature(TenantFeatureEnum.CODE_BAD_PRODUCT)
public class BadProductController {

    @Resource
    private BadProductService badProductService;

    @GetMapping("/list")
    @RequirePermission(value = PermissionCodeEnum.CODE_BADPRODUCT_LIST, message = "您没有权限查看次品列表")
    public Result<PageResult<BadProductVO>> list(BadProductPageRequest request) {
        return Result.success(badProductService.page(request));
    }

    @PostMapping("/save")
    @RequirePermission(value = PermissionCodeEnum.CODE_BADPRODUCT_SAVE, message = "您没有权限登记次品")
    public Result<Void> save(@Valid @RequestBody BadProductSaveRequest request) {
        badProductService.save(request);
        return Result.success(null);
    }

    @PostMapping("/process")
    @RequirePermission(value = PermissionCodeEnum.CODE_BADPRODUCT_PROCESS, message = "您没有权限处理次品")
    public Result<Void> process(@Valid @RequestBody BadProductProcessRequest request) {
        badProductService.process(request);
        return Result.success(null);
    }
}
