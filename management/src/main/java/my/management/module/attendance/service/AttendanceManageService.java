package my.management.module.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.dto.PageResult;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.privacy.PrivacyProtectionUtil;
import my.hive.shared.redis.HiveRedisKeyBuilder;
import my.management.common.enums.BinaryFlagEnum;
import my.management.common.enums.CommonStatusEnum;
import my.management.common.utils.ExcelUtil;
import my.management.module.attendance.mapper.AttendanceManageMapper;
import my.management.module.attendance.mapper.TenantAttendanceLocationManageMapper;
import my.management.module.attendance.mapper.TenantAttendanceRuleManageMapper;
import my.management.module.attendance.model.dto.AttendanceLocationSaveRequest;
import my.management.module.attendance.model.dto.AttendancePageRequest;
import my.management.module.attendance.model.dto.AttendanceRuleSaveRequest;
import my.management.module.attendance.model.entity.TenantAttendanceLocation;
import my.management.module.attendance.model.entity.TenantAttendanceRule;
import my.management.module.attendance.model.enums.AttendancePunchStatusEnum;
import my.management.module.attendance.model.vo.AttendanceDepartmentVO;
import my.management.module.attendance.model.vo.AttendanceLocationVO;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端考勤服务，负责考勤统计、列表查询和筛选项组装。
 */
@Service
public class AttendanceManageService {

    private static final DateTimeFormatter PUNCH_DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final List<Integer> DEFAULT_WORK_DAYS = List.of(1, 2, 3, 4, 5);
    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 200L;
    private static final long EXPORT_PAGE_SIZE = 10000L;

    @Resource
    private AttendanceManageMapper attendanceManageMapper;

    @Resource
    private TenantAttendanceRuleManageMapper tenantAttendanceRuleManageMapper;

    @Resource
    private TenantAttendanceLocationManageMapper tenantAttendanceLocationManageMapper;

