package my.hive.api.tenant;

import jakarta.annotation.Resource;
import my.hive.shared.dto.Result;
import my.hive.domain.tenant.model.vo.TenantFieldConfigVO;
import my.hive.domain.tenant.service.TenantFieldConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * TenantFieldConfigController exposes current-tenant field customization to business pages.
 */
@RestController
@RequestMapping("/tenant/field-config")
public class TenantFieldConfigController {

    @Resource
    private TenantFieldConfigService tenantFieldConfigService;

    @GetMapping
    public Result<List<TenantFieldConfigVO>> list(@RequestParam String moduleCode) {
        return Result.success(tenantFieldConfigService.listForCurrentTenant(moduleCode));
    }
}
