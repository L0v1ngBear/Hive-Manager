package my.management.module.attendance.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.attendance.model.entity.TenantAttendanceRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 管理端考勤规则数据访问层。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface TenantAttendanceRuleManageMapper extends BaseMapper<TenantAttendanceRule> {

    @Select("""
            SELECT id, tenant_code, tenant_name, status, latitude, longitude, address, radius,
                   work_start_time, work_end_time, off_work_start_time, off_work_end_time,
                   over_time_start_time, over_time_end_time, late_tolerance_minutes,
                   early_tolerance_minutes, work_days, enable_gps, enable_wifi, wifi_ssid
            FROM tenant_attendance_rule
            WHERE tenant_code = #{tenantCode}
            LIMIT 1
            """)
    TenantAttendanceRule selectByTenantCode(@Param("tenantCode") String tenantCode);
}
