package my.management.module.attendance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.dto.PageResult;
import my.hive.common.exception.BusinessException;
import my.management.common.utils.ExcelUtil;
import my.management.module.attendance.mapper.AttendanceManageMapper;
import my.management.module.attendance.mapper.TenantAttendanceRuleManageMapper;
import my.management.module.attendance.model.dto.AttendancePageRequest;
import my.management.module.attendance.model.dto.AttendanceRuleSaveRequest;
import my.management.module.attendance.model.entity.TenantAttendanceRule;
import my.management.module.attendance.model.vo.AttendanceDepartmentVO;
import my.management.module.attendance.model.vo.AttendanceRecordManageVO;
import my.management.module.attendance.model.vo.AttendanceRuleVO;
import my.management.module.attendance.model.vo.AttendanceSummaryVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端考勤服务，负责考勤统计、列表查询和筛选项组装。
 */
@Service
public class AttendanceManageService {

    private static final DateTimeFormatter PUNCH_DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String ATTENDANCE_RULE_CACHE_KEY = "companyAttendanceRule";
    private static final List<Integer> DEFAULT_WORK_DAYS = List.of(1, 2, 3, 4, 5);

    @Resource
    private AttendanceManageMapper attendanceManageMapper;

    @Resource
    private TenantAttendanceRuleManageMapper tenantAttendanceRuleManageMapper;

    @Resource
    private ExcelUtil excelUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public AttendanceSummaryVO summary(LocalDate date) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        String dayPrefix = dayPrefix(date);

