package my.hive.api.attendance;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.shared.dto.PageResult;
import my.hive.shared.dto.Result;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import my.hive.domain.attendance.model.dto.AttendancePageRequest;
import my.hive.domain.attendance.model.dto.AttendancePunchRequest;
import my.hive.domain.attendance.model.dto.AttendanceRuleSaveRequest;
import my.hive.domain.attendance.model.entity.AttendanceRecord;
import my.hive.domain.attendance.model.vo.AttendanceDepartmentVO;
import my.hive.domain.attendance.model.vo.AttendanceRecordManageVO;
import my.hive.domain.attendance.model.vo.AttendanceRuleVO;
import my.hive.domain.attendance.model.vo.AttendanceSummaryVO;
import my.hive.domain.attendance.service.AttendanceService;
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
public class AttendanceController {

    @Resource
    private AttendanceService attendanceService;

    @GetMapping("/summary")
    @RequirePermission(value = PermissionCatalogV3.CODE_ATTENDANCE_RECORD_LIST, message = "您没有权限查看考勤统计")
    public Result<AttendanceSummaryVO> summary(@RequestParam(required = false) LocalDate date) {
        return Result.success(attendanceService.summary(date));
    }

    @GetMapping("/page")
    @RequirePermission(value = PermissionCatalogV3.CODE_ATTENDANCE_RECORD_LIST, message = "您没有权限查看考勤记录")
    public Result<PageResult<AttendanceRecordManageVO>> page(AttendancePageRequest request) {
        return Result.success(attendanceService.page(request));
    }

    @GetMapping("/departments")
    @RequirePermission(value = PermissionCatalogV3.CODE_ATTENDANCE_RECORD_LIST, message = "您没有权限查看考勤部门")
    public Result<List<AttendanceDepartmentVO>> departments() {
        return Result.success(attendanceService.departments());
    }

    @GetMapping("/rule")
    @RequirePermission(value = PermissionCatalogV3.CODE_ATTENDANCE_RULE_LIST, message = "您没有权限查看考勤规则")
    public Result<AttendanceRuleVO> rule() {
        return Result.success(attendanceService.getRule());
    }

    @PostMapping("/punch")
    @RequirePermission(value = PermissionCatalogV3.CODE_ATTENDANCE_PUNCH, message = "您没有考勤打卡权限")
    @CollectLog(module = "attendance", action = "punch", bizType = "attendance_record", description = "mini-program attendance punch")
    public Result<Void> punch(@Valid @RequestBody AttendancePunchRequest request) {
        attendanceService.punch(request);
        return Result.success(null);
    }

    @GetMapping("/records/me")
    @RequirePermission(value = PermissionCatalogV3.CODE_ATTENDANCE_RECORD_LIST, message = "您没有查看考勤记录的权限")
    public Result<List<AttendanceRecord>> recordsForCurrentUser() {
        return Result.success(attendanceService.recordsForCurrentUser());
    }

    @PostMapping("/rule/save")
    @RequirePermission(value = PermissionCatalogV3.CODE_ATTENDANCE_RULE_UPDATE, message = "您没有权限保存考勤规则")
    @CollectLog(module = "attendance", action = "save_rule", bizType = "tenant_attendance_rule", description = "save tenant attendance rule")
    public Result<Void> saveRule(@Valid @RequestBody AttendanceRuleSaveRequest request) {
        attendanceService.saveRule(request);
        return Result.success(null);
    }

    @GetMapping("/export-excel")
    @RequirePermission(value = PermissionCatalogV3.CODE_ATTENDANCE_EXPORT, message = "您没有权限导出考勤记录")
    public void exportExcel(AttendancePageRequest request, HttpServletResponse response) {
        attendanceService.exportExcel(request, response);
    }
}
