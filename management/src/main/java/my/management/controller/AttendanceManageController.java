package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import my.hive.shared.dto.PageResult;
import my.hive.shared.dto.Result;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
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
@RequireTenantFeature(TenantFeatureEnum.CODE_ATTENDANCE)
public class AttendanceManageController {

    @Resource
    private AttendanceManageService attendanceManageService;

    @GetMapping("/summary")
    @RequirePermission(value = PermissionCodeEnum.CODE_ATTENDANCE_RECORD_LIST, message = "您没有权限查看考勤统计")
    public Result<AttendanceSummaryVO> summary(@RequestParam(required = false) LocalDate date) {
        return Result.success(attendanceManageService.summary(date));
    }

    @GetMapping("/page")
    @RequirePermission(value = PermissionCodeEnum.CODE_ATTENDANCE_RECORD_LIST, message = "您没有权限查看考勤记录")
    public Result<PageResult<AttendanceRecordManageVO>> page(AttendancePageRequest request) {
        return Result.success(attendanceManageService.page(request));
    }

    @GetMapping("/departments")
    @RequirePermission(value = PermissionCodeEnum.CODE_ATTENDANCE_RECORD_LIST, message = "您没有权限查看考勤部门")
    public Result<List<AttendanceDepartmentVO>> departments() {
        return Result.success(attendanceManageService.departments());
    }

    @GetMapping("/rule")
    @RequirePermission(value = PermissionCodeEnum.CODE_ATTENDANCE_RULE_LIST, message = "您没有权限查看考勤规则")
    public Result<AttendanceRuleVO> rule() {
        return Result.success(attendanceManageService.getRule());
    }

    @PostMapping("/rule/save")
    @RequirePermission(value = PermissionCodeEnum.CODE_ATTENDANCE_RULE_UPDATE, message = "您没有权限保存考勤规则")
    @CollectLog(module = "attendance", action = "save_rule", bizType = "tenant_attendance_rule", description = "save tenant attendance rule")
    public Result<Void> saveRule(@Valid @RequestBody AttendanceRuleSaveRequest request) {
        attendanceManageService.saveRule(request);
        return Result.success(null);
    }

    @GetMapping("/export-excel")
    @RequirePermission(value = PermissionCodeEnum.CODE_ATTENDANCE_EXPORT, message = "您没有权限导出考勤记录")
    public void exportExcel(AttendancePageRequest request, HttpServletResponse response) {
        attendanceManageService.exportExcel(request, response);
    }
}
