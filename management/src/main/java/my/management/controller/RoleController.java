package my.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.management.common.dto.PageResult;
import my.management.common.dto.Result;
import my.management.module.sys.model.dto.SysRoleAddRequest;
import my.management.module.sys.model.dto.SysRoleUpdateRequest;
import my.management.module.sys.model.entity.SysPermission;
import my.management.module.sys.model.entity.SysRole;
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

@RestController
@RequestMapping("/sys/role")
@Validated
public class RoleController {

    @Resource
    private RoleService roleService;

    @GetMapping("/page")
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
    public Result<Void> create(@Valid @RequestBody SysRoleAddRequest request) {
        roleService.createNewRole(request);
        return Result.success(null);
    }

    @GetMapping("/role/all")
    public Result<List<SysPermission>> all() {
        return Result.success(roleService.selectAllPermission());
    }

    @GetMapping("/{roleId}/permission-ids")
    public Result<Set<Long>> getRolePermissionIds(@PathVariable Long roleId) {
        return Result.success(roleService.getRolePermissionIds(roleId));
    }

    @PostMapping("/role/update")
    public Result<Void> update(@Valid @RequestBody SysRoleUpdateRequest request) {
        roleService.updateRole(request);
        return Result.success(null);
    }
}