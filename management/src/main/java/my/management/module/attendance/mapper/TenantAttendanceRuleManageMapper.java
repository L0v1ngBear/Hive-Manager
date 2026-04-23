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

    @Select("SELECT * FROM tenant_attendance_rule WHERE tenant_code = #{tenantCode} LIMIT 1")
    TenantAttendanceRule selectByTenantCode(@Param("tenantCode") String tenantCode);
}
