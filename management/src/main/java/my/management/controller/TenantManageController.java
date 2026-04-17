package my.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.management.common.annotation.RequirePermission;
import my.management.common.dto.PageResult;
import my.management.common.dto.Result;
import my.management.module.tenant.model.dto.TenantCreateRequest;
import my.management.module.tenant.model.dto.TenantPageRequest;
import my.management.module.tenant.model.vo.TenantDetailVO;
import my.management.module.tenant.model.vo.TenantPageVO;
import my.management.module.tenant.service.TenantManageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * TenantManageController 是管理端后端请求入口控制类，负责接收请求并调用对应服务。
 */
@RestController
@RequestMapping("/platform/tenant")
public class TenantManageController {

    @Resource
    private TenantManageService tenantManageService;

    @GetMapping("/page")
    @RequirePermission(value = "platform:tenant:view", message = "您没有权限查看租户信息")
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
    @RequirePermission(value = "platform:tenant:view", message = "您没有权限查看租户详情")
    public Result<TenantDetailVO> detail(@PathVariable Long id) {
        return Result.success(tenantManageService.detail(id));
    }

    @PostMapping("/create")
    @RequirePermission(value = "platform:tenant:view", message = "您没有权限创建租户")
    public Result<Long> create(@Valid @RequestBody TenantCreateRequest request) {
        return Result.success(tenantManageService.createTenant(request));
    }
}
