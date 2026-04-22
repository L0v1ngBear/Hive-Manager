package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.common.annotation.RequirePermission;
import my.hive.common.dto.Result;
import my.management.module.organization.model.dto.OrganizationDepartmentSaveRequest;
import my.management.module.organization.model.vo.OrganizationEmployeeVO;
import my.management.module.organization.model.vo.OrganizationOverviewVO;
import my.management.module.organization.service.OrganizationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端组织架构控制器，提供部门层级维护和部门员工查看接口。
 */
@RestController
@RequestMapping("/organization")
public class OrganizationController {

    @Resource
    private OrganizationService organizationService;

    @GetMapping("/overview")
    @RequirePermission(value = "employee:list", message = "您没有权限查看组织架构")
    public Result<OrganizationOverviewVO> overview() {
        return Result.success(organizationService.overview());
    }

    @GetMapping("/department/{departmentId}/employees")
    @RequirePermission(value = "employee:list", message = "您没有权限查看部门员工")
    public Result<List<OrganizationEmployeeVO>> employees(@PathVariable Long departmentId) {
        return Result.success(organizationService.employees(departmentId));
    }

    @PostMapping("/department/save")
    @RequirePermission(value = "employee:update", message = "您没有权限维护组织架构")
    public Result<Long> saveDepartment(@Valid @RequestBody OrganizationDepartmentSaveRequest request) {
        return Result.success(organizationService.save(request));
    }

    @DeleteMapping("/department/{departmentId}")
    @RequirePermission(value = "employee:delete", message = "您没有权限删除部门")
    public Result<Void> deleteDepartment(@PathVariable Long departmentId) {
        organizationService.delete(departmentId);
        return Result.success(null);
    }
}
