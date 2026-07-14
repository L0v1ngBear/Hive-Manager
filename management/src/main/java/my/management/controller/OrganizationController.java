package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import my.hive.shared.dto.Result;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import my.management.module.organization.model.dto.OrganizationDepartmentSaveRequest;
import my.management.module.organization.model.vo.OrganizationEmployeeVO;
import my.management.module.organization.model.vo.OrganizationJoinCodeVO;
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
@RequireTenantFeature(TenantFeatureEnum.CODE_EMPLOYEE)
public class OrganizationController {

    @Resource
    private OrganizationService organizationService;

    @GetMapping("/overview")
    @RequirePermission(value = PermissionCodeEnum.CODE_EMPLOYEE_LIST, message = "您没有权限查看组织架构")
    public Result<OrganizationOverviewVO> overview() {
        return Result.success(organizationService.overview());
    }

    @GetMapping("/department/{departmentId}/employees")
    @RequirePermission(value = PermissionCodeEnum.CODE_EMPLOYEE_LIST, message = "您没有权限查看部门员工")
    public Result<List<OrganizationEmployeeVO>> employees(@PathVariable Long departmentId) {
        return Result.success(organizationService.employees(departmentId));
    }

    @PostMapping("/join-code")
    @RequirePermission(value = PermissionCodeEnum.CODE_EMPLOYEE_CREATE, message = "您没有权限生成组织加入码")
    @CollectLog(module = "organization", action = "create_join_code", bizType = "organization", description = "管理端生成组织加入码", recordResult = false)
    public Result<OrganizationJoinCodeVO> createJoinCode() {
        return Result.success(organizationService.createJoinCode());
    }

    @PostMapping("/department/save")
    @RequirePermission(value = PermissionCodeEnum.CODE_EMPLOYEE_UPDATE, message = "您没有权限维护组织架构")
    @CollectLog(module = "organization", action = "save_department", bizType = "department", bizNo = "#request.id", description = "管理端保存部门")
    public Result<Long> saveDepartment(@Valid @RequestBody OrganizationDepartmentSaveRequest request) {
        return Result.success(organizationService.save(request));
    }

    @DeleteMapping("/department/{departmentId}")
    @RequirePermission(value = PermissionCodeEnum.CODE_EMPLOYEE_DELETE, message = "您没有权限删除部门")
    @CollectLog(module = "organization", action = "delete_department", bizType = "department", bizNo = "#departmentId", description = "管理端删除部门")
    public Result<Void> deleteDepartment(@PathVariable Long departmentId) {
        organizationService.delete(departmentId);
        return Result.success(null);
    }
}
