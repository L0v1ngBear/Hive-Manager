package my.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.management.common.dto.PageResult;
import my.management.common.dto.Result;
import my.management.module.sys.model.entity.SysRole;
import my.management.module.sys.service.RoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@RestController("/sys/role")
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
    public Result<SysRole> create(@RequestBody SysRole role) {
}
