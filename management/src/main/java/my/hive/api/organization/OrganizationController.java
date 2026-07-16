package my.hive.api.organization;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.shared.dto.Result;
import my.hive.shared.tenant.RequireTenantFeature;
import my.hive.domain.tenant.model.enums.TenantFeatureEnum;
import my.hive.domain.organization.model.dto.OrganizationDepartmentSaveRequest;
import my.hive.domain.organization.model.dto.OrganizationPositionSaveRequest;
import my.hive.domain.organization.model.vo.OrganizationEmployeeVO;
import my.hive.domain.organization.model.vo.OrganizationJoinCodeVO;
import my.hive.domain.organization.model.vo.OrganizationOverviewVO;
import my.hive.domain.organization.model.vo.OrganizationPositionVO;
import my.hive.domain.organization.service.OrganizationService;
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
    @RequirePermission(value = PermissionCatalogV3.CODE_ORGANIZATION_VIEW, message = "您没有权限查看组织架构")
    public Result<OrganizationOverviewVO> overview() {
        return Result.success(organizationService.overview());
    }

    @GetMapping("/department/{departmentId}/employees")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORGANIZATION_VIEW, message = "您没有权限查看部门员工")
    public Result<List<OrganizationEmployeeVO>> employees(@PathVariable Long departmentId) {
        return Result.success(organizationService.employees(departmentId));
    }

    @PostMapping("/join-code")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_CREATE, message = "您没有权限生成组织加入码")
    @CollectLog(module = "organization", action = "create_join_code", bizType = "organization", description = "管理端生成组织加入码", recordResult = false)
    public Result<OrganizationJoinCodeVO> createJoinCode() {
        return Result.success(organizationService.createJoinCode());
    }

    @PostMapping("/department/save")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORGANIZATION_DEPARTMENT_MANAGE, message = "您没有权限维护部门")
    @CollectLog(module = "organization", action = "save_department", bizType = "department", bizNo = "#request.id", description = "管理端保存部门")
    public Result<Long> saveDepartment(@Valid @RequestBody OrganizationDepartmentSaveRequest request) {
        return Result.success(organizationService.save(request));
    }

    @DeleteMapping("/department/{departmentId}")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORGANIZATION_DEPARTMENT_DELETE, message = "您没有权限删除部门")
    @CollectLog(module = "organization", action = "delete_department", bizType = "department", bizNo = "#departmentId", description = "管理端删除部门")
    public Result<Void> deleteDepartment(@PathVariable Long departmentId) {
        organizationService.delete(departmentId);
        return Result.success(null);
    }

    @GetMapping("/department/{departmentId}/positions")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORGANIZATION_VIEW, message = "您没有权限查看职位")
    public Result<List<OrganizationPositionVO>> positions(@PathVariable Long departmentId) {
        return Result.success(organizationService.positions(departmentId));
    }

    @PostMapping("/position/save")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORGANIZATION_POSITION_MANAGE, message = "您没有权限维护职位")
    @CollectLog(module = "organization", action = "save_position", bizType = "position", bizNo = "#request.id", description = "管理端保存职位")
    public Result<Long> savePosition(@Valid @RequestBody OrganizationPositionSaveRequest request) {
        return Result.success(organizationService.savePosition(request));
    }

    @DeleteMapping("/position/{positionId}")
    @RequirePermission(value = PermissionCatalogV3.CODE_ORGANIZATION_POSITION_DELETE, message = "您没有权限删除职位")
    @CollectLog(module = "organization", action = "delete_position", bizType = "position", bizNo = "#positionId", description = "管理端删除职位")
    public Result<Void> deletePosition(@PathVariable Long positionId) {
        organizationService.deletePosition(positionId);
        return Result.success(null);
    }
}
