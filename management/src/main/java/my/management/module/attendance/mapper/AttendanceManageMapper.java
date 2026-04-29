package my.management.module.attendance.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import my.management.module.attendance.model.dto.AttendancePageRequest;
import my.management.module.attendance.model.vo.AttendanceDepartmentVO;
import my.management.module.attendance.model.vo.AttendanceRecordManageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 管理端考勤数据访问层。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface AttendanceManageMapper {

    @Select("SELECT COUNT(1) FROM user u WHERE u.tenant_code = #{tenantCode} AND (u.status IS NULL OR u.status = 1)")
    Long countActiveEmployees(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT COUNT(DISTINCT a.user_id) FROM attendance_record a ",
            "WHERE a.tenant_code = #{tenantCode} ",
            "AND a.punch_id LIKE CONCAT(#{dayPrefix}, '%') ",
            "AND a.sign_in_status IS NOT NULL"
    })
    Long countActual(@Param("tenantCode") String tenantCode, @Param("dayPrefix") String dayPrefix);

    @Select({
            "SELECT COUNT(1) FROM attendance_record a ",
            "WHERE a.tenant_code = #{tenantCode} ",
            "AND a.punch_id LIKE CONCAT(#{dayPrefix}, '%') ",
            "AND a.sign_in_status = 1"
    })
    Long countLate(@Param("tenantCode") String tenantCode, @Param("dayPrefix") String dayPrefix);

    @Select({
            "SELECT COUNT(1) FROM attendance_record a ",
            "WHERE a.tenant_code = #{tenantCode} ",
            "AND a.punch_id LIKE CONCAT(#{dayPrefix}, '%') ",
            "AND a.sign_out_status = 2"
    })
    Long countEarly(@Param("tenantCode") String tenantCode, @Param("dayPrefix") String dayPrefix);

    @Select({
            "SELECT COUNT(1) FROM attendance_record a ",
            "WHERE a.tenant_code = #{tenantCode} ",
            "AND a.punch_id LIKE CONCAT(#{dayPrefix}, '%') ",
            "AND (a.sign_in_status IN (3, 6) OR a.sign_out_status IN (3, 6))"
    })
    Long countMissing(@Param("tenantCode") String tenantCode, @Param("dayPrefix") String dayPrefix);

    @Select({
            "<script>",
            "SELECT a.id, a.punch_id AS punchId, a.user_id AS userId, ",
            "u.name AS employeeName, ext.emp_no AS empNo, COALESCE(u.phone_mask, u.phone) AS phone, u.department_name AS departmentName, ",
            "a.sign_in_time AS signInTime, a.sign_in_status AS signInStatus, ",
            "a.sign_out_time AS signOutTime, a.sign_out_status AS signOutStatus, ",
            "a.create_time AS createTime, a.update_time AS updateTime ",
            "FROM attendance_record a ",
            "LEFT JOIN user u ON u.id = a.user_id AND u.tenant_code = a.tenant_code ",
            "LEFT JOIN emp_employee_ext ext ON ext.user_id = u.id AND ext.tenant_code = u.tenant_code AND ext.is_deleted = 0 ",
            "WHERE a.tenant_code = #{tenantCode} ",
            "<if test='dayPrefix != null and dayPrefix != \"\"'>",
            "AND a.punch_id LIKE CONCAT(#{dayPrefix}, '%') ",
            "</if>",
            "<if test='query.keyword != null and query.keyword != \"\"'>",
            "AND (u.name LIKE CONCAT('%', #{query.keyword}, '%') ",
            "OR COALESCE(u.phone_mask, u.phone) LIKE CONCAT('%', #{query.keyword}, '%') ",
            "OR ext.emp_no LIKE CONCAT('%', #{query.keyword}, '%') ",
            "<if test='query.keywordPhoneHash != null and query.keywordPhoneHash != \"\"'>OR u.phone_hash = #{query.keywordPhoneHash} </if>) ",
            "</if>",
            "<if test='query.departmentName != null and query.departmentName != \"\"'>",
            "AND u.department_name = #{query.departmentName} ",
            "</if>",
            "<if test='query.status != null and query.status == \"normal\"'>",
            "AND a.sign_in_status = 0 AND (a.sign_out_status IS NULL OR a.sign_out_status IN (0, 4)) ",
            "</if>",
            "<if test='query.status != null and query.status == \"late\"'>",
            "AND a.sign_in_status = 1 ",
            "</if>",
            "<if test='query.status != null and query.status == \"early\"'>",
            "AND a.sign_out_status = 2 ",
            "</if>",
            "<if test='query.status != null and query.status == \"missing\"'>",
            "AND (a.sign_in_status IN (3, 6) OR a.sign_out_status IN (3, 6)) ",
            "</if>",
            "<if test='query.status != null and query.status == \"leave\"'>",
            "AND (a.sign_in_status = 5 OR a.sign_out_status = 5) ",
            "</if>",
            "<if test='query.status != null and query.status == \"overtime\"'>",
            "AND a.sign_out_status = 4 ",
            "</if>",
            "ORDER BY a.update_time DESC, a.id DESC",
            "</script>"
    })
    Page<AttendanceRecordManageVO> selectPage(Page<AttendanceRecordManageVO> page,
                                              @Param("tenantCode") String tenantCode,
                                              @Param("dayPrefix") String dayPrefix,
                                              @Param("query") AttendancePageRequest query);

    @Select({
            "SELECT DISTINCT u.department_name AS name ",
            "FROM user u ",
            "WHERE u.tenant_code = #{tenantCode} ",
            "AND u.department_name IS NOT NULL AND u.department_name != '' ",
            "ORDER BY u.department_name ASC"
    })
    List<AttendanceDepartmentVO> selectDepartments(@Param("tenantCode") String tenantCode);
}
