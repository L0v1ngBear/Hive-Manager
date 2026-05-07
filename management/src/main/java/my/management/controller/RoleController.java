package my.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.common.annotation.RequirePermission;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import my.management.module.sys.model.dto.SysRoleAddRequest;
import my.management.module.sys.model.dto.SysRoleUpdateRequest;
import my.management.module.sys.model.entity.SysRole;
import my.management.module.sys.model.vo.SysPermissionTreeVO;
import my.management.module.sys.service.RoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
/**
 * RoleController 是管理端后端请求入口控制类，负责接收请求并调用对应服务。
 */
@RestController
@RequestMapping("/sys/role")
@RequireTenantFeature(TenantFeatureEnum.CODE_ROLE)
@Validated
public class RoleController {

    @Resource
    private RoleService roleService;

    @GetMapping("/page")
    @RequirePermission(value = PermissionCodeEnum.CODE_ROLE_LIST, message = "您没有权限查看角色列表")
    public Result<PageResult<SysRole>> page(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) String keyword) {
        Page<SysRole> rolePage = roleService.selectPage(page, size, keyword);
        PageResult<SysRole> pageResult = new PageResult<>();
        BeanUtils.copyProperties(rolePage, pageResult);
        pageResult.setData(rolePage.getRecords());
        return Result.success(pageResult);
    }

    @PostMapping("/create")
    @RequirePermission(value = PermissionCodeEnum.CODE_ROLE_CREATE, message = "您没有权限创建角色")
    public Result<Void> create(@Valid @RequestBody SysRoleAddRequest request) {
        roleService.createNewRole(request);
        return Result.success(null);
    }

    @GetMapping("/role/all")
    @RequirePermission(value = PermissionCodeEnum.CODE_ROLE_PERMISSION_LIST, message = "您没有权限查看权限列表")
    public Result<List<SysPermissionTreeVO>> all() {
        return Result.success(roleService.selectAllPermissionTree());
    }

    @GetMapping("/{roleId}/permission-ids")
    @RequirePermission(value = PermissionCodeEnum.CODE_ROLE_PERMISSION_LIST, message = "您没有权限查看角色权限")
    public Result<Set<Long>> getRolePermissionIds(@PathVariable Long roleId) {
        return Result.success(roleService.getRolePermissionIds(roleId));
    }

    @PostMapping("/role/update")
    @RequirePermission(value = PermissionCodeEnum.CODE_ROLE_UPDATE, message = "您没有权限配置角色权限")
    public Result<Void> update(@Valid @RequestBody SysRoleUpdateRequest request) {
        roleService.updateRole(request);
        return Result.success(null);
    }
}
