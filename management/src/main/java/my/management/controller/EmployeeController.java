package my.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.management.common.annotation.RequirePermission;
import my.management.common.dto.PageResult;
import my.management.common.dto.Result;
import my.management.module.employee.model.dto.EmployeeBatchUpdateRequest;
import my.management.module.employee.model.dto.EmployeeCreateRequest;
import my.management.module.employee.model.dto.EmployeePageQuery;
import my.management.module.employee.model.dto.EmployeeStatusChangeRequest;
import my.management.module.employee.model.dto.EmployeeUpdateRequest;
import my.management.module.employee.model.vo.EmployeeDetailVO;
import my.management.module.employee.model.vo.EmployeeFormOptionsVO;
import my.management.module.employee.model.vo.EmployeeLeaderOptionVO;
import my.management.module.employee.model.vo.EmployeePageVO;
import my.management.module.employee.model.vo.EmployeeStatsVO;
import my.management.module.employee.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/emp/employee")
@Validated
public class EmployeeController {

    @Resource
    private EmployeeService employeeService;

    @GetMapping("/page")
    @RequirePermission(value = "employee:list", message = "您没有权限查看员工列表")
    public Result<PageResult<EmployeePageVO>> page(@Valid EmployeePageQuery query) {
        Page<EmployeePageVO> employeePage = employeeService.page(query);
        PageResult<EmployeePageVO> pageResult = new PageResult<>();
        BeanUtils.copyProperties(employeePage, pageResult);
        pageResult.setData(employeePage.getRecords());
        return Result.success(pageResult);
    }

    @GetMapping("/stats")
    @RequirePermission(value = "employee:list", message = "您没有权限查看员工统计")
    public Result<EmployeeStatsVO> stats() {
        return Result.success(employeeService.stats());
    }

    @GetMapping("/{id}")
    @RequirePermission(value = "employee:detail", message = "您没有权限查看员工详情")
    public Result<EmployeeDetailVO> detail(@PathVariable Long id) {
        return Result.success(employeeService.detail(id));
    }

    @PostMapping("/create")
    @RequirePermission(value = "employee:create", message = "您没有权限新增员工")
    public Result<Long> create(@Valid @RequestBody EmployeeCreateRequest request) {
        return Result.success(employeeService.create(request));
    }

    @PostMapping("/update")
    @RequirePermission(value = "employee:update", message = "您没有权限编辑员工")
    public Result<Void> update(@Valid @RequestBody EmployeeUpdateRequest request) {
        employeeService.update(request);
        return Result.success(null);
    }

    @PostMapping("/change-status")
    @RequirePermission(value = "employee:status", message = "您没有权限调整员工状态")
    public Result<Void> changeStatus(@Valid @RequestBody EmployeeStatusChangeRequest request) {
        employeeService.changeStatus(request);
        return Result.success(null);
    }

    @PostMapping("/batch-update")
    @RequirePermission(value = "employee:update", message = "您没有权限批量编辑员工")
    public Result<Void> batchUpdate(@Valid @RequestBody EmployeeBatchUpdateRequest request) {
        employeeService.batchUpdate(request);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "employee:delete", message = "您没有权限删除员工")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return Result.success(null);
    }

    @GetMapping("/leader/search")
    @RequirePermission(value = "employee:list", message = "您没有权限查询负责人")
    public Result<List<EmployeeLeaderOptionVO>> searchLeaders(@RequestParam(required = false) String keyword,
                                                              @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(employeeService.searchLeaders(keyword, limit));
    }

    @GetMapping("/init-form-options")
    @RequirePermission(value = "employee:list", message = "您没有权限查看员工表单选项")
    public Result<EmployeeFormOptionsVO> initFormOptions() {
        return Result.success(employeeService.initFormOptions());
    }

    @PostMapping("/export")
    @RequirePermission(value = "employee:export", message = "您没有权限导出员工数据")
    public Result<List<EmployeePageVO>> export(@RequestBody(required = false) EmployeePageQuery query) {
        return Result.success(employeeService.export(query == null ? new EmployeePageQuery() : query));
    }
}
