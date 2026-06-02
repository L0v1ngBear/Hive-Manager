package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.common.dto.Result;
import my.management.module.tenant.model.dto.TenantLicenseUpdateRequest;
import my.management.module.tenant.model.dto.TenantOwnerAccountRequest;
import my.management.module.tenant.model.dto.TenantProfileUpdateRequest;
import my.management.module.tenant.model.dto.TenantStatusUpdateRequest;
import my.management.module.tenant.model.vo.TenantFeatureOptionVO;
import my.management.module.tenant.model.vo.TenantManageVO;
import my.management.module.tenant.service.TenantManageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/platform/tenants")
public class TenantManageController {

    @Resource
    private TenantManageService tenantManageService;

    @GetMapping
    public Result<List<TenantManageVO>> list() {
        return Result.success(tenantManageService.list());
    }

    @GetMapping("/features")
    public Result<List<TenantFeatureOptionVO>> featureCatalog() {
        return Result.success(tenantManageService.featureCatalog());
    }

    @PutMapping("/{id}/profile")
    public Result<TenantManageVO> updateProfile(@PathVariable Long id,
                                                @Valid @RequestBody TenantProfileUpdateRequest request) {
        return Result.success(tenantManageService.updateProfile(id, request));
    }

    @PostMapping("/{id}/logo")
    public Result<TenantManageVO> uploadLogo(@PathVariable Long id,
                                             @RequestParam("file") MultipartFile file) {
        return Result.success(tenantManageService.uploadLogo(id, file));
    }

    @PutMapping("/{id}/license")
    public Result<TenantManageVO> updateLicense(@PathVariable Long id,
                                                @Valid @RequestBody TenantLicenseUpdateRequest request) {
        return Result.success(tenantManageService.updateLicense(id, request));
    }

    @PutMapping("/{id}/status")
    public Result<TenantManageVO> updateStatus(@PathVariable Long id,
                                               @Valid @RequestBody TenantStatusUpdateRequest request) {
        return Result.success(tenantManageService.updateStatus(id, request));
    }

    @PutMapping("/{id}/owner-account")
    public Result<TenantManageVO> reassignOwnerAccount(@PathVariable Long id,
                                                       @Valid @RequestBody TenantOwnerAccountRequest request) {
        return Result.success(tenantManageService.reassignOwnerAccount(id, request));
    }
}
