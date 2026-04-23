package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import my.hive.common.annotation.RequirePermission;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.module.attendance.model.dto.AttendancePageRequest;
import my.management.module.attendance.model.dto.AttendanceRuleSaveRequest;
import my.management.module.attendance.model.vo.AttendanceDepartmentVO;
import my.management.module.attendance.model.vo.AttendanceRecordManageVO;
import my.management.module.attendance.model.vo.AttendanceRuleVO;
import my.management.module.attendance.model.vo.AttendanceSummaryVO;
import my.management.module.attendance.service.AttendanceManageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 管理端考勤管理控制器。
 */
@RestController
@RequestMapping("/attendance")
public class AttendanceManageController {

    @Resource
    private AttendanceManageService attendanceManageService;

    @GetMapping("/summary")
    @RequirePermission(value = "attendance:record:list", message = "您没有权限查看考勤统计")
    public Result<AttendanceSummaryVO> summary(@RequestParam(required = false) LocalDate date) {
        return Result.success(attendanceManageService.summary(date));
    }

    @GetMapping("/page")
    @RequirePermission(value = "attendance:record:list", message = "您没有权限查看考勤记录")
    public Result<PageResult<AttendanceRecordManageVO>> page(AttendancePageRequest request) {
        return Result.success(attendanceManageService.page(request));
    }

    @GetMapping("/departments")
    @RequirePermission(value = "attendance:record:list", message = "您没有权限查看考勤部门")
    public Result<List<AttendanceDepartmentVO>> departments() {
        return Result.success(attendanceManageService.departments());
    }

    @GetMapping("/rule")
    @RequirePermission(value = "attendance:record:list", message = "您没有权限查看考勤规则")
    public Result<AttendanceRuleVO> rule() {
        return Result.success(attendanceManageService.getRule());
    }

    @PostMapping("/rule/save")
    @RequirePermission(value = "attendance:record:list", message = "您没有权限保存考勤规则")
    public Result<Void> saveRule(@Valid @RequestBody AttendanceRuleSaveRequest request) {
        attendanceManageService.saveRule(request);
        return Result.success(null);
    }

    @GetMapping("/export-excel")
    @RequirePermission(value = "attendance:record:list", message = "您没有权限导出考勤记录")
    public void exportExcel(AttendancePageRequest request, HttpServletResponse response) {
        attendanceManageService.exportExcel(request, response);
    }
}