        AttendanceSummaryVO vo = new AttendanceSummaryVO();
        vo.setTotalEmployeeCount(nvl(attendanceManageMapper.countActiveEmployees(tenantCode)));
        vo.setActualCount(nvl(attendanceManageMapper.countActual(tenantCode, dayPrefix)));
        vo.setLateCount(nvl(attendanceManageMapper.countLate(tenantCode, dayPrefix)));
        vo.setEarlyCount(nvl(attendanceManageMapper.countEarly(tenantCode, dayPrefix)));
        vo.setMissingCount(nvl(attendanceManageMapper.countMissing(tenantCode, dayPrefix)));
        vo.setAttendanceRate(vo.getTotalEmployeeCount() == 0
                ? 0D
                : Math.round(vo.getActualCount() * 10000.0 / vo.getTotalEmployeeCount()) / 100.0);
        return vo;
    }

    public PageResult<AttendanceRecordManageVO> page(AttendancePageRequest request) {
        normalizeRequest(request);
        String tenantCode = TenantPermissionContext.getTenantCode();
        Page<AttendanceRecordManageVO> page = attendanceManageMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()),
                tenantCode,
                dayPrefix(request.getDate()),
                request
        );
        page.getRecords().forEach(this::fillStatus);

        PageResult<AttendanceRecordManageVO> result = new PageResult<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setData(page.getRecords());
        return result;
    }

    public List<AttendanceDepartmentVO> departments() {
        return attendanceManageMapper.selectDepartments(TenantPermissionContext.getTenantCode());
    }

    public AttendanceRuleVO getRule() {
        TenantAttendanceRule rule = tenantAttendanceRuleManageMapper.selectByTenantCode(TenantPermissionContext.getTenantCode());
        return rule == null ? defaultRule() : toRuleVO(rule);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveRule(AttendanceRuleSaveRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        validateRule(request);

        TenantAttendanceRule rule = tenantAttendanceRuleManageMapper.selectByTenantCode(tenantCode);
        if (rule == null) {
            rule = new TenantAttendanceRule();
            rule.setTenantCode(tenantCode);
            rule.setTenantName(tenantCode);
            rule.setStatus(1);
        }

        rule.setWorkStartTime(parseTime(request.getWorkStartTime(), "第一次上班时间"));
        rule.setWorkEndTime(parseTime(request.getWorkEndTime(), "第一次下班时间"));
        rule.setOffWorkStartTime(parseTime(request.getOffWorkStartTime(), "第二次上班时间"));
        rule.setOffWorkEndTime(parseTime(request.getOffWorkEndTime(), "第二次下班时间"));
        rule.setOverTimeStartTime(parseOptionalTime(request.getOverTimeStartTime(), "加班开始时间"));
        rule.setOverTimeEndTime(parseOptionalTime(request.getOverTimeEndTime(), "加班结束时间"));
        rule.setLateToleranceMinutes(nonNegative(request.getLateToleranceMinutes()));
        rule.setEarlyToleranceMinutes(nonNegative(request.getEarlyToleranceMinutes()));
        rule.setWorkDays(toWorkDays(request.getWorkDays()));
        rule.setEnableGps(Boolean.TRUE.equals(request.getEnableGps()) ? 1 : 0);
        rule.setLatitude(request.getLatitude());
        rule.setLongitude(request.getLongitude());
        rule.setRadius(request.getRadius());
        rule.setAddress(clean(request.getAddress()));
        rule.setEnableWifi(Boolean.TRUE.equals(request.getEnableWifi()) ? 1 : 0);
        rule.setWifiSsid(clean(request.getWifiSsid()));

        if (rule.getId() == null) {
            tenantAttendanceRuleManageMapper.insert(rule);
        } else {
            tenantAttendanceRuleManageMapper.updateById(rule);
        }

        // 小程序后端打卡规则会缓存到 Redis，管理端保存后必须清理，避免继续使用旧规则。
        stringRedisTemplate.opsForHash().delete(ATTENDANCE_RULE_CACHE_KEY, tenantCode);
    }

    public void exportExcel(AttendancePageRequest request, HttpServletResponse response) {
        normalizeRequest(request);
        request.setPageNum(1L);
        request.setPageSize(10000L);

        List<String> headers = List.of("员工姓名", "工号", "手机号", "部门", "上班打卡", "下班打卡", "状态", "更新时间");
        List<List<String>> rows = page(request).getData().stream()
                .map(item -> List.of(
                        excelUtil.stringify(item.getEmployeeName()),
                        excelUtil.stringify(item.getEmpNo()),
                        excelUtil.stringify(item.getPhone()),
                        excelUtil.stringify(item.getDepartmentName()),
                        excelUtil.stringify(item.getSignInTime()),
                        excelUtil.stringify(item.getSignOutTime()),
                        excelUtil.stringify(item.getStatusText()),
                        excelUtil.stringify(item.getUpdateTime() == null ? item.getCreateTime() : item.getUpdateTime())
                ))
                .toList();

        excelUtil.writeToResponse(response,
                excelUtil.createWorkbook("考勤记录", headers, rows),
                "考勤记录_" + (request.getDate() == null ? LocalDate.now() : request.getDate()) + ".xlsx");
    }

    private void normalizeRequest(AttendancePageRequest request) {
        if (request.getPageNum() == null || request.getPageNum() <= 0) {
            request.setPageNum(1L);
        }
        if (request.getPageSize() == null || request.getPageSize() <= 0) {
            request.setPageSize(10L);
        }
        request.setKeyword(clean(request.getKeyword()));
        request.setDepartmentName(clean(request.getDepartmentName()));
        request.setStatus(clean(request.getStatus()));
    }

    private AttendanceRuleVO defaultRule() {
        AttendanceRuleVO vo = new AttendanceRuleVO();
        vo.setWorkStartTime("08:00");
        vo.setWorkEndTime("12:00");
        vo.setOffWorkStartTime("13:00");
        vo.setOffWorkEndTime("17:00");
        vo.setOverTimeStartTime("18:00");
        vo.setOverTimeEndTime("21:00");
        vo.setLateToleranceMinutes(0);
        vo.setEarlyToleranceMinutes(0);
        vo.setWorkDays(DEFAULT_WORK_DAYS);
        vo.setEnableGps(Boolean.TRUE);
        vo.setRadius(200D);
        vo.setEnableWifi(Boolean.FALSE);
        return vo;
    }

    private AttendanceRuleVO toRuleVO(TenantAttendanceRule rule) {
        AttendanceRuleVO vo = new AttendanceRuleVO();
        vo.setWorkStartTime(formatTime(rule.getWorkStartTime()));
        vo.setWorkEndTime(formatTime(rule.getWorkEndTime()));
        vo.setOffWorkStartTime(formatTime(rule.getOffWorkStartTime()));
        vo.setOffWorkEndTime(formatTime(rule.getOffWorkEndTime()));
        vo.setOverTimeStartTime(formatTime(rule.getOverTimeStartTime()));
        vo.setOverTimeEndTime(formatTime(rule.getOverTimeEndTime()));
        vo.setLateToleranceMinutes(nonNegative(rule.getLateToleranceMinutes()));
        vo.setEarlyToleranceMinutes(nonNegative(rule.getEarlyToleranceMinutes()));
        vo.setWorkDays(parseWorkDays(rule.getWorkDays()));
        vo.setEnableGps(rule.getEnableGps() == null || rule.getEnableGps() == 1);
        vo.setLatitude(rule.getLatitude());
        vo.setLongitude(rule.getLongitude());
        vo.setRadius(rule.getRadius());
        vo.setAddress(rule.getAddress());
        vo.setEnableWifi(rule.getEnableWifi() != null && rule.getEnableWifi() == 1);
        vo.setWifiSsid(rule.getWifiSsid());
        return vo;
    }

    private void validateRule(AttendanceRuleSaveRequest request) {
        if (Boolean.TRUE.equals(request.getEnableGps())) {
            if (request.getLatitude() == null || request.getLongitude() == null || request.getRadius() == null || request.getRadius() <= 0) {
                throw new BusinessException("启用GPS围栏时，请填写经纬度和有效半径");
            }
        }
        if (Boolean.TRUE.equals(request.getEnableWifi()) && clean(request.getWifiSsid()) == null) {
            throw new BusinessException("启用WiFi验证时，请填写WiFi名称");
        }
        if (request.getWorkDays() == null || request.getWorkDays().isEmpty()) {
            throw new BusinessException("请至少选择一个工作日");
        }
    }

    private LocalTime parseTime(String value, String label) {
        try {
            return LocalTime.parse(value.length() == 5 ? value + ":00" : value);
        } catch (Exception e) {
            throw new BusinessException(label + "格式不正确");
        }
    }

    private LocalTime parseOptionalTime(String value, String label) {
        return clean(value) == null ? null : parseTime(value, label);
    }

    private String formatTime(LocalTime value) {
        return value == null ? "" : value.toString().substring(0, 5);
    }

    private String toWorkDays(List<Integer> days) {
        List<Integer> safeDays = days == null || days.isEmpty() ? DEFAULT_WORK_DAYS : days;
        return safeDays.stream()
                .filter(Objects::nonNull)
                .filter(day -> day >= 1 && day <= 7)
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<Integer> parseWorkDays(String value) {
        if (clean(value) == null) {
            return DEFAULT_WORK_DAYS;
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(Integer::valueOf)
                .filter(day -> day >= 1 && day <= 7)
                .distinct()
                .sorted()
                .toList();
    }

    private Integer nonNegative(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private void fillStatus(AttendanceRecordManageVO row) {
        if (row.getSignInStatus() != null) {
            if (row.getSignInStatus() == 1) {
                row.setStatus("late");
                row.setStatusText("迟到");
                return;
            }
            if (row.getSignInStatus() == 3) {
                row.setStatus("missing");
                row.setStatusText("缺勤");
                return;
            }
            if (row.getSignInStatus() == 5) {
                row.setStatus("leave");
                row.setStatusText("请假");
                return;
            }
            if (row.getSignInStatus() == 6) {
                row.setStatus("missing");
                row.setStatusText("缺卡");
                return;
            }
        }
        if (row.getSignOutStatus() != null) {
            if (row.getSignOutStatus() == 2) {
                row.setStatus("early");
                row.setStatusText("早退");
                return;
            }
            if (row.getSignOutStatus() == 3 || row.getSignOutStatus() == 6) {
                row.setStatus("missing");
                row.setStatusText("缺卡");
                return;
            }
            if (row.getSignOutStatus() == 4) {
                row.setStatus("overtime");
                row.setStatusText("加班");
                return;
            }
            if (row.getSignOutStatus() == 5) {
                row.setStatus("leave");
                row.setStatusText("请假");
                return;
            }
        }
        row.setStatus("normal");
        row.setStatusText("正常");
    }

    private String dayPrefix(LocalDate date) {
        return (date == null ? LocalDate.now() : date).format(PUNCH_DAY_FORMATTER);
    }

    private String clean(String value) {
        if (value == null || value.isBlank() || "undefined".equals(value) || "null".equals(value) || "all".equals(value)) {
            return null;
        }
        return value.trim();
    }

    private Long nvl(Long value) {
        return value == null ? 0L : value;
    }
}
