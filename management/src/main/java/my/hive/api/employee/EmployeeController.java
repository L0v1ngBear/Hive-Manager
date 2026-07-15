package my.hive.api.employee;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.shared.dto.PageResult;
import my.hive.shared.dto.Result;
import my.hive.shared.tenant.RequireTenantFeature;
import my.hive.domain.tenant.model.enums.TenantFeatureEnum;
import my.hive.shared.dto.ImportResultVO;
import my.hive.domain.employee.model.dto.EmployeeBatchUpdateRequest;
import my.hive.domain.employee.model.dto.EmployeeCreateRequest;
import my.hive.domain.employee.model.dto.EmployeePageQuery;
import my.hive.domain.employee.model.dto.EmployeePermissionOverrideRequest;
import my.hive.domain.employee.model.dto.EmployeeStatusChangeRequest;
import my.hive.domain.employee.model.dto.EmployeeUpdateRequest;
import my.hive.domain.employee.model.vo.EmployeeDetailVO;
import my.hive.domain.employee.model.vo.EmployeeFormOptionsVO;
import my.hive.domain.employee.model.vo.EmployeeLeaderOptionVO;
import my.hive.domain.employee.model.vo.EmployeePageVO;
import my.hive.domain.employee.model.vo.EmployeePermissionProfileVO;
import my.hive.domain.employee.model.vo.EmployeeStatsVO;
import my.hive.domain.employee.service.EmployeePermissionProfileService;
import my.hive.domain.employee.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
/**
 * EmployeeController 是管理端后端请求入口控制类，负责接收请求并调用对应服务。
 */
@RestController
@RequestMapping("/emp/employee")
@RequireTenantFeature(TenantFeatureEnum.CODE_EMPLOYEE)
@Validated
public class EmployeeController {

    @Resource
    private EmployeeService employeeService;

    @Resource
    private EmployeePermissionProfileService employeePermissionProfileService;

    @GetMapping("/page")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_LIST, message = "您没有权限查看员工列表")
    public Result<PageResult<EmployeePageVO>> page(@Valid EmployeePageQuery query) {
        Page<EmployeePageVO> employeePage = employeeService.page(query);
        PageResult<EmployeePageVO> pageResult = new PageResult<>();
        BeanUtils.copyProperties(employeePage, pageResult);
        pageResult.setData(employeePage.getRecords());
        return Result.success(pageResult);
    }

    @GetMapping("/stats")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_LIST, message = "您没有权限查看员工统计")
    public Result<EmployeeStatsVO> stats() {
        return Result.success(employeeService.stats());
    }

    @GetMapping("/{id}")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_DETAIL, message = "您没有权限查看员工详情")
    public Result<EmployeeDetailVO> detail(@PathVariable Long id) {
        return Result.success(employeeService.detail(id));
    }

    @PostMapping("/create")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_CREATE, message = "您没有权限新增员工")
    @CollectLog(module = "employee", action = "create", bizType = "employee", description = "管理端新增员工")
    public Result<Long> create(@Valid @RequestBody EmployeeCreateRequest request) {
        return Result.success(employeeService.create(request));
    }

    @PostMapping("/update")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_UPDATE, message = "您没有权限编辑员工")
    @CollectLog(module = "employee", action = "update", bizType = "employee", bizNo = "#request.id", description = "管理端编辑员工")
    public Result<Void> update(@Valid @RequestBody EmployeeUpdateRequest request) {
        employeeService.update(request);
        return Result.success(null);
    }

    @GetMapping("/{id}/permission-profile")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_PERMISSION_MANAGE, message = "您没有权限配置员工单独权限")
    public Result<EmployeePermissionProfileVO> permissionProfile(@PathVariable Long id) {
        return Result.success(employeePermissionProfileService.profile(id));
    }

    @PutMapping("/{id}/permission-overrides")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_PERMISSION_MANAGE, message = "您没有权限配置员工单独权限")
    @CollectLog(module = "employee", action = "permission_overrides", bizType = "employee", bizNo = "#id", description = "管理端配置员工单独权限")
    public Result<Long> updatePermissionOverrides(@PathVariable Long id,
                                                  @Valid @RequestBody EmployeePermissionOverrideRequest request) {
        return Result.success(employeePermissionProfileService.updateOverrides(id, request));
    }

    @PostMapping("/change-status")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_STATUS, message = "您没有权限调整员工状态")
    @CollectLog(module = "employee", action = "change_status", bizType = "employee", bizNo = "#request.id", description = "管理端调整员工状态")
    public Result<Void> changeStatus(@Valid @RequestBody EmployeeStatusChangeRequest request) {
        employeeService.changeStatus(request);
        return Result.success(null);
    }

    @PostMapping("/batch-update")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_UPDATE, message = "您没有权限批量编辑员工")
    @CollectLog(module = "employee", action = "batch_update", bizType = "employee", description = "管理端批量更新员工")
    public Result<Void> batchUpdate(@Valid @RequestBody EmployeeBatchUpdateRequest request) {
        employeeService.batchUpdate(request);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_DELETE, message = "您没有权限删除员工")
    @CollectLog(module = "employee", action = "delete", bizType = "employee", bizNo = "#id", description = "管理端删除员工")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return Result.success(null);
    }

    @GetMapping("/leader/search")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_LIST, message = "您没有权限查询负责人")
    public Result<List<EmployeeLeaderOptionVO>> searchLeaders(@RequestParam(required = false) String keyword,
                                                              @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(employeeService.searchLeaders(keyword, limit));
    }

    @GetMapping("/init-form-options")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_LIST, message = "您没有权限查看员工表单选项")
    public Result<EmployeeFormOptionsVO> initFormOptions() {
        return Result.success(employeeService.initFormOptions());
    }

    @PostMapping("/export")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_EXPORT, message = "您没有权限导出员工数据")
    @CollectLog(module = "employee", action = "export", bizType = "employee", description = "管理端导出员工数据")
    public Result<List<EmployeePageVO>> export(@RequestBody(required = false) EmployeePageQuery query) {
        return Result.success(employeeService.export(query == null ? new EmployeePageQuery() : query));
    }

    @GetMapping("/export-excel")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_EXPORT, message = "您没有权限导出员工数据")
    @CollectLog(module = "employee", action = "export_excel", bizType = "employee", description = "管理端导出员工 Excel")
    public void exportExcel(@Valid EmployeePageQuery query, HttpServletResponse response) {
        employeeService.exportExcel(query, response);
    }

    @GetMapping("/import-template")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_IMPORT, message = "您没有权限下载员工导入模板")
    public void downloadImportTemplate(HttpServletResponse response) {
        employeeService.downloadImportTemplate(response);
    }

    @PostMapping("/import")
    @RequirePermission(value = PermissionCatalogV3.CODE_EMPLOYEE_IMPORT, message = "您没有权限导入员工数据")
    @CollectLog(module = "employee", action = "import", bizType = "employee", description = "管理端导入员工数据")
    public Result<ImportResultVO> importEmployees(@RequestParam("file") MultipartFile file) {
        return Result.success(employeeService.importEmployees(file));
    }

}