    @Resource
    private ExcelUtil excelUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private PrivacyProtectionUtil privacyProtectionUtil;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

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
        normalizeRequest(request, MAX_PAGE_SIZE);
        return pageNormalized(request);
    }

    private PageResult<AttendanceRecordManageVO> pageNormalized(AttendancePageRequest request) {
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
        List<AttendanceLocationSaveRequest> locations = normalizeLocations(request);
        AttendanceLocationSaveRequest primaryLocation = locations.isEmpty() ? null : locations.get(0);

        TenantAttendanceRule rule = tenantAttendanceRuleManageMapper.selectByTenantCode(tenantCode);
        if (rule == null) {
            rule = new TenantAttendanceRule();
            rule.setTenantCode(tenantCode);
            rule.setTenantName(tenantCode);
            rule.setStatus(CommonStatusEnum.ENABLED.getCode());
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
        rule.setEnableGps(BinaryFlagEnum.codeOf(request.getEnableGps()));
        rule.setLatitude(primaryLocation == null ? request.getLatitude() : primaryLocation.getLatitude());
        rule.setLongitude(primaryLocation == null ? request.getLongitude() : primaryLocation.getLongitude());
        rule.setRadius(primaryLocation == null ? request.getRadius() : primaryLocation.getRadius());
        rule.setAddress(primaryLocation == null ? clean(request.getAddress()) : clean(primaryLocation.getAddress()));
        rule.setEnableWifi(BinaryFlagEnum.codeOf(request.getEnableWifi()));
        rule.setWifiSsid(clean(request.getWifiSsid()));

        if (rule.getId() == null) {
            tenantAttendanceRuleManageMapper.insert(rule);
        } else {
            tenantAttendanceRuleManageMapper.updateById(rule);
        }

        saveLocations(tenantCode, BinaryFlagEnum.isYes(rule.getEnableGps()), locations);
        clearAttendanceRuleCache(tenantCode);
    }

    private void saveLocations(String tenantCode, boolean enableGps, List<AttendanceLocationSaveRequest> locations) {
        List<TenantAttendanceLocation> existing = tenantAttendanceLocationManageMapper.selectList(new LambdaQueryWrapper<TenantAttendanceLocation>()
                .eq(TenantAttendanceLocation::getStatus, CommonStatusEnum.ENABLED.getCode()));
        Map<Long, TenantAttendanceLocation> existingById = existing.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(TenantAttendanceLocation::getId, Function.identity(), (left, right) -> left));
        Set<Long> requestedIds = enableGps ? locations.stream()
                .map(AttendanceLocationSaveRequest::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()) : Set.of();
        for (TenantAttendanceLocation item : existing) {
            if (!requestedIds.contains(item.getId())) {
                item.setStatus(CommonStatusEnum.DISABLED.getCode());
                tenantAttendanceLocationManageMapper.updateById(item);
            }
        }
        if (!enableGps) {
            return;
        }
        int sortOrder = 1;
        for (AttendanceLocationSaveRequest request : locations) {
            TenantAttendanceLocation location = request.getId() == null ? null : existingById.get(request.getId());
            if (location == null) {
                location = new TenantAttendanceLocation();
            }
            location.setTenantCode(tenantCode);
            location.setLocationName(clean(request.getLocationName()) == null ? "打卡点" + sortOrder : clean(request.getLocationName()));
            location.setLatitude(request.getLatitude());
            location.setLongitude(request.getLongitude());
            location.setAddress(clean(request.getAddress()));
            location.setRadius(safeRadius(request.getRadius()));
            location.setStatus(CommonStatusEnum.ENABLED.getCode());
            location.setSortOrder(sortOrder++);
            if (location.getId() == null) {
                tenantAttendanceLocationManageMapper.insert(location);
            } else {
                tenantAttendanceLocationManageMapper.updateById(location);
            }
        }
    }

    private void clearAttendanceRuleCache(String tenantCode) {
        try {
            stringRedisTemplate.delete(redisKeyBuilder.cache("tenant", "attendance-rule", tenantCode));
        } catch (Exception ignored) {
        }
    }

    public void exportExcel(AttendancePageRequest request, HttpServletResponse response) {
        normalizeRequest(request, EXPORT_PAGE_SIZE);
        request.setPageNum(DEFAULT_PAGE_NUM);
        request.setPageSize(EXPORT_PAGE_SIZE);

        List<String> headers = List.of("员工姓名", "工号", "手机号", "部门", "上班打卡", "下班打卡", "状态", "更新时间");
        List<List<String>> rows = pageNormalized(request).getData().stream()
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

        excelUtil.writeRowsToResponse(response,
                "考勤记录",
                headers,
                rows,
                "考勤记录_" + (request.getDate() == null ? LocalDate.now() : request.getDate()) + ".xlsx");
    }

    private void normalizeRequest(AttendancePageRequest request, long maxPageSize) {
        if (request.getPageNum() == null || request.getPageNum() <= 0) {
            request.setPageNum(DEFAULT_PAGE_NUM);
        }
        if (request.getPageSize() == null || request.getPageSize() <= 0) {
            request.setPageSize(DEFAULT_PAGE_SIZE);
        } else {
            request.setPageSize(Math.min(request.getPageSize(), maxPageSize));
        }
        request.setKeyword(clean(request.getKeyword()));
        request.setKeywordPhoneHash(privacyProtectionUtil.mayBePhoneKeyword(request.getKeyword())
                ? privacyProtectionUtil.hashPhone(request.getKeyword())
                : null);
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
        // 没有真实经纬度时不能默认开启 GPS，避免小程序打卡被空坐标误拦截。
        vo.setEnableGps(Boolean.FALSE);
        vo.setRadius(200D);
        vo.setEnableWifi(Boolean.FALSE);
        vo.setLocations(List.of());
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
        vo.setEnableGps(rule.getEnableGps() == null || BinaryFlagEnum.isYes(rule.getEnableGps()));
        vo.setLatitude(rule.getLatitude());
        vo.setLongitude(rule.getLongitude());
        vo.setRadius(rule.getRadius());
        vo.setAddress(rule.getAddress());
        List<AttendanceLocationVO> locations = tenantAttendanceLocationManageMapper.selectActiveByTenantCode(rule.getTenantCode())
                .stream()
                .map(this::toLocationVO)
                .toList();
        if (locations.isEmpty() && isValidLatitude(rule.getLatitude()) && isValidLongitude(rule.getLongitude())) {
            AttendanceLocationVO fallback = new AttendanceLocationVO();
            fallback.setLocationName(clean(rule.getAddress()) == null ? "公司打卡点" : clean(rule.getAddress()));
            fallback.setLatitude(rule.getLatitude());
            fallback.setLongitude(rule.getLongitude());
            fallback.setRadius(safeRadius(rule.getRadius()));
            fallback.setAddress(rule.getAddress());
            locations = List.of(fallback);
        }
        vo.setLocations(locations);
        vo.setEnableWifi(BinaryFlagEnum.isYes(rule.getEnableWifi()));
        vo.setWifiSsid(rule.getWifiSsid());
        return vo;
    }

    private AttendanceLocationVO toLocationVO(TenantAttendanceLocation location) {
        AttendanceLocationVO vo = new AttendanceLocationVO();
        vo.setId(location.getId());
        vo.setLocationName(location.getLocationName());
        vo.setLatitude(location.getLatitude());
        vo.setLongitude(location.getLongitude());
        vo.setAddress(location.getAddress());
        vo.setRadius(safeRadius(location.getRadius()));
        return vo;
    }

    private void validateRule(AttendanceRuleSaveRequest request) {
        if (Boolean.TRUE.equals(request.getEnableGps())) {
            if (normalizeLocations(request).isEmpty()) {
                throw new BusinessException("启用GPS围栏时，请至少配置一个有效打卡地点");
            }
        }
        if (Boolean.TRUE.equals(request.getEnableWifi()) && clean(request.getWifiSsid()) == null) {
            throw new BusinessException("启用WiFi验证时，请填写WiFi名称");
        }
        if (request.getWorkDays() == null || request.getWorkDays().isEmpty()) {
            throw new BusinessException("请至少选择一个工作日");
        }
    }

    private List<AttendanceLocationSaveRequest> normalizeLocations(AttendanceRuleSaveRequest request) {
        List<AttendanceLocationSaveRequest> source = request.getLocations();
        if ((source == null || source.isEmpty())
                && (request.getLatitude() != null || request.getLongitude() != null || request.getRadius() != null || clean(request.getAddress()) != null)) {
            AttendanceLocationSaveRequest legacy = new AttendanceLocationSaveRequest();
            legacy.setLocationName(clean(request.getAddress()) == null ? "公司打卡点" : clean(request.getAddress()));
            legacy.setLatitude(request.getLatitude());
            legacy.setLongitude(request.getLongitude());
            legacy.setRadius(request.getRadius());
            legacy.setAddress(request.getAddress());
            source = List.of(legacy);
        }
        if (source == null) {
            return List.of();
        }
        return source.stream()
                .filter(Objects::nonNull)
                .peek(this::validateLocation)
                .toList();
    }

    private void validateLocation(AttendanceLocationSaveRequest location) {
        if (!isValidLatitude(location.getLatitude()) || !isValidLongitude(location.getLongitude())) {
            throw new BusinessException("打卡点经纬度不合法，请检查后再保存");
        }
        Double radius = location.getRadius();
        if (radius == null || radius <= 0D || radius > 5000D) {
            throw new BusinessException("打卡点允许半径需大于0且不超过5000米");
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

    private Double safeRadius(Double value) {
        return value == null || value <= 0D ? 300D : value;
    }

    private boolean isValidLatitude(Double value) {
        return value != null && value >= -90D && value <= 90D;
    }

    private boolean isValidLongitude(Double value) {
        return value != null && value >= -180D && value <= 180D;
    }

    private void fillStatus(AttendanceRecordManageVO row) {
        row.setPhone(privacyProtectionUtil.maskPhone(row.getPhone()));
        AttendancePunchStatusEnum.ResolvedStatus status = AttendancePunchStatusEnum.resolveManageStatus(
                row.getSignInStatus(),
                row.getSignOutStatus()
        );
        row.setStatus(status.viewKey());
        row.setStatusText(status.label());
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
