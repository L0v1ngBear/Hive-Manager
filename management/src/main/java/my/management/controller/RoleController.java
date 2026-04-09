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
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        List<SysPermission> permissions = roleService.selectAllPermission();
        return Result.success(permissions);
    }

    @PostMapping("/role/update")
    public Result<Void> update(@Valid @RequestBody SysRoleUpdateRequest request) {
        roleService.updateRole(request);
        return Result.success(null);
    }
}
